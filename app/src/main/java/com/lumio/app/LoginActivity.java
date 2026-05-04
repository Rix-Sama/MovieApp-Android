package com.lumio.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LUMIO_LOGIN";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SessionManager.getInstance(this).isLoggedIn()) { goToMain(); return; }

        setContentView(R.layout.activity_login);

        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        queue = Volley.newRequestQueue(this);

        btnLogin.setOnClickListener(v -> attemptLogin());

        View btnGoRegister = findViewById(R.id.btnGoRegister);
        if (btnGoRegister != null)
            btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        View tvRegister = findViewById(R.id.tvRegister);
        if (tvRegister != null)
            tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);

        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, ApiClient.LOGIN, body,
                response -> {
                    setLoading(false);
                    try {
                        SessionManager.getInstance(this).saveSession(
                            response.getString("token"),
                            response.getLong("id"),
                            response.getString("nom"),
                            response.getString("prenom"),
                            response.getString("email"),
                            response.getString("role"),
                            response.optInt("restrictionLevel", 1)
                        );
                        Toast.makeText(this,
                            "Bienvenue " + response.getString("prenom") + " ! 👋",
                            Toast.LENGTH_SHORT).show();
                        goToMain();
                    } catch (Exception e) {
                        Toast.makeText(this, "Erreur parsing", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    setLoading(false);
                    String msg;
                    if (error.networkResponse != null) {
                        msg = "Erreur " + error.networkResponse.statusCode + " : email ou mot de passe incorrect";
                    } else if (error.getCause() != null) {
                        msg = "Erreur réseau : " + error.getCause().getMessage();
                        Log.e(TAG, "Network error", error.getCause());
                    } else {
                        msg = "Impossible de joindre le serveur";
                        Log.e(TAG, "Unknown error: " + error.getMessage());
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            ) {
                @Override public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    h.put("Content-Type", "application/json");
                    h.put("Accept", "application/json");
                    return h;
                }
            };

            req.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1f));
            queue.add(req);
        } catch (Exception e) {
            setLoading(false);
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
