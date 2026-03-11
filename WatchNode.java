package CineVerse;

// Node for the singly linked list used in WatchList and WatchHistory
public class WatchNode {
    Movie movie;
    WatchNode next;

    public WatchNode(Movie movie) {
        this.movie = movie;
        this.next  = null;
    }
}