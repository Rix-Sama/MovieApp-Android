package com.lumio.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.android.volley.*;
import com.android.volley.toolbox.*;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import org.json.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DiscoverFragment extends Fragment {

    private static final String TAG     = "DiscoverFragment";
    private static final String API_KEY = "e21c9bd08ef733416fa4adc42dad2a14";
    private static final String BASE    = "https://api.themoviedb.org/3/";

    private SwipeRefreshLayout swipeRefresh;
    private ImageView imgHero;
    private TextView tvHeroTitle, tvHeroYear, tvHeroScore, tvHeroBadge, tvAvatar;
    private View btnHeroPlay, btnHeroInfo;
    private RecyclerView recyclerTrending, recyclerMovies, recyclerTopRated;
    private Chip chipBlur;

    private MovieGridAdapter adapterTrending, adapterMovies, adapterTopRated;
    private RequestQueue queue;
    private boolean blurEnabled = true;
    private MyMovieData heroMovie;

    public static DiscoverFragment newInstance() { return new DiscoverFragment(); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_discover, container, false);

        swipeRefresh     = root.findViewById(R.id.swipeRefresh);
        imgHero          = root.findViewById(R.id.imgHero);
        tvHeroTitle      = root.findViewById(R.id.tvHeroTitle);
        tvHeroYear       = root.findViewById(R.id.tvHeroYear);
        tvHeroScore      = root.findViewById(R.id.tvHeroScore);
        tvHeroBadge      = root.findViewById(R.id.tvHeroBadge);
        btnHeroPlay      = root.findViewById(R.id.btnHeroPlay);
        btnHeroInfo      = root.findViewById(R.id.btnHeroInfo);
        recyclerTrending = root.findViewById(R.id.recyclerTrending);
        recyclerMovies   = root.findViewById(R.id.recyclerMovies);
        recyclerTopRated = root.findViewById(R.id.recyclerTopRated);
        chipBlur         = root.findViewById(R.id.chipBlurToggle);
        tvAvatar         = root.findViewById(R.id.tvAvatar);

        // Avatar → Profil
        SessionManager s = SessionManager.getInstance(requireContext());
        String initials = "";
        if (!s.getPrenom().isEmpty()) initials += s.getPrenom().charAt(0);
        if (!s.getNom().isEmpty())    initials += s.getNom().charAt(0);
        tvAvatar.setText(initials.toUpperCase());
        tvAvatar.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), ProfileActivity.class)));

        setupRecyclers();
        setupBlurToggle();

        queue = Volley.newRequestQueue(requireContext());

        swipeRefresh.setColorSchemeColors(
            getResources().getColor(R.color.lumio_blue, null));
        swipeRefresh.setProgressBackgroundColorSchemeColor(
            getResources().getColor(R.color.bg_surface, null));
        swipeRefresh.setOnRefreshListener(this::fetchAll);

        fetchAll();
        return root;
    }

    private void setupRecyclers() {
        adapterTrending = new MovieGridAdapter(new MyMovieData[0], requireContext());
        adapterMovies   = new MovieGridAdapter(new MyMovieData[0], requireContext());
        adapterTopRated = new MovieGridAdapter(new MyMovieData[0], requireContext());

        recyclerTrending.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerMovies.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerTopRated.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        recyclerTrending.setAdapter(adapterTrending);
        recyclerMovies.setAdapter(adapterMovies);
        recyclerTopRated.setAdapter(adapterTopRated);
    }

    private void setupBlurToggle() {
        chipBlur.setOnCheckedChangeListener((btn, checked) -> {
            blurEnabled = checked;
            chipBlur.setText(checked ? "Blur ON" : "Blur OFF");
            adapterTrending.setGlobalBlurEnabled(checked);
            adapterMovies.setGlobalBlurEnabled(checked);
            adapterTopRated.setGlobalBlurEnabled(checked);
        });
    }

    public void setBlurEnabled(boolean enabled) {
        blurEnabled = enabled;
        if (adapterMovies != null) {
            adapterTrending.setGlobalBlurEnabled(enabled);
            adapterMovies.setGlobalBlurEnabled(enabled);
            adapterTopRated.setGlobalBlurEnabled(enabled);
        }
    }

    public void filterMovies(String query) {
        if (adapterMovies != null) adapterMovies.getFilter().filter(query);
    }

    private void fetchAll() {
        swipeRefresh.setRefreshing(true);
        fetchMovies("movie/popular",    adapterTrending, true);
        fetchMovies("movie/now_playing", adapterMovies,  false);
        fetchMovies("movie/top_rated",  adapterTopRated, false);
    }

    private void fetchMovies(String endpoint, MovieGridAdapter adapter, boolean isHero) {
        String url = BASE + endpoint + "?api_key=" + API_KEY + "&language=fr-FR";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONArray results = response.getJSONArray("results");
                    int count = Math.min(results.length(), 20);
                    MyMovieData[] movies = new MyMovieData[count];
                    AtomicInteger remaining = new AtomicInteger(count);

                    for (int i = 0; i < count; i++) {
                        JSONObject obj = results.getJSONObject(i);
                        movies[i] = new MyMovieData(
                            obj.getInt("id"),
                            obj.optString("title", ""),
                            obj.optString("release_date", ""),
                            obj.optString("poster_path", ""),
                            obj.optString("backdrop_path", ""),
                            obj.optString("overview", ""),
                            obj.optDouble("vote_average", 0),
                            obj.optBoolean("adult", false)
                        );
                        fetchCert(movies[i], movies, remaining, adapter, isHero);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Parse error", e);
                    swipeRefresh.setRefreshing(false);
                }
            },
            error -> {
                Log.e(TAG, "Network error: " + error.getMessage());
                swipeRefresh.setRefreshing(false);
            }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(20000, 1, 1f));
        queue.add(req);
    }

    private void fetchCert(MyMovieData movie, MyMovieData[] all,
                            AtomicInteger remaining, MovieGridAdapter adapter,
                            boolean isHero) {
        String url = BASE + "movie/" + movie.getMovieId()
                   + "/release_dates?api_key=" + API_KEY;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONArray results = response.getJSONArray("results");
                    String cert = "";
                    outer:
                    for (int j = 0; j < results.length(); j++) {
                        JSONObject c = results.getJSONObject(j);
                        if ("US".equals(c.getString("iso_3166_1"))) {
                            JSONArray dates = c.getJSONArray("release_dates");
                            for (int k = 0; k < dates.length(); k++) {
                                String cc = dates.getJSONObject(k).optString("certification", "");
                                if (!cc.isEmpty()) { cert = cc; break outer; }
                            }
                        }
                    }
                    movie.setCertification(cert);
                    if (MovieGridAdapter.SENSITIVE_RATINGS.contains(cert)) movie.setAdult(true);
                } catch (JSONException ignored) {}

                checkAndUpdate(all, remaining, adapter, isHero);
            },
            error -> checkAndUpdate(all, remaining, adapter, isHero)
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1f));
        queue.add(req);
    }

    private void checkAndUpdate(MyMovieData[] all, AtomicInteger remaining,
                                 MovieGridAdapter adapter, boolean isHero) {
        if (remaining.decrementAndGet() == 0 && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                adapter.updateData(all);
                adapter.setGlobalBlurEnabled(blurEnabled);
                swipeRefresh.setRefreshing(false);
                if (isHero && all.length > 0) setHero(all[0]);
            });
        }
    }

    private void setHero(MyMovieData movie) {
        heroMovie = movie;
        tvHeroTitle.setText(movie.getMovieName());
        tvHeroYear.setText(movie.getYear());
        tvHeroScore.setText("★ " + movie.getFormattedScore());

        String cert = movie.getCertification();
        tvHeroBadge.setVisibility(!cert.isEmpty() ? View.VISIBLE : View.GONE);
        tvHeroBadge.setText(cert);

        if (!movie.getBackdropPath().isEmpty()) {
            Glide.with(this)
                 .load("https://image.tmdb.org/t/p/w780" + movie.getBackdropPath())
                 .centerCrop().into(imgHero);
        }

        // Play → Movie detail
        btnHeroPlay.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getMovieId());
            intent.putExtra("isAdult", movie.isAdult());
            startActivity(intent);
        });

        // + Ma liste → add to watchlist
        btnHeroInfo.setOnClickListener(v -> addToWatchlist(movie));
    }

    private void addToWatchlist(MyMovieData movie) {
        try {
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("movieId",     movie.getMovieId());
            body.put("movieTitle",  movie.getMovieName());
            body.put("posterPath",  movie.getMovieImage());
            body.put("voteAverage", movie.getVoteAverage());
            body.put("releaseDate", movie.getMovieDate());

            String token = SessionManager.getInstance(requireContext()).getAuthHeader();

            JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, ApiClient.WATCHLIST, body,
                response -> Toast.makeText(requireContext(),
                    "✅ Ajouté à Ma liste", Toast.LENGTH_SHORT).show(),
                error -> {
                    if (error.networkResponse != null
                            && error.networkResponse.statusCode == 400) {
                        Toast.makeText(requireContext(),
                            "Déjà dans votre liste", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                            "Erreur ajout watchlist", Toast.LENGTH_SHORT).show();
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
}
