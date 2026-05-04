package com.lumio.app;

import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Lecteur vidéo local avec ExoPlayer + analyse ML Kit en temps réel.
 *
 * ── Pourquoi ExoPlayer et pas WebView ? ────────────────────────────────────
 * WebView (YouTube embed) est sandboxé : getDrawingCache() retourne toujours null
 * ou un bitmap vide. ExoPlayer expose un SurfaceView accessible dont on peut
 * capturer les frames via PixelCopy (API 26+) pour les analyser avec ML Kit.
 */
public class LocalVideoPlayerActivity extends AppCompatActivity {

    private static final String TAG = "LocalVideoPlayer";

    /** Labels ML Kit déclenchant le blur */
    private static final Set<String> SENSITIVE_LABELS = new HashSet<>(Arrays.asList(
            "blood", "wound", "injury", "gore",
            "nudity", "adult content", "sexual activity",
            "weapon", "gun", "knife", "firearm"
    ));

    private static final float CONFIDENCE_THRESHOLD = 0.80f;
    private static final int   ANALYSIS_INTERVAL_MS = 1000; // toutes les 2.5 s

    // UI
    private PlayerView playerView;
    private View blurOverlay;
    private ImageView blurredFrame;
    private TextView tvDetectedLabel;
    private View statusBar;
    private View statusDot;
    private TextView tvStatus;
    private SwitchMaterial switchBlurActive;

    // Player
    private ExoPlayer player;

    // ML Kit
    private ImageLabeler labeler;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isBlurred     = false;
    private boolean isAnalyzing   = false;
    private boolean blurEnabled   = true;

    private final Runnable analysisRunnable = new Runnable() {
        @Override public void run() {
            if (blurEnabled && !isAnalyzing && player != null
                    && player.isPlaying()) {
                captureAndAnalyze();
            }
            handler.postDelayed(this, ANALYSIS_INTERVAL_MS);
        }
    };

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_video_player);

        playerView      = findViewById(R.id.playerView);
        blurOverlay     = findViewById(R.id.blurOverlay);
        blurredFrame    = findViewById(R.id.blurredFrame);
        tvDetectedLabel = findViewById(R.id.tvDetectedLabel);
        statusBar       = findViewById(R.id.statusBar);
        statusDot       = findViewById(R.id.statusDot);
        tvStatus        = findViewById(R.id.tvStatus);
        switchBlurActive = findViewById(R.id.switchBlurActive);

        String uriStr   = getIntent().getStringExtra("videoUri");
        String title    = getIntent().getStringExtra("videoTitle");
        blurEnabled     = getIntent().getBooleanExtra("blurEnabled", true);

        setupPlayer(uriStr);
        setupMlKit();
        setupBlurSwitch();
        setupContinueButton();

        statusBar.setVisibility(blurEnabled ? View.VISIBLE : View.GONE);
        switchBlurActive.setChecked(blurEnabled);

        // Démarrer l'analyse après un délai (laisser le temps à la vidéo de charger)
        handler.postDelayed(analysisRunnable, 4000);
    }

    // ── ExoPlayer ─────────────────────────────────────────────────────────────

    private void setupPlayer(String uriStr) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);

        if (uriStr != null) {
            MediaItem item = MediaItem.fromUri(Uri.parse(uriStr));
            player.setMediaItem(item);
            player.prepare();
            player.play();
        }

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                tvStatus.setText(isPlaying
                    ? "ML Kit: analyse active"
                    : "ML Kit: en pause");
            }
        });
    }

    // ── ML Kit ───────────────────────────────────────────────────────────────

    private void setupMlKit() {
        ImageLabelerOptions opts = new ImageLabelerOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .build();
        labeler = ImageLabeling.getClient(opts);
    }

    /**
     * Capture une frame du SurfaceView via PixelCopy (API 26+) puis
     * envoie le bitmap à ML Kit pour analyse.
     */
    private void captureAndAnalyze() {
        if (!blurEnabled || isBlurred) return;

        // Récupérer le SurfaceView exposé par ExoPlayer
        SurfaceView sv = (SurfaceView) playerView.getVideoSurfaceView();
        if (sv == null || sv.getWidth() == 0) return;

        isAnalyzing = true;
        tvStatus.setText("ML Kit: capture en cours…");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // PixelCopy : la seule méthode fiable pour capturer un SurfaceView
            Bitmap bmp = Bitmap.createBitmap(sv.getWidth(), sv.getHeight(),
                                             Bitmap.Config.ARGB_8888);
            PixelCopy.request(sv, bmp, copyResult -> {
                if (copyResult == PixelCopy.SUCCESS) {
                    analyzeFrame(bmp);
                } else {
                    isAnalyzing = false;
                    tvStatus.setText("ML Kit: capture échouée");
                }
            }, handler);
        } else {
            // Fallback API 26 non disponible — pas d'analyse (Android < 8)
            isAnalyzing = false;
        }
    }

    private void analyzeFrame(Bitmap bitmap) {
        // Réduire pour ML Kit
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 640, 360, true);
        InputImage image = InputImage.fromBitmap(resized, 0);

        labeler.process(image)
            .addOnSuccessListener(labels -> {
                String detected = null;
                for (ImageLabel label : labels) {
                    String text = label.getText().toLowerCase();
                    for (String keyword : SENSITIVE_LABELS) {
                        if (text.contains(keyword)) {
                            detected = label.getText() + " ("
                                + Math.round(label.getConfidence() * 100) + "%)";
                            break;
                        }
                    }
                    if (detected != null) break;
                }
                if (detected != null) {
                    applyBlur(bitmap, detected);
                } else {
                    removeBlur();
                    tvStatus.setText("ML Kit: aucun contenu sensible détecté");
                }
                isAnalyzing = false;
            })
            .addOnFailureListener(e -> {
                isAnalyzing = false;
                tvStatus.setText("ML Kit: erreur d'analyse");
            });
    }

    // ── Blur UI ───────────────────────────────────────────────────────────────

    private void applyBlur(Bitmap frame, String reason) {
        if (isBlurred) return;
        isBlurred = true;

        runOnUiThread(() -> {
            // Mettre une version floue de la frame en fond
            Bitmap blurred = blurBitmap(frame);
            blurredFrame.setImageBitmap(blurred);
            tvDetectedLabel.setText("Détecté: " + reason);

            blurOverlay.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(300);
            blurOverlay.startAnimation(fadeIn);

            // Pause automatique du lecteur
            if (player != null) player.pause();

            statusDot.setBackgroundResource(R.drawable.bg_badge_red);
            tvStatus.setText("🔞 Contenu sensible — lecture suspendue");
        });
    }

    private void removeBlur() {
        if (!isBlurred) return;
        isBlurred = false;

        runOnUiThread(() -> {
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(400);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}
                @Override public void onAnimationEnd(Animation a) {
                    blurOverlay.setVisibility(View.GONE);
                }
            });
            blurOverlay.startAnimation(fadeOut);
            statusDot.setBackgroundResource(R.drawable.bg_dot_green);
        });
    }

    private Bitmap blurBitmap(Bitmap original) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Sur Android 12+ on retourne le bitmap non flouté ;
                // RenderEffect sera appliqué sur l'ImageView directement
                if (blurredFrame != null) {
                    blurredFrame.setRenderEffect(
                        RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP));
                }
                return original;
            }
            // RenderScript pour Android 8-11
            Bitmap out = original.copy(Bitmap.Config.ARGB_8888, true);
            RenderScript rs = RenderScript.create(this);
            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation inp = Allocation.createFromBitmap(rs, out);
            Allocation outp = Allocation.createTyped(rs, inp.getType());
            blur.setRadius(25f);
            blur.setInput(inp);
            blur.forEach(outp);
            outp.copyTo(out);
            rs.destroy();
            return out;
        } catch (Exception e) {
            return original;
        }
    }

    // ── Controls ─────────────────────────────────────────────────────────────

    private void setupBlurSwitch() {
        switchBlurActive.setOnCheckedChangeListener((btn, checked) -> {
            blurEnabled = checked;
            statusBar.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (!checked && isBlurred) removeBlur();
            if (!checked && player != null && !player.isPlaying()) player.play();
        });
    }

    private void setupContinueButton() {
        View btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(v -> {
            removeBlur();
            if (player != null) player.play();
        });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(analysisRunnable);
        if (player != null) player.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(analysisRunnable, ANALYSIS_INTERVAL_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(analysisRunnable);
        if (labeler != null) labeler.close();
        if (player  != null) { player.release(); player = null; }
    }
}
