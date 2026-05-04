package com.lumio.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.List;

public class LocalMovieAdapter extends RecyclerView.Adapter<LocalMovieAdapter.ViewHolder> {

    private final List<LocalMovieData> movies;
    private final Context context;
    private final LocalMovieRepository repo;
    private OnListChangedListener listener;

    public interface OnListChangedListener { void onListChanged(); }

    public LocalMovieAdapter(List<LocalMovieData> movies, Context context) {
        this.movies  = movies;
        this.context = context;
        this.repo    = LocalMovieRepository.getInstance(context);
    }

    public void setOnListChangedListener(OnListChangedListener l) { listener = l; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.item_local_movie, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalMovieData movie = movies.get(position);

        holder.tvFileName.setText(movie.getDisplayName());
        holder.tvFileInfo.setText(movie.getExtension() + " • " + movie.getFormattedSize());

        // Thumbnail via Glide (pour les vidéos locales, Glide charge la 1ère frame)
        Glide.with(context)
             .load(movie.getFileUri())
             .centerCrop()
             .placeholder(android.R.drawable.ic_media_play)
             .into(holder.imgThumbnail);

        // Blur badge
        holder.badgeBlur.setVisibility(movie.isBlurEnabled() ? View.VISIBLE : View.GONE);

        // Blur switch (sans déclencher d'events en cascade pendant le bind)
        holder.switchBlur.setOnCheckedChangeListener(null);
        holder.switchBlur.setChecked(movie.isBlurEnabled());
        holder.switchBlur.setOnCheckedChangeListener((btn, checked) -> {
            repo.updateBlurState(movie, checked);
            holder.badgeBlur.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        // Clic → lancer le lecteur
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, LocalVideoPlayerActivity.class);
            intent.putExtra("videoUri", movie.getFileUri().toString());
            intent.putExtra("videoTitle", movie.getDisplayName());
            intent.putExtra("blurEnabled", movie.isBlurEnabled());
            context.startActivity(intent);
        });

        // Options menu (3 points)
        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMore);
            popup.getMenu().add("▶ Lire");
            popup.getMenu().add("🗑 Supprimer de la liste");
            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.contains("Lire")) {
                    holder.itemView.performClick();
                } else if (title.contains("Supprimer")) {
                    new AlertDialog.Builder(context)
                        .setTitle("Supprimer")
                        .setMessage("Retirer \"" + movie.getDisplayName() + "\" de la liste ?")
                        .setPositiveButton("Supprimer", (d, w) -> {
                            repo.removeMovie(movie);
                            movies.remove(movie);
                            notifyDataSetChanged();
                            if (listener != null) listener.onListChanged();
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
                }
                return true;
            });
            popup.show();
        });
    }

    @Override public int getItemCount() { return movies.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail, btnMore;
        TextView tvFileName, tvFileInfo, badgeBlur;
        SwitchMaterial switchBlur;

        ViewHolder(@NonNull View v) {
            super(v);
            imgThumbnail = v.findViewById(R.id.imgThumbnail);
            btnMore      = v.findViewById(R.id.btnMore);
            tvFileName   = v.findViewById(R.id.tvFileName);
            tvFileInfo   = v.findViewById(R.id.tvFileInfo);
            badgeBlur    = v.findViewById(R.id.badgeBlur);
            switchBlur   = v.findViewById(R.id.switchBlur);
        }
    }
}
