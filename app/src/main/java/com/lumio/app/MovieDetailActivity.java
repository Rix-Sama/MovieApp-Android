package com.lumio.app;

import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MovieDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MovieDetailActivity";
    private static final String API_KEY = "e21c9bd08ef733416fa4adc42dad2a14";
    private static final String W500 = "https://image.tmdb.org/t/p/w500";
    private static final String W780 = "https://image.tmdb.org/t/p/w780";

    private ImageView imgBackdrop, imgPoster;
    private View posterBlurOverlay;
    private TextView tvTitle, tvYear, tvScore, tvRatingBadge, tvOverview;
    private View btnPlayTrailer;
    private RequestQueue queue;
    private String trailerKey;
    private boolean isAdult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        imgBackdrop       = findViewById(R.id.imgBackdrop);
        imgPoster         = findViewById(R.id.imgPoster);
        posterBlurOverlay = findViewById(R.id.posterBlurOverlay);
        tvTitle           = findViewById(R.id.tvTitle);
        tvYear            = findViewById(R.id.tvYear);
        tvScore           = findViewById(R.id.tvScore);
        tvRatingBadge     = findViewById(R.id.tvRatingBadge);
        tvOverview        = findViewById(R.id.tvOverview);
        btnPlayTrailer    = findViewById(R.id.btnPlayTrailer);

        LinearLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        isAdult = getIntent().getBooleanExtra("isAdult", false);
        int movieId = getIntent().getIntExtra("movieId", -1);

        queue = Volley.newRequestQueue(this);
        if (movieId != -1) {
            fetchDetails(movieId);
            fetchTrailer(movieId);
        }

        btnPlayTrailer.setOnClickListener(v -> {
            if (trailerKey != null) {
                Intent intent = new Intent(this, TrailerPlayerActivity.class);
                intent.putExtra("trailerKey", trailerKey);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Trailer non disponible", Toast.LENGTH_SHORT).show();
            }
        });

        SupportMapFragment mapFrag = (SupportMapFragment)
            getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFrag != null) mapFrag.getMapAsync(this);
    }

    private void fetchDetails(int id) {
        String url = "https://api.themoviedb.org/3/movie/" + id
                   + "?api_key=" + API_KEY + "&language=fr-FR";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                tvTitle.setText(response.optString("title", ""));
                String date = response.optString("release_date", "");
                tvYear.setText(date.length() >= 4 ? date.substring(0, 4) : date);
                tvScore.setText("★ " + String.format("%.1f", response.optDouble("vote_average", 0)) + " / 10");
                String overview = response.optString("overview", "");
                tvOverview.setText(overview.isEmpty() ? "Aucune description disponible." : overview);

                String backdrop = response.optString("backdrop_path", "");
                String poster   = response.optString("poster_path", "");
                if (!backdrop.isEmpty()) Glide.with(this).load(W780 + backdrop).centerCrop().into(imgBackdrop);
                if (!poster.isEmpty())   Glide.with(this).load(W500 + poster).centerCrop().into(imgPoster);
                applyBlurIfNeeded();
            },
            error -> Log.e(TAG, "Detail error: " + error.getMessage())
        );
        queue.add(req);
    }

    private void fetchTrailer(int id) {
        String url = "https://api.themoviedb.org/3/movie/" + id + "/videos?api_key=" + API_KEY;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONArray results = response.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject v = results.getJSONObject(i);
                        if ("Trailer".equals(v.getString("type")) && "YouTube".equals(v.getString("site"))) {
                            trailerKey = v.getString("key");
                            break;
                        }
                    }
                } catch (JSONException ignored) {}
            }, null
        );
        queue.add(req);
    }

    private void applyBlurIfNeeded() {
        if (!isAdult) { posterBlurOverlay.setVisibility(View.GONE); return; }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            imgPoster.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP));
        }
        posterBlurOverlay.setVisibility(View.VISIBLE);
        posterBlurOverlay.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Contenu sensible")
                .setMessage("Afficher l'affiche de ce film ?")
                .setPositiveButton("Afficher", (d, w) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) imgPoster.setRenderEffect(null);
                    posterBlurOverlay.setVisibility(View.GONE);
                })
                .setNegativeButton("Annuler", null).show()
        );
    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng pos = new LatLng(33.596460, -7.615480);
        map.addMarker(new MarkerOptions().position(pos).title("Mégarama Casablanca"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 13));
    }
}
