package nz.massey.musicplayer;

// Song class to hold details about each music file
public class Song {
    private String title;
    private String artist;
    private String path;
    private final long albumId;
    public Song(String title, String artist, String path, long albumId) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.albumId = albumId;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }
    public long getAlbumId() {
        return albumId;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getArtist() {
        return artist;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
}
