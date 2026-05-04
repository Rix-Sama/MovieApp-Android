package com.lumio.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class    MainActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 200;

    // Tabs index
    private static final int TAB_HOME      = 0;
    private static final int TAB_SEARCH    = 1;
    private static final int TAB_WATCHLIST = 2;
    private static final int TAB_MYFILMS   = 3;

    private ViewPager2 viewPager;
    private FloatingActionButton fabUpload;
    private LinearLayout navHome, navSearch, navWatchlist, navMyMovies;

    private DiscoverFragment  discoverFragment;
    private SearchFragment    searchFragment;
    private WatchlistFragment watchlistFragment;
    private MyMoviesFragment  myMoviesFragment;

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) importVideo(uri);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        viewPager    = findViewById(R.id.viewPager);
        fabUpload    = findViewById(R.id.fabUpload);
        navHome      = findViewById(R.id.navHome);
        navSearch    = findViewById(R.id.navSearch);
        navWatchlist = findViewById(R.id.navWatchlist);
        navMyMovies  = findViewById(R.id.navMyMovies);

        setupViewPager();
        setupBottomNav();
    }

    // ── ViewPager ────────────────────────────────────────────────────────────

    private void setupViewPager() {
        discoverFragment  = DiscoverFragment.newInstance();
        searchFragment    = SearchFragment.newInstance();
        watchlistFragment = WatchlistFragment.newInstance();
        myMoviesFragment  = MyMoviesFragment.newInstance();

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override
            public Fragment createFragment(int pos) {
                switch (pos) {
                    case TAB_SEARCH:    return searchFragment;
                    case TAB_WATCHLIST: return watchlistFragment;
                    case TAB_MYFILMS:   return myMoviesFragment;
                    default:            return discoverFragment;
                }
            }
            @Override public int getItemCount() { return 4; }
        });

        viewPager.setUserInputEnabled(false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                fabUpload.setVisibility(position == TAB_MYFILMS ? View.VISIBLE : View.GONE);
                updateNavSelection(position);
                // Refresh watchlist seulement si le fragment est attaché
                if (position == TAB_WATCHLIST && watchlistFragment != null
                        && watchlistFragment.isAdded() && watchlistFragment.getContext() != null) {
                    watchlistFragment.loadWatchlist();
                }
            }
        });
    }

    // ── Bottom nav ───────────────────────────────────────────────────────────

    private void setupBottomNav() {
        navHome.setOnClickListener(v      -> viewPager.setCurrentItem(TAB_HOME, false));
        navSearch.setOnClickListener(v    -> viewPager.setCurrentItem(TAB_SEARCH, false));
        navWatchlist.setOnClickListener(v -> viewPager.setCurrentItem(TAB_WATCHLIST, false));
        navMyMovies.setOnClickListener(v  -> viewPager.setCurrentItem(TAB_MYFILMS, false));
        fabUpload.setOnClickListener(v    -> checkPermissionAndPickVideo());
    }

    private void updateNavSelection(int position) {
        int[] tabs = {TAB_HOME, TAB_SEARCH, TAB_WATCHLIST, TAB_MYFILMS};
        LinearLayout[] navs = {navHome, navSearch, navWatchlist, navMyMovies};

        for (int i = 0; i < navs.length; i++) {
            boolean active = (position == tabs[i]);
            View ico = navs[i].getChildAt(0);
            View lbl = navs[i].getChildAt(1);
            View dot = navs[i].getChildAt(2);
            if (ico != null) ico.setAlpha(active ? 1f : 0.4f);
            if (lbl instanceof TextView)
                ((TextView) lbl).setTextColor(getResources().getColor(
                    active ? R.color.lumio_blue : R.color.text_muted, null));
            if (dot != null) dot.setVisibility(active ? View.VISIBLE : View.GONE);
        }
    }

    // ── Permissions + video import ───────────────────────────────────────────

    private void checkPermissionAndPickVideo() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? Manifest.permission.READ_MEDIA_VIDEO
            : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{perm}, PERM_REQUEST);
        else
            openVideoPicker();
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                      | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        videoPickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == PERM_REQUEST && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED)
            openVideoPicker();
        else
            Toast.makeText(this, "Permission refusée", Toast.LENGTH_LONG).show();
    }

    private void importVideo(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        String displayName = "Film inconnu"; long fileSize = 0; String mimeType = "video/*";
        try (Cursor c = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                displayName = c.getString(0);
                fileSize    = c.getLong(1);
            }
        }
        String detectedMime = getContentResolver().getType(uri);
        if (detectedMime != null) mimeType = detectedMime;

        String name = displayName;
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);

        LocalMovieData movie = new LocalMovieData(
            System.currentTimeMillis(), displayName, "", uri, fileSize, mimeType);
        LocalMovieRepository.getInstance(this).addMovie(movie);
        if (myMoviesFragment != null) myMoviesFragment.refreshList();
        viewPager.setCurrentItem(TAB_MYFILMS, false);
        Toast.makeText(this, "✅ \"" + name + "\" ajouté", Toast.LENGTH_LONG).show();
    }
}
