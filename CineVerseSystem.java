package CineVerse;

import java.util.Scanner;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class CineVerseSystem {

    // Movie database (up to 100 movies)
    Movie[] movies = new Movie[100];
    int count  = 0;
    int nextId = 0; // set automatically after loading from file

    // User database (up to 50 users)
    User[] users   = new User[50];
    int userCount  = 0;
    User activeUser = null;  // currently logged-in user

    // File to save movie data
    private static final String MOVIES_FILE = "movies.txt";

    // File to save user data
    private static final String USERS_FILE  = "users.txt";

    // Stack – undo last added movie / undo last watchlist add 
    // Stores the movieId of each newly added movie in the order they were added
    int[] undoMovieStack = new int[100];
    int   undoMovieTop   = -1;           // -1 means stack is empty

    // Queue – tracks watch list and watch history 
    Movie[] watchQueue = new Movie[100];
    int     queueFront = 0;
    int     queueRear  = -1;
    int     queueSize  = 0;

    // Hash Table – fast search by title, genre, language, OTT
    // Linear probing used to handle collisions
    Movie[] hashTable = new Movie[125]; // 25 percent excess space 

    // Constructor – load from files instead of hard coding
    public CineVerseSystem() {
        loadMoviesFromFile(); // reads movies.txt using BufferedReader
        loadUsersFromFile(); // reads users.txt using BufferedReader
        mergeSortByTitle(movies, 0, count - 1); // By default sort titles into ascending order
        buildHashTable(); // build hash table after all movies are loaded
    }

    // File loading

    // Reads movies.txt line by line using BufferedReader
    // Skips comment lines starting with # and blank lines
    public void loadMoviesFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(MOVIES_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // skip comments
                String[] parts = line.split("\\|");
                if (parts.length < 9) continue; // skip malformed lines
                int id = Integer.parseInt(parts[0].trim());
                String title = parts[1].trim();
                String genre = parts[2].trim();
                String language = parts[3].trim();
                String ageRating = parts[4].trim();
                int year = Integer.parseInt(parts[5].trim());
                String ott = parts[6].trim();
                double baseRating = Double.parseDouble(parts[7].trim());
                int views = Integer.parseInt(parts[8].trim());
                if (count < movies.length) {
                    movies[count++] = new Movie(id, title, genre, language, ageRating, year, ott, baseRating, views);
                    if (id >= nextId) nextId = id + 1; 
                }
            }
            System.out.println("Loaded " + count + " movies from " + MOVIES_FILE);
        } catch (IOException e) {
            System.out.println("Warning: Could not load movies file. " + e.getMessage());
        }
    }

    // Reads users.txt line by line using BufferedReader
    // Skips comment lines starting with # and blank lines
    public void loadUsersFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // skip comments
                String[] parts = line.split("\\|");
                if (parts.length < 3) continue; // skip malformed lines
                String username = parts[0].trim();
                String genre    = parts[1].trim();
                String language = parts[2].trim();
                if (userCount < users.length) {
                    users[userCount++] = new User(username, genre, language);
                }
            }
            System.out.println("Loaded " + userCount + " users from " + USERS_FILE);
        } catch (IOException e) {
            System.out.println("Warning: Could not load users file. " + e.getMessage());
        }
    }

    // Add movie (By default sort titles into ascending order while adding)
    public void addMovie(Scanner sc) {
        sc.nextLine(); // consume leftover newline
        System.out.print("Enter Title: ");
        String title = sc.nextLine();
        System.out.print("Enter Genre: ");
        String genre = sc.nextLine();
        System.out.print("Enter Language: ");
        String language = sc.nextLine();
        System.out.print("Enter Age Rating (U / U/A / A / PG-13 / R): ");
        String ageRating = sc.nextLine();
        System.out.print("Enter Year Released: ");
        int year = sc.nextInt(); sc.nextLine();
        System.out.print("Enter OTT Platform: ");
        String ott = sc.nextLine();
        System.out.print("Enter Base Rating (1-5): ");
        double baseRating = sc.nextDouble();

        if (count < movies.length) {
            movies[count] = new Movie(nextId, title, genre, language, ageRating, year, ott, baseRating, 0);
            // Push this movie's id onto undo stack so we can undo this add (Stack push)
            undoMovieStack[++undoMovieTop] = nextId;
            nextId++;
            count++;
            // Keep array sorted alphabetically so binary search stays valid
            mergeSortByTitle(movies, 0, count - 1);
            // Rebuild hash table after adding new movie
            buildHashTable();
            System.out.println("Movie added successfully with ID: " + (nextId - 1));
            // Append the newly added movie to the file
            saveMoviesToFile(title, genre, language, ageRating, year, ott, baseRating);
            //rewriteMoviesFile();
        } else {
            System.out.println("Cannot add more movies. Array is full.");
        }
    }

    // Undo the last added movie – removes it from array, hash table, and rewrites the file (Stack pop)
    public void undoAddMovie() {
        if (undoMovieTop == -1) {
            System.out.println("Nothing to undo.");
            return;
        }
        // Pop the id of the last added movie from the undo stack
        int removedId = undoMovieStack[undoMovieTop--];
        String removedTitle = "";
        // Find this movie in the array and remove it
        for (int i = 0; i < count; i++) {
            if (movies[i].movieId == removedId) {
                removedTitle = movies[i].title;
                // Shift array left to fill the gap
                for (int j = i; j < count - 1; j++) {
                    movies[j] = movies[j + 1];
                }
                movies[count - 1] = null;
                count--;
                nextId--;
                break;
            }
        }
        // Rebuild hash table after removal
        buildHashTable();
        // Rewrite the file without the removed movie
        rewriteMoviesFile();
        System.out.println("Undo successful! Removed: \"" + removedTitle + "\"");
    }

    // Undo the last watch list add – removes the most recently added item from watch list (Stack Pop)
    public void undoWatchlistAdd() {
        if (activeUser == null) {
            System.out.println("Please log in first.");
            return;
        }
        if (activeUser.watchlistHead == null) {
            System.out.println("Watchlist is already empty. Nothing to undo.");
            return;
        }
        // Head of watchlist = last added item = stack top, so remove head
        Movie removed = activeUser.watchlistHead.movie;
        activeUser.watchlistHead = activeUser.watchlistHead.next;
        System.out.println("Undo successful! Removed \"" + removed.title + "\" from your watchlist.");
    }

    // Display all movies
    public void displayMovies() {
        System.out.printf("%-4s | %-25s | %-10s | %-9s | %-5s | %4s | %-15s | %-11s | %s%n",
                "ID", "Title", "Genre", "Language", "Age", "Year", "OTT Platform", "Rating", "Views");
        System.out.println("-".repeat(120));
        for (int i = 0; i < count; i++) {
            movies[i].display();
        }
    }

    // Binary Search by Title (array is always kept sorted alphabetically) 
    public void binarySearchByTitle(String title) {
        int si = 0, ei = count - 1;
        boolean found = false;
        while (si <= ei) {
            int mi = (si + ei) / 2;
            int cmp = movies[mi].title.compareToIgnoreCase(title);
            if (cmp == 0) {
                movies[mi].display();
                found = true;
                break;
            } else if (cmp < 0) {
                si = mi + 1;
            } else {
                ei = mi - 1;
            }
        }
        if (!found) System.out.println("Movie not found!");
    }

    // find array index by title (after sort)
    private int findIndexByTitle(String title) {
        for (int i = 0; i < count; i++) {
            if (movies[i].title.equalsIgnoreCase(title)) return i;
        }
        return -1;
    }

    // find movie object by title
    public Movie getMovieByTitle(String title) {
        int idx = findIndexByTitle(title);
        return idx == -1 ? null : movies[idx];
    }

    // Watch Movie – shows full movie list first, then asks which to watch
    public void watchMovie(Scanner sc) throws InterruptedException {
        if (activeUser == null) {
            System.out.println("Please log in first.");
            return;
        }

        // Show all available movies first so user can pick
        System.out.println("\nMovies available to watch:");
        displayMovies();

        sc.nextLine();
        System.out.print("Enter movie title to watch: ");
        String title = sc.nextLine();
        Movie m = getMovieByTitle(title);
        if (m == null) {
            System.out.println("Movie not found!");
            return;
        }

        // "Play" the movie
        System.out.println("\n🎬 Now playing: " + m.title);
        Thread.sleep(1000);
        System.out.println("...\nThe End...\n");

        // Ask for rating
        System.out.print("Rate this movie ( 1(Worst) - 10(Excellent) ): ");
        int userRating = sc.nextInt();
        while (userRating < 1 || userRating > 10) {
            System.out.print("Invalid. Enter a rating between 1 and 10: ");
            userRating = sc.nextInt();
        }
        m.addUserRating(userRating);
        System.out.printf("Thanks! Updated movie rating: %.1f%n", m.rating);

        // Update user records
        activeUser.addToHistory(m);
        activeUser.removeFromWatchlist(m.movieId);

        // Record in watch queue (enque method)
        enqueueWatchQueue(m);
    }

    // User management - Registration
    public void registerUser(Scanner sc) {
        if (userCount >= users.length) {
            System.out.println("User limit reached.");
            return;
        }
        sc.nextLine();
        System.out.print("Enter username: ");
        String name = sc.nextLine();
        // Check duplicate
        for (int i = 0; i < userCount; i++) {
            if (users[i].username.equalsIgnoreCase(name)) {
                System.out.println("Username already exists.");
                return;
            }
        }
        System.out.print("Enter your preferred genre (Action/Comedy/Fantasy/Animation/Adventure/Mythology/Drama/Romance etc.): ");
        String genre = sc.nextLine();
        System.out.print("Enter your preferred language (Hindi/Telugu/Tamil/English/Japanese etc.): ");
        String language = sc.nextLine();
        users[userCount++] = new User(name, genre, language);
        System.out.println("User \"" + name + "\" registered successfully!");
        // Save new user to file (append)
        saveUserToFile(name, genre, language);
    }

    public void loginUser(Scanner sc) {
        sc.nextLine();
        System.out.print("Enter username: ");
        String name = sc.nextLine();
        for (int i = 0; i < userCount; i++) {
            if (users[i].username.equalsIgnoreCase(name)) {
                activeUser = users[i];
                System.out.println("Welcome back, " + activeUser.username + "! 👋");
                return;
            }
        }
        System.out.println("User not found. Please register first.");
    }

    public void logoutUser() {
        if (activeUser != null) {
            System.out.println("Goodbye, " + activeUser.username + "!");
            activeUser = null;
        } else {
            System.out.println("No user is logged in.");
        }
    }

    // Recommendation sub-menu – 1. by genre  2. by language (Linear Search)
    public void showRecommendations(Scanner sc) {
        if (activeUser == null) {
            System.out.println("Please log in first.");
            return;
        }

        System.out.println("\nRecommendation Menu");
        System.out.println("1. By Genre    (your genre: "    + activeUser.preferredGenre    + ")");
        System.out.println("2. By Language (your language: " + activeUser.preferredLanguage + ")");
        System.out.print("Enter choice: ");
        int recChoice = sc.nextInt();

        if (recChoice == 1) {
            // Recommend by genre
            System.out.println("\nRecommended for you (Genre: " + activeUser.preferredGenre + ") :");
            boolean any = false;
            for (int i = 0; i < count; i++) {
                if (movies[i].genre.equalsIgnoreCase(activeUser.preferredGenre)) {
                    movies[i].display();
                    any = true;
                    System.out.print("Add to watchlist? (y/n): ");
                    String ans = sc.next();
                    if (ans.equalsIgnoreCase("y")) {
                        activeUser.addToWatchlist(movies[i]);
                        enqueueWatchQueue(movies[i]); 
                    }
                }
            }
            if (!any) System.out.println("No recommendations found for this genre.");

        } else if (recChoice == 2) {
            // Recommend by language
            sc.nextLine();
            System.out.print("Enter language to filter by: ");
            String lang = sc.nextLine();
            System.out.println("\nRecommended for you (Language: " + lang + ") :");
            boolean any = false;
            for (int i = 0; i < count; i++) {
                if (movies[i].language.equalsIgnoreCase(lang)) {
                    movies[i].display();
                    any = true;
                    System.out.print("Add to watchlist? (y/n): ");
                    String ans = sc.next();
                    if (ans.equalsIgnoreCase("y")) {
                        activeUser.addToWatchlist(movies[i]);
                        enqueueWatchQueue(movies[i]); 
                    }
                }
            }
            if (!any) System.out.println("No recommendations found for this language.");

        } else {
            System.out.println("Invalid choice.");
        }
    }

    // Hash Table (linear probing) 

    // Hash function
    private int hash(String title) {
        int h = 0;
        for (int i = 0; i < title.length(); i++) {
            h = (h * 31 + Character.toLowerCase(title.charAt(i))) % hashTable.length;
        }
        return Math.abs(h);
    }

    // Build the hash table from the movies array - start ;after every add / undo
    public void buildHashTable() {
        // Clear the hash table first
        for (int i = 0; i < hashTable.length; i++) hashTable[i] = null;
        // Insert each movie – use linear probing if slot is taken
        for (int i = 0; i < count; i++) {
            int idx = hash(movies[i].title);
            while (hashTable[idx] != null) {
                idx = (idx + 1) % hashTable.length; // linear probing
            }
            hashTable[idx] = movies[i];
        }
    }

    // Hash Table search – search by title (hashtable), genre, language, or OTT (linear search)
    public void hashSearch(Scanner sc) {
        sc.nextLine();
        System.out.println("\nHash Table Search");
        System.out.println("1. Search by Title    (Hashtable search)");
        System.out.println("2. Search by Genre    (Linear search)");
        System.out.println("3. Search by Language (Linear search)");
        System.out.println("4. Search by OTT Platform (Linear search)");
        System.out.print("Enter choice: ");
        int searchChoice = Integer.parseInt(sc.nextLine().trim());

        if (searchChoice == 1) {
            // Hashtable search by title
            System.out.print("Enter Title: ");
            String title = sc.nextLine();
            int idx    = hash(title);
            boolean found = false;
            int probes = 0;
            while (hashTable[idx] != null && probes < hashTable.length) {
                if (hashTable[idx].title.equalsIgnoreCase(title)) {
                    hashTable[idx].display();
                    found = true;
                    break;
                }
                idx = (idx + 1) % hashTable.length; // linear probing
                probes++;
            }
            if (!found) System.out.println("Movie not found!");

        } else if (searchChoice == 2) {
            // Search by genre – scan entry in hash table - linear search 
            System.out.print("Enter Genre: ");
            String genre = sc.nextLine();
            boolean found = false;
            for (int i = 0; i < hashTable.length; i++) {
                if (hashTable[i] != null && hashTable[i].genre.equalsIgnoreCase(genre)) {
                    hashTable[i].display();
                    found = true;
                }
            }
            if (!found) System.out.println("No movies found for this genre!");

        } else if (searchChoice == 3) {
            // Search by language – scan entry in hash table - linear search 
            System.out.print("Enter Language: ");
            String language = sc.nextLine();
            boolean found = false;
            for (int i = 0; i < hashTable.length; i++) {
                if (hashTable[i] != null && hashTable[i].language.equalsIgnoreCase(language)) {
                    hashTable[i].display();
                    found = true;
                }
            }
            if (!found) System.out.println("No movies found for this language!");

        } else if (searchChoice == 4) {
            // Search by OTT platform – scan entry in hash table - linear search 
            System.out.print("Enter OTT Platform: ");
            String ott = sc.nextLine();
            boolean found = false;
            for (int i = 0; i < hashTable.length; i++) {
                if (hashTable[i] != null && hashTable[i].ottPlatform.equalsIgnoreCase(ott)) {
                    hashTable[i].display();
                    found = true;
                }
            }
            if (!found) System.out.println("No movies found for this OTT platform!");

        } else {
            System.out.println("Invalid choice.");
        }
    }

    // Queue operations (circular queue) 

    // Enqueue a movie into the watch queue
    private void enqueueWatchQueue(Movie m) {
        if (queueSize < watchQueue.length) {
            queueRear = (queueRear + 1) % watchQueue.length;
            watchQueue[queueRear] = m;
            queueSize++;
        }
    }

    // Dequeue – removes and shows the front of the queue
    public void dequeueWatchQueue() {
        if (queueSize == 0) {
            System.out.println("Watch queue is empty.");
            return;
        }
        Movie m = watchQueue[queueFront];
        queueFront = (queueFront + 1) % watchQueue.length;
        queueSize--;
        System.out.println("Dequeued: \"" + m.title + "\"");
    }

    // Display all movies currently in the watch queue
    public void displayWatchQueue() {
        if (queueSize == 0) {
            System.out.println("Watch queue is empty.");
            return;
        }
        System.out.println("\nWatch Queue (FIFO – first added plays first):");
        for (int i = 0; i < queueSize; i++) {
            int idx = (queueFront + i) % watchQueue.length;
            watchQueue[idx].display();
        }
    }

    // Priority Queue Using Heap Sort 

    // Shows highest rated unwatched movies first using heap sort
    // Movies already in the active user's watch history are skipped
    public void showTopRatedUnwatched() {
        if (activeUser == null) {
            System.out.println("Please log in first.");
            return;
        }

        // Copy only unwatched movies into a temp array
        Movie[] temp = new Movie[count];
        int tempCount = 0;
        for (int i = 0; i < count; i++) {
            if (!activeUser.hasWatched(movies[i].movieId)) {
                temp[tempCount++] = movies[i];
            }
        }

        if (tempCount == 0) {
            System.out.println("You have watched all available movies!");
            return;
        }

        // Build a max-heap based on rating 
        for (int i = tempCount / 2 - 1; i >= 0; i--) {
            heapify(temp, tempCount, i);
        }

        // Extract and display from heap – highest rated comes out first
        System.out.println("\nTop Rated Unwatched Movies (Highest Priority First) :");
        System.out.printf("%-4s | %-40s | %-12s | %-8s | %-5s | %4s | %-15s | %-12s | %s%n",
                "ID", "Title", "Genre", "Language", "Age", "Year", "OTT Platform", "Rating", "Views");
        System.out.println("-".repeat(120));
        int remaining = tempCount;
        while (remaining > 0) {
            temp[0].display(); // root of max-heap = highest rated
            // Move last element to root and re-heapify to maintain heap property
            temp[0]       = temp[remaining - 1];
            remaining--;
            heapify(temp, remaining, 0);
        }
    }

    // Heapify helper – maintains max-heap property based on rating
    private void heapify(Movie[] arr, int n, int i) {
        int largest = i;
        int left    = 2 * i + 1;
        int right   = 2 * i + 2;
        if (left  < n && arr[left].rating  > arr[largest].rating) 
        		largest = left;
        if (right < n && arr[right].rating > arr[largest].rating) 
        		largest = right;
        if (largest != i) {
            Movie temp   = arr[i];
            arr[i]       = arr[largest];
            arr[largest] = temp;
            heapify(arr, n, largest);
        }
    }

    // File

    // Append a newly added movie to the file.
    // Existing content is never deleted – new entry is added at the bottom.
    private void saveMoviesToFile(String title, String genre, String language,
                                  String ageRating, int year, String ott, double baseRating) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(MOVIES_FILE, true))) { // append = true
            bw.write((nextId - 1) + " | " + title + " | " + genre + " | " + language + " | "
                    + ageRating + " | " + year + " | " + ott + " | "
                    + String.format("%.1f", baseRating) + " | 0");
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Warning: Could not save movie to file. " + e.getMessage());
        }
    }

    // Rewrites the entire movies file – called after undo to remove a movie from the file
    private void rewriteMoviesFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(MOVIES_FILE, false))) {
            bw.write("# CineVerse Movie Database");
            bw.newLine();
            bw.write("# Format: ID | Title | Genre | Language | AgeRating | Year | OTTPlatform | BaseRating | Views");
            bw.newLine();
            bw.write("# -----------------------------------------------------------------------");
            bw.newLine();
            for (int i = 0; i < count; i++) {
                Movie m = movies[i];
                bw.write(m.movieId + " | " + m.title + " | " + m.genre + " | " + m.language + " | "
                        + m.ageRating + " | " + m.yearReleased + " | " + m.ottPlatform + " | "
                        + String.format("%.1f", m.rating) + " | " + m.views);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not rewrite movies file. " + e.getMessage());
        }
    }

    // Append a newly registered user to users.txt
    private void saveUserToFile(String username, String genre, String language) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE, true))) { // append = true
            bw.write(username + " | " + genre + " | " + language);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Warning: Could not save user to file. " + e.getMessage());
        }
    }

    // Sorting methods 

    // Quick Sort – descending by rating (Quick Sort)
    void quickSortByRating(Movie[] a, int si, int ei) {
        if (si < ei) {
            int j = partitionByRating(a, si, ei);
            quickSortByRating(a, si, j - 1);
            quickSortByRating(a, j + 1, ei);
        }
    }
    int partitionByRating(Movie[] a, int si, int ei) {
        double pivot = a[si].rating;
        int i = si, j = ei + 1;
        do {
            do { i++; } while (i <= ei && a[i].rating > pivot);
            do { j--; } while (a[j].rating < pivot);
            if (i < j) swap(a, i, j);
        } while (i < j);
        swap(a, si, j);
        return j;
    }

    // Quick Sort – descending by views (Quick Sort)
    void quickSortByViews(Movie[] a, int si, int ei) {
        if (si < ei) {
            int j = partitionByViews(a, si, ei);
            quickSortByViews(a, si, j - 1);
            quickSortByViews(a, j + 1, ei);
        }
    }
    int partitionByViews(Movie[] a, int si, int ei) {
        int pivot = a[si].views;
        int i = si, j = ei + 1;
        do {
            do { i++; } while (i <= ei && a[i].views > pivot);
            do { j--; } while (a[j].views < pivot);
            if (i < j) swap(a, i, j);
        } while (i < j);
        swap(a, si, j);
        return j;
    }

    // Merge Sort – ascending by title (alphabetical) (Merge Sort)
    void mergeSortByTitle(Movie[] arr, int si, int ei) {
        if (si < ei) {
            int mi = (si + ei) / 2;
            mergeSortByTitle(arr, si, mi);
            mergeSortByTitle(arr, mi + 1, ei);
            mergeByTitle(arr, si, mi, ei);
        }
    }
    void mergeByTitle(Movie[] arr, int si, int mi, int ei) {
        Movie[] temp = new Movie[ei - si + 1];
        int i = si, j = mi + 1, k = 0;
        while (i <= mi && j <= ei) {
            if (arr[i].title.compareToIgnoreCase(arr[j].title) <= 0)
                temp[k++] = arr[i++];
            else
                temp[k++] = arr[j++];
        }
        while (i <= mi) temp[k++] = arr[i++];
        while (j <= ei) temp[k++] = arr[j++];
        for (int x = si, y = 0; x <= ei; x++, y++) arr[x] = temp[y];
    }

    // Merge Sort – ascending by language (alphabetical) (Merge Sort)
    void mergeSortByLanguage(Movie[] arr, int si, int ei) {
        if (si < ei) {
            int mi = (si + ei) / 2;
            mergeSortByLanguage(arr, si, mi);
            mergeSortByLanguage(arr, mi + 1, ei);
            mergeByLanguage(arr, si, mi, ei);
        }
    }
    void mergeByLanguage(Movie[] arr, int si, int mi, int ei) {
        Movie[] temp = new Movie[ei - si + 1];
        int i = si, j = mi + 1, k = 0;
        while (i <= mi && j <= ei) {
            if (arr[i].language.compareToIgnoreCase(arr[j].language) <= 0)
                temp[k++] = arr[i++];
            else
                temp[k++] = arr[j++];
        }
        while (i <= mi) temp[k++] = arr[i++];
        while (j <= ei) temp[k++] = arr[j++];
        for (int x = si, y = 0; x <= ei; x++, y++) arr[x] = temp[y];
    }

    // Merge Sort – descending by year released (most recent first) (Merge Sort)
    void mergeSortByYear(Movie[] arr, int si, int ei) {
        if (si < ei) {
            int mi = (si + ei) / 2;
            mergeSortByYear(arr, si, mi);
            mergeSortByYear(arr, mi + 1, ei);
            mergeByYear(arr, si, mi, ei);
        }
    }
    void mergeByYear(Movie[] arr, int si, int mi, int ei) {
        Movie[] temp = new Movie[ei - si + 1];
        int i = si, j = mi + 1, k = 0;
        while (i <= mi && j <= ei) {
            if (arr[i].yearReleased >= arr[j].yearReleased)
                temp[k++] = arr[i++];
            else
                temp[k++] = arr[j++];
        }
        while (i <= mi) temp[k++] = arr[i++];
        while (j <= ei) temp[k++] = arr[j++];
        for (int x = si, y = 0; x <= ei; x++, y++) arr[x] = temp[y];
    }

    public void swap(Movie[] a, int i, int j) {
        Movie temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }
}