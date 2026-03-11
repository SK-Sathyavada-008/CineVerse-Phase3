package CineVerse;

// creating a movie class
public class Movie {
    int movieId;
    String title;
    String genre;
    String language;
    String ageRating;
    int yearReleased;
    String ottPlatform;
    double rating;
    int views;
    long totalUserRating;
    int ratingCount; 

    // movie constructor
    public Movie(int movieId, String title, String genre, String language, String ageRating, int yearReleased, String ottPlatform, double baseRating, int views) {
        this.movieId     = movieId;
        this.title       = title;
        this.genre       = genre;
        this.language    = language;
        this.ageRating   = ageRating;
        this.yearReleased = yearReleased;
        this.ottPlatform = ottPlatform;
        this.rating      = baseRating;
        this.views       = views;
        this.totalUserRating = (long)(baseRating * 10);
        this.ratingCount     = 10; 
    }

    // Called when a user watches and rates the movie
    public void addUserRating(int userRating) {
        views++;
        totalUserRating += userRating;
        ratingCount++;
        // New rating = weighted average 
        // take a rating from 1 to 10
        // (sum / count ) => rating on 10; /2 rating on 5 scale
        rating = Math.round(((double) totalUserRating / ratingCount) / 2.0 * 10.0) / 10.0;
    }

    // display method to print movie details
    public void display() {
        System.out.printf("%-4d | %-25s | %-10s | %-9s | %-5s | %4d | %-15s | Rating: %.1f | Views: %d%n",
                movieId, title, genre, language, ageRating, yearReleased, ottPlatform, rating, views);
    }
}