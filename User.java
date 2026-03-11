package CineVerse;

public class User {
    String username;
    String preferredGenre;
    String preferredLanguage;   // added for language-based recommendations

    // Linked list head pointers
    WatchNode watchlistHead;    // movies the user wants to watch
    WatchNode historyHead;      // movies the user has already watched

    public User(String username, String preferredGenre, String preferredLanguage) {
        this.username          = username;
        this.preferredGenre    = preferredGenre;
        this.preferredLanguage = preferredLanguage;
        this.watchlistHead     = null;
        this.historyHead       = null;
    }

    // Watchlist operations
    public void addToWatchlist(Movie movie) {
        // Avoid duplicates
        WatchNode temp = watchlistHead;
        while (temp != null) {
            if (temp.movie.movieId == movie.movieId) {
                System.out.println("\"" + movie.title + "\" is already in your watchlist.");
                return;
            }
            temp = temp.next;
        }
        WatchNode newNode = new WatchNode(movie);
        newNode.next = watchlistHead;
        watchlistHead = newNode;
        System.out.println("\"" + movie.title + "\" added to your watchlist!");
    }

    public void displayWatchlist() {
        if (watchlistHead == null) {
            System.out.println("Your watchlist is empty.");
            return;
        }
        System.out.println("\n" + username + "'s Watchlist:");
        WatchNode temp = watchlistHead;
        while (temp != null) {
        		temp.movie.display();
        		temp = temp.next;
        }
    }

    // Remove a movie from watchlist (called after watching)
    public void removeFromWatchlist(int movieId) {
        if (watchlistHead == null) return;
        if (watchlistHead.movie.movieId == movieId) {
            watchlistHead = watchlistHead.next;
            return;
        }
        WatchNode temp = watchlistHead;
        while (temp.next != null) {
            if (temp.next.movie.movieId == movieId) {
            		temp.next = temp.next.next;
                return;
            }
            temp = temp.next;
        }
    }

    // Watch History 
    public void addToHistory(Movie movie) {
        WatchNode newNode = new WatchNode(movie);
        newNode.next = historyHead;
        historyHead  = newNode;
    }

    public void displayHistory() {
        if (historyHead == null) {
            System.out.println("No watch history yet.");
            return;
        }
        System.out.println("\n" + username + "'s Watch History:");
        WatchNode temp = historyHead;
        while (temp != null) {
            temp.movie.display();
            temp = temp.next;
        }
    }

    // Check if this movie is already in watch history
    public boolean hasWatched(int movieId) {
        WatchNode temp = historyHead;
        while (temp != null) {
            if (temp.movie.movieId == movieId) return true;
            temp = temp.next;
        }
        return false;
    }
}