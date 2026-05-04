package com.lumio.app;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Lecteur de trailers YouTube via WebView embed.
 * Simple et fiable — on n'essaie PAS de capturer le WebView (sandboxé).
 * Le blur ML Kit en temps réel est réservé aux vidéos locales (ExoPlayer).
 */
public class TrailerPlayerActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trailer_player);

        webView = findViewById(R.id.webView);
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        String trailerKey = getIntent().getStringExtra("trailerKey");
        setupWebView();
        if (trailerKey != null) loadTrailer(trailerKey);
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/116.0 Mobile Safari/537.36");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadTrailer(String key) {
        String html = "<html><body style='margin:0;padding:0;background:#000'>" +
            "<iframe width='100%' height='100%' " +
            "src='https://www.youtube.com/embed/" + key +
            "?autoplay=1&controls=1&rel=0' " +
            "frameborder='0' allowfullscreen allow='autoplay; encrypted-media'>" +
            "</iframe></body></html>";
        webView.loadData(html, "text/html", "utf-8");
    }

    @Override
    protected void onPause()   { super.onPause();  webView.onPause(); }
    @Override
    protected void onResume()  { super.onResume(); webView.onResume(); }
    @Override
    protected void onDestroy() { super.onDestroy(); webView.destroy(); }
}
