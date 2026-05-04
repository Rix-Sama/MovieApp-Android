package com.lumio.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton pour persister la liste des films importés entre les sessions.
 * Les URIs sont stockées dans SharedPreferences en JSON.
 */
public class LocalMovieRepository {

    private static final String PREFS_NAME  = "local_movies_prefs";
    private static final String KEY_MOVIES  = "movies_json";

    private static LocalMovieRepository instance;
    private final SharedPreferences prefs;
    private final List<LocalMovieData> movies = new ArrayList<>();

    private LocalMovieRepository(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    public static synchronized LocalMovieRepository getInstance(Context context) {
        if (instance == null) instance = new LocalMovieRepository(context);
        return instance;
    }

    public List<LocalMovieData> getMovies() { return movies; }

    public void addMovie(LocalMovieData movie) {
        // Éviter les doublons par URI
        for (LocalMovieData m : movies) {
            if (m.getFileUri().equals(movie.getFileUri())) return;
        }
        movies.add(movie);
        save();
    }

    public void removeMovie(LocalMovieData movie) {
        movies.remove(movie);
        save();
    }

    public void updateBlurState(LocalMovieData movie, boolean enabled) {
        movie.setBlurEnabled(enabled);
        save();
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (LocalMovieData m : movies) {
                JSONObject obj = new JSONObject();
                obj.put("id",          m.getId());
                obj.put("displayName", m.getDisplayName());
                obj.put("filePath",    m.getFilePath());
                obj.put("fileUri",     m.getFileUri().toString());
                obj.put("fileSize",    m.getFileSizeBytes());
                obj.put("mimeType",    m.getMimeType());
                obj.put("blurEnabled", m.isBlurEnabled());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_MOVIES, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        try {
            String json = prefs.getString(KEY_MOVIES, null);
            if (json == null) return;
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                LocalMovieData m = new LocalMovieData(
                    obj.getLong("id"),
                    obj.getString("displayName"),
                    obj.optString("filePath", ""),
                    Uri.parse(obj.getString("fileUri")),
                    obj.getLong("fileSize"),
                    obj.optString("mimeType", "video/*")
                );
                m.setBlurEnabled(obj.optBoolean("blurEnabled", true));
                movies.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
