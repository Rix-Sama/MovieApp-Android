package com.lumio.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchFragment extends Fragment {

    private static final String API_KEY = "e21c9bd08ef733416fa4adc42dad2a14";
    private static final String SEARCH_URL =
        "https://api.themoviedb.org/3/search/movie?api_key=" + API_KEY
        + "&language=fr-FR&query=";

    private EditText etSearch;
    private TextView btnClear;
    private ProgressBar progressSearch;
    private RecyclerView recyclerSearch;
    private View emptySearch;
    private MovieGridAdapter adapter;
    private RequestQueue queue;
    private android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;

    public static SearchFragment newInstance() { return new SearchFragment(); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search, container, false);

        etSearch      = root.findViewById(R.id.etSearch);
        btnClear      = root.findViewById(R.id.btnClear);
        progressSearch = root.findViewById(R.id.progressSearch);
        recyclerSearch = root.findViewById(R.id.recyclerSearch);
        emptySearch   = root.findViewById(R.id.emptySearch);

        adapter = new MovieGridAdapter(new MyMovieData[0], requireContext());
        recyclerSearch.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerSearch.setAdapter(adapter);

        queue = Volley.newRequestQueue(requireContext());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int b, int count) {
                String query = s.toString().trim();
                btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                if (query.length() < 2) {
                    showEmpty();
                    return;
                }
                // Debounce 500ms
                searchRunnable = () -> performSearch(query);
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });

        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            showEmpty();
        });

        return root;
    }

    private void performSearch(String query) {
        progressSearch.setVisibility(View.VISIBLE);
        recyclerSearch.setVisibility(View.GONE);
        emptySearch.setVisibility(View.GONE);

        String url = SEARCH_URL + android.net.Uri.encode(query) + "&page=1";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONArray results = response.getJSONArray("results");
                    if (results.length() == 0) {
                        progressSearch.setVisibility(View.GONE);
                        emptySearch.setVisibility(View.VISIBLE);
                        return;
                    }
                    int count = Math.min(results.length(), 30);
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
                        final int idx = i;
                        // Pas besoin de certifications pour la recherche
                        if (remaining.decrementAndGet() == 0 && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressSearch.setVisibility(View.GONE);
                                adapter.updateData(movies);
                                recyclerSearch.setVisibility(View.VISIBLE);
                            });
                        }
                    }
                } catch (Exception e) {
                    progressSearch.setVisibility(View.GONE);
                    showEmpty();
                }
            },
            error -> {
                progressSearch.setVisibility(View.GONE);
                showEmpty();
            }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1f));
        queue.add(req);
    }

    private void showEmpty() {
        progressSearch.setVisibility(View.GONE);
        recyclerSearch.setVisibility(View.GONE);
        emptySearch.setVisibility(View.VISIBLE);
    }
}
