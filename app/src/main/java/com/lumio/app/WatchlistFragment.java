package com.lumio.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchlistFragment extends Fragment {

    private RecyclerView recyclerWatchlist;
    private View emptyWatchlist;
    private TextView tvWatchlistCount;
    private RequestQueue queue;
    private WatchlistAdapter adapter;
    private final List<JSONObject> items = new ArrayList<>();

    public static WatchlistFragment newInstance() { return new WatchlistFragment(); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_watchlist, container, false);

        recyclerWatchlist = root.findViewById(R.id.recyclerWatchlist);
        emptyWatchlist    = root.findViewById(R.id.emptyWatchlist);
        tvWatchlistCount  = root.findViewById(R.id.tvWatchlistCount);

        queue = Volley.newRequestQueue(requireContext());
        adapter = new WatchlistAdapter();
        recyclerWatchlist.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerWatchlist.setAdapter(adapter);

        loadWatchlist();
        return root;
    }

    @Override public void onResume() { super.onResume(); loadWatchlist(); }

    public void loadWatchlist() {
        String token = SessionManager.getInstance(requireContext()).getAuthHeader();

        JsonArrayRequest req = new JsonArrayRequest(
            Request.Method.GET, ApiClient.WATCHLIST, null,
            response -> {
                items.clear();
                for (int i = 0; i < response.length(); i++) {
                    try { items.add(response.getJSONObject(i)); }
                    catch (JSONException ignored) {}
                }
                adapter.notifyDataSetChanged();
                updateState();
            },
            error -> Log.e("WATCHLIST", "Error: " + error.getMessage())
        ) {
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Authorization", token);
                return h;
            }
        };
        queue.add(req);
    }

    private void updateState() {
        int count = items.size();
        tvWatchlistCount.setText(count + " film" + (count > 1 ? "s" : ""));
        if (count == 0) {
            recyclerWatchlist.setVisibility(View.GONE);
            emptyWatchlist.setVisibility(View.VISIBLE);
        } else {
            recyclerWatchlist.setVisibility(View.VISIBLE);
            emptyWatchlist.setVisibility(View.GONE);
        }
    }

    private void removeItem(int movieId, int position) {
        String url = ApiClient.WATCHLIST + "/" + movieId;
        String token = SessionManager.getInstance(requireContext()).getAuthHeader();

        com.android.volley.toolbox.StringRequest req =
            new com.android.volley.toolbox.StringRequest(
                Request.Method.DELETE, url,
                response -> {
                    if (position < items.size()) items.remove(position);
                    adapter.notifyDataSetChanged();
                    updateState();
                },
                error -> Log.e("WATCHLIST", "Remove error")
            ) {
                @Override public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    h.put("Authorization", token);
                    return h;
                }
            };
        queue.add(req);
    }

    // ── Inner Adapter ────────────────────────────────────────────────────────

    class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_watchlist, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            try {
                JSONObject item = items.get(position);
                String title  = item.optString("movieTitle", "");
                String poster = item.optString("posterPath", "");
                double score  = item.optDouble("voteAverage", 0);
                String date   = item.optString("releaseDate", "");
                int movieId   = item.optInt("movieId", 0);

                holder.tvTitle.setText(title);
                holder.tvYear.setText(date.length() >= 4 ? date.substring(0, 4) : date);
                holder.tvScore.setText("★ " + String.format("%.1f", score));

                if (!poster.isEmpty()) {
                    Glide.with(requireContext())
                         .load("https://image.tmdb.org/t/p/w185" + poster)
                         .centerCrop().into(holder.imgPoster);
                }

                holder.btnRemove.setOnClickListener(v -> removeItem(movieId, position));

                holder.itemView.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(
                        requireContext(), MovieDetailActivity.class);
                    intent.putExtra("movieId", movieId);
                    intent.putExtra("isAdult", false);
                    startActivity(intent);
                });

            } catch (Exception ignored) {}
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView imgPoster, btnRemove;
            TextView tvTitle, tvYear, tvScore;
            VH(@NonNull View v) {
                super(v);
                imgPoster = v.findViewById(R.id.imgPoster);
                btnRemove = v.findViewById(R.id.btnRemove);
                tvTitle   = v.findViewById(R.id.tvTitle);
                tvYear    = v.findViewById(R.id.tvYear);
                tvScore   = v.findViewById(R.id.tvScore);
            }
        }
    }
}
