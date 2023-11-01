package nz.massey.musicplayer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private final List<Song> songs;
    private final OnSongListener onSongListener;
    private int currentPlayingPosition = -1;
    public SongAdapter(List<Song> songs, OnSongListener onSongListener) { // constructor
        this.songs = songs;
        this.onSongListener = onSongListener;
    }
    public interface OnSongListener { // listener interface
        void onSongClick(int position);
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Song currentSong = songs.get(position);
        holder.songTitle.setText(currentSong.getTitle());
        holder.songArtist.setText(currentSong.getArtist());

        // Highlight if this song is currently playing
        if (currentPlayingPosition == position) {
            holder.itemView.setBackgroundColor(Color.GRAY);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            notifyItemChanged(currentPlayingPosition); // Reset the previous song item
            currentPlayingPosition = position;
            notifyItemChanged(currentPlayingPosition); // Update the new song item
            onSongListener.onSongClick(position);
        });
    }
    public void setCurrentPlayingPosition(int position) {
        notifyItemChanged(currentPlayingPosition);
        this.currentPlayingPosition = position;
        notifyItemChanged(currentPlayingPosition);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
    public static class SongViewHolder extends RecyclerView.ViewHolder {
        public TextView songTitle, songArtist;
        public SongViewHolder(View view) {
            super(view);
            songTitle = view.findViewById(R.id.textViewSongTitle); // set title
            songArtist = view.findViewById(R.id.textViewSongArtist); // set artist
        }
    }
}

