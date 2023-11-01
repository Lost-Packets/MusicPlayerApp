package nz.massey.musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongListener{
    ////////////////////////////////////////////Vars////////////////////////////////////////////////
    private ImageView mainAlbumArtImageView;
    private final List<Song> songList = new ArrayList<>();
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private int currentSongIndex;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1001;  // API < 33
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 1002; // API >= 33
    private TextView currentTime;
    private TextView totalTime;
    private SeekBar seekBar;
    private final Handler handler = new Handler();

    ///////////////////////////////////////////Permissions//////////////////////////////////////////
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Check Version
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
            } else {
                loadMusicFiles();
            }
        } else { // Older Version
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                loadMusicFiles();
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { // On Request API version > 33
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMusicFiles();
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { // On Result API version <= 33
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadMusicFiles();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //////////////////////////////////////////Music Logic///////////////////////////////////////////
    private final Runnable updateSongTime = new Runnable() {
        public void run() {
            try {
                if (mediaPlayer != null) {
                    int currentTimeMs = mediaPlayer.getCurrentPosition(); // Get current position of song
                    seekBar.setProgress(currentTimeMs); // set seekbar
                    String currentTimeStr = String.format(Locale.getDefault(), "%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(currentTimeMs),
                            TimeUnit.MILLISECONDS.toSeconds(currentTimeMs) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentTimeMs))
                    );
                    currentTime.setText(currentTimeStr); // set current time text
                    handler.postDelayed(this, 1000); // Update every second
                    Log.d("MainActivity", "Current time: " + currentTimeStr);
                }
            } catch (Exception e) { // debug
                Log.e("MainActivity", "Error updating song time", e);
            }

        }
    };
    public void loadMusicFiles() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; // Get Uri
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
        };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String artist = cursor.getString(1);
                String path = cursor.getString(2);
                long albumId = cursor.getLong(3);
                // Add to song list
                songList.add(new Song(title, artist, path, albumId));
            }
            cursor.close();
        }
    }
    private void playNextSong() {
        if (songList.isEmpty()) return; // check if empty
        if (currentSongIndex < (songList.size() - 1)) {
            currentSongIndex++;
        } else { // reset
            currentSongIndex = 0;
        }
        playSong(currentSongIndex); // play
    }
    private void playPreviousSong() {
        if (songList.isEmpty()) return; // check if empty
        if (currentSongIndex > 0) {
            currentSongIndex--;
        } else { // reset
            currentSongIndex = songList.size() - 1;
        }
        playSong(currentSongIndex);
    }
    private void playSong(int songIndex) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        Uri songUri = Uri.parse(songList.get(songIndex).getPath()); // parse the song path

        try {
            mediaPlayer.setDataSource(getApplicationContext(), songUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        seekBar.setMax(mediaPlayer.getDuration()); // set max seek range (song length)
        int totalTimeMs = mediaPlayer.getDuration(); // Get total duration of the song in milliseconds
        String totalTimeStr = String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(totalTimeMs), TimeUnit.MILLISECONDS.toSeconds(totalTimeMs)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeMs)));
        totalTime.setText(totalTimeStr); // set total time text

        // Start the handler update current time
        handler.postDelayed(updateSongTime, 1000);
        // Debug
        Log.d("MainActivity", "Total time: " + totalTimeStr);
    }
    private Bitmap getAlbumArt(Context context, long albumId) {
        Bitmap albumArt = null;
        try {
            final Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri uri = ContentUris.withAppendedId(artworkUri, albumId);

            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                albumArt = BitmapFactory.decodeFileDescriptor(fd);
                pfd.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return albumArt;
    }
    @Override
    public void onSongClick(int position) {
        Song selectedSong = songList.get(position); // get song
        Bitmap albumArt = getAlbumArt(this, selectedSong.getAlbumId());

        if (albumArt != null) {
            mainAlbumArtImageView.setImageBitmap(albumArt); // get album art
        } else {
            mainAlbumArtImageView.setImageResource(R.drawable.placeholder_image); // else placeholder
        }

        if (mediaPlayer.isPlaying()) { // reset if new song is clicked
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        try { // play new song
            mediaPlayer.setDataSource(selectedSong.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int totalTimeMs = mediaPlayer.getDuration(); // Get total duration of the song in milliseconds
        String totalTimeStr = String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(totalTimeMs), TimeUnit.MILLISECONDS.toSeconds(totalTimeMs)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeMs)));
        totalTime.setText(totalTimeStr); // Set totalTime TextView

        // Start the handler to update the current time
        handler.postDelayed(updateSongTime, 1000); // 1 sec increments
        handler.post(updateSongTime);
        seekBar.setMax(mediaPlayer.getDuration());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSongTime);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    //////////////////////////////////////////OnCreate//////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request Permissions
        requestStoragePermission();

        // Create RecyclerView
        RecyclerView recyclerView = findViewById(R.id.songList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        SongAdapter adapter = new SongAdapter(songList, this);
        recyclerView.setAdapter(adapter);

        // Set Ids
        mainAlbumArtImageView = findViewById(R.id.albumArt);
        seekBar = findViewById(R.id.seekBar);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        Button playButton = findViewById(R.id.playButton);
        Button pauseButton = findViewById(R.id.pauseButton);
        Button prevButton = findViewById(R.id.prevButton);
        Button nextButton = findViewById(R.id.nextButton);

        // Click Listeners For Buttons
        playButton.setOnClickListener(v -> {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        });

        pauseButton.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        });

        prevButton.setOnClickListener(v -> {
            playPreviousSong();
            adapter.setCurrentPlayingPosition(currentSongIndex);
        });

        nextButton.setOnClickListener(v -> {
            playNextSong();
            adapter.setCurrentPlayingPosition(currentSongIndex);
        });

        // Seek Listeners
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
            }
        });
    }
}