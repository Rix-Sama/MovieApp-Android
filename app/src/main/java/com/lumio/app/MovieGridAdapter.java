package com.lumio.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MovieGridAdapter extends RecyclerView.Adapter<MovieGridAdapter.ViewHolder>
        implements Filterable {

    private static final String TAG      = "MovieGridAdapter";
    private static final String TMDB_IMG = "https://image.tmdb.org/t/p/w342";

    public static final Set<String> SENSITIVE_RATINGS = new HashSet<>(
            Arrays.asList("R", "NC-17", "TV-MA", "18", "18+", "X", "M")
    );

    private MyMovieData[] originalData;
    private List<MyMovieData> filteredData;
    private final Context context;
    private boolean globalBlurEnabled = true;
    private final Set<Integer> unblocked = new HashSet<>();

    public MovieGridAdapter(MyMovieData[] data, Context context) {
        this.originalData = data;
        this.filteredData = new ArrayList<>(Arrays.asList(data));
        this.context = context;
    }

    public void setGlobalBlurEnabled(boolean enabled) {
        globalBlurEnabled = enabled;
        notifyDataSetChanged();
    }

    public void updateData(MyMovieData[] newData) {
        originalData = newData;
        filteredData = new ArrayList<>(Arrays.asList(newData));
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie_grid, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMovieData movie = filteredData.get(position);

        holder.tvTitle.setText(movie.getMovieName());
        holder.tvYear.setText(movie.getYear());
        holder.tvScore.setText("★ " + movie.getFormattedScore());

        String cert = movie.getCertification();
        if (!cert.isEmpty()) {
            holder.tvRating.setText(cert);
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }

        String posterUrl = TMDB_IMG + movie.getMovieImage();
        boolean shouldBlur = globalBlurEnabled
                && movie.isAdult()
                && !unblocked.contains(movie.getMovieId());

        if (shouldBlur) {
            applyBlurEffect(holder.imgPoster);
            Glide.with(context).load(posterUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.imgPoster);
            holder.blurOverlay.setVisibility(View.VISIBLE);

            holder.itemView.setOnClickListener(v ->
                    new android.app.AlertDialog.Builder(context)
                            .setTitle("⚠️ Contenu sensible")
                            .setMessage("Ce film est classifié " + cert + ".\nAfficher quand même ?")
                            .setPositiveButton("Afficher", (d, w) -> {
                                unblocked.add(movie.getMovieId());
                                notifyItemChanged(holder.getAdapterPosition());
                            })
                            .setNegativeButton("Annuler", null)
                            .show()
            );
            holder.itemView.setOnLongClickListener(v -> true);

        } else {
            removeBlurEffect(holder.imgPoster);
            holder.blurOverlay.setVisibility(View.GONE);
            Glide.with(context).load(posterUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.imgPoster);

            // Clic simple → détail
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MovieDetailActivity.class);
                intent.putExtra("movieId", movie.getMovieId());
                intent.putExtra("isAdult", movie.isAdult());
                context.startActivity(intent);
            });

            // Appui long → Ma liste
            holder.itemView.setOnLongClickListener(v -> {
                addToWatchlist(movie);
                return true;
            });
        }
    }

    private void addToWatchlist(MyMovieData movie) {
        try {
            JSONObject body = new JSONObject();
            body.put("movieId",     movie.getMovieId());
            body.put("movieTitle",  movie.getMovieName());
            body.put("posterPath",  movie.getMovieImage());
            body.put("voteAverage", movie.getVoteAverage());
            body.put("releaseDate", movie.getMovieDate());

            String token = SessionManager.getInstance(context).getAuthHeader();
            RequestQueue queue = Volley.newRequestQueue(context);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST, ApiClient.WATCHLIST, body,
                    response -> Toast.makeText(context,
                            "✅ Ajouté à Ma liste", Toast.LENGTH_SHORT).show(),
                    error -> {
                        if (error.networkResponse != null
                                && error.networkResponse.statusCode == 400) {
                            Toast.makeText(context,
                                    "Déjà dans votre liste", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Watchlist error: " + error.getMessage());
                        }
                    }
            ) {
                @Override public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    h.put("Authorization", token);
                    h.put("Content-Type", "application/json");
                    return h;
                }
            };
            queue.add(req);
        } catch (Exception e) {
            Log.e(TAG, "addToWatchlist error", e);
        }
    }

    private void applyBlurEffect(ImageView iv) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            iv.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP));
    }

    private void removeBlurEffect(ImageView iv) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            iv.setRenderEffect(null);
    }

    @Override public int getItemCount() { return filteredData.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        View blurOverlay;
        TextView tvTitle, tvYear, tvScore, tvRating;

        ViewHolder(@NonNull View v) {
            super(v);
            imgPoster   = v.findViewById(R.id.imgPoster);
            blurOverlay = v.findViewById(R.id.blurOverlay);
            tvTitle     = v.findViewById(R.id.tvTitle);
            tvYear      = v.findViewById(R.id.tvYear);
            tvScore     = v.findViewById(R.id.tvScore);
            tvRating    = v.findViewById(R.id.tvRating);
        }
    }

    @Override public Filter getFilter() { return movieFilter; }

    private final Filter movieFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence c) {
            List<MyMovieData> list = new ArrayList<>();
            if (c == null || c.length() == 0) {
                list.addAll(Arrays.asList(originalData));
            } else {
                String q = c.toString().toLowerCase().trim();
                for (MyMovieData m : originalData) {
                    if (m.getMovieName().toLowerCase().contains(q)) list.add(m);
                }
            }
            FilterResults r = new FilterResults();
            r.values = list;
            return r;
        }

        @Override @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence c, FilterResults r) {
            filteredData.clear();
            if (r.values != null) filteredData.addAll((List<MyMovieData>) r.values);
            notifyDataSetChanged();
        }
    };
}