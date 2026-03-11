package CineVerse;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);
        CineVerseSystem cv = new CineVerseSystem();
        int choice;

        do {
            System.out.println("\n🎬  CineVerse  🎬");
            System.out.println("================================");

            // Show active user
            if (cv.activeUser != null)
                System.out.println("👤 Logged in as: " + cv.activeUser.username);

            System.out.println();
            System.out.println("Movie Menu");
            System.out.println(" 1. Display All Movies");
            System.out.println(" 2. Add Movie");
            System.out.println(" 3. Search Movie by Title (Binary Search)");
            System.out.println(" 4. Search by Title/Genre/Language/OTT (Hash Table)");
            System.out.println(" 5. Sort by Rating (Quick Sort)");
            System.out.println(" 6. Sort by Views (Quick Sort)");
            System.out.println(" 7. Sort by Title (Merge Sort)");
            System.out.println(" 8. Sort by Language (Merge Sort)");
            System.out.println(" 9. Sort by Year Released (Merge Sort)");
            System.out.println();
            System.out.println("Member Menu");
            System.out.println("10. Register");
            System.out.println("11. Login");
            System.out.println("12. Logout");
            System.out.println("13. Watch a Movie");
            System.out.println("14. View My Watchlist (Linked List)");
            System.out.println("15. View My Watch History (Linked List)");
            System.out.println("16. Get Recommendations (Genre / Language)");
            System.out.println();
            System.out.println("17. Top Rated Unwatched Movies (Priority Queue / Heap Sort)");
            System.out.println("18. View Watch Queue (Queue)");
            System.out.println("19. Undo Last Added Movie (Stack)");
            System.out.println("20. Undo Last Watchlist Add (Stack)");
            System.out.println();
            System.out.println("21.  Exit");
            System.out.print("Enter your choice: ");
            choice = sc.nextInt();

            switch (choice) {
                case 1:
                    cv.displayMovies();
                    break;
                case 2:
                    cv.addMovie(sc);
                    break;
                case 3:
                    sc.nextLine();
                    System.out.print("Enter Title to Search: ");
                    String searchTitle = sc.nextLine();
                    cv.binarySearchByTitle(searchTitle);
                    break;
                case 4:
                    cv.hashSearch(sc);
                    break;
                case 5:
                    cv.quickSortByRating(cv.movies, 0, cv.count - 1);
                    System.out.println("\nMovies sorted by Rating :");
                    cv.displayMovies();
                    // Re-sort alphabetically so binary search stays valid
                    cv.mergeSortByTitle(cv.movies, 0, cv.count - 1);
                    break;
                case 6:
                    cv.quickSortByViews(cv.movies, 0, cv.count - 1);
                    System.out.println("\nMovies sorted by Views :");
                    cv.displayMovies();
                    // Re-sort alphabetically so binary search stays valid
                    cv.mergeSortByTitle(cv.movies, 0, cv.count - 1);
                    break;
                case 7:
                    cv.mergeSortByTitle(cv.movies, 0, cv.count - 1);
                    System.out.println("\nMovies sorted by Title :");
                    cv.displayMovies();
                    break;
                case 8:
                    cv.mergeSortByLanguage(cv.movies, 0, cv.count - 1);
                    System.out.println("\nMovies sorted by Language :");
                    cv.displayMovies();
                    // Re-sort alphabetically so binary search stays valid
                    cv.mergeSortByTitle(cv.movies, 0, cv.count - 1);
                    break;
                case 9:
                    cv.mergeSortByYear(cv.movies, 0, cv.count - 1);
                    System.out.println("\nMovies sorted by Year Released :");
                    cv.displayMovies();
                    // Re-sort alphabetically so binary search stays valid
                    cv.mergeSortByTitle(cv.movies, 0, cv.count - 1);
                    break;

                case 10:
                    cv.registerUser(sc);
                    break;
                case 11:
                    cv.loginUser(sc);
                    break;
                case 12:
                    cv.logoutUser();
                    break;
                case 13:
                    cv.watchMovie(sc);
                    break;
                case 14:
                    if (cv.activeUser == null)
                        System.out.println("Please log in first.");
                    else
                        cv.activeUser.displayWatchlist();
                    break;
                case 15:
                    if (cv.activeUser == null)
                        System.out.println("Please log in first.");
                    else
                        cv.activeUser.displayHistory();
                    break;
                case 16:
                    cv.showRecommendations(sc);
                    break;

                case 17:
                    cv.showTopRatedUnwatched();
                    break;
                case 18:
                    cv.displayWatchQueue();
                    break;
                case 19:
                    cv.undoAddMovie();
                    break;
                case 20:
                    cv.undoWatchlistAdd();
                    break;

                case 21:
                    System.out.println("🎬 Bye!");
                    break;
                default:
                    System.out.println("Invalid choice! Please try again.");
            }

        } while (choice != 21);
        sc.close();
    }
}