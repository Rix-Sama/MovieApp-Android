package com.lumio.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "LUMIO_REGISTER";

    private EditText etNom, etPrenom, etEmail, etPassword;
    private RadioGroup rgRole;
    private RadioButton rbParent, rbEnfant;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNom       = findViewById(R.id.etNom);
        etPrenom    = findViewById(R.id.etPrenom);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        rgRole      = findViewById(R.id.rgRole);
        rbParent    = findViewById(R.id.rbParent);
        rbEnfant    = findViewById(R.id.rbEnfant);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin     = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        queue = Volley.newRequestQueue(this);

        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String nom      = etNom.getText().toString().trim();
        String prenom   = etPrenom.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Mot de passe trop court (6 min)", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = (rgRole.getCheckedRadioButtonId() == R.id.rbEnfant) ? "ENFANT" : "PARENT";
        setLoading(true);

        try {
            JSONObject body = new JSONObject();
            body.put("nom", nom);
            body.put("prenom", prenom);
            body.put("email", email);
            body.put("password", password);
            body.put("role", role);

            Log.d(TAG, "Envoi vers " + ApiClient.REGISTER + " → " + body.toString());

            JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ApiClient.REGISTER,
                body,
                response -> {
                    setLoading(false);
                    Log.d(TAG, "Réponse reçue : " + response.toString());
                    try {
                        String token    = response.getString("token");
                        long userId     = response.getLong("id");
                        String rNom     = response.getString("nom");
                        String rPrenom  = response.getString("prenom");
                        String rEmail   = response.getString("email");
                        String rRole    = response.getString("role");
                        int restriction = response.optInt("restrictionLevel", 1);

                        SessionManager.getInstance(this).saveSession(
                            token, userId, rNom, rPrenom, rEmail, rRole, restriction);

                        Toast.makeText(this,
                            "Bienvenue " + rPrenom + " ! 🎉", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finishAffinity();
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur parsing réponse", e);
                        Toast.makeText(this, "Erreur parsing : " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    setLoading(false);
                    String msg;
                    if (error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        String errBody = error.networkResponse.data != null
                            ? new String(error.networkResponse.data) : "(vide)";
                        msg = "Erreur HTTP " + code + " : " + errBody;
                        Log.e(TAG, "HTTP " + code + " → " + errBody);
                    } else if (error.getCause() != null) {
                        msg = "Erreur réseau : " + error.getCause().getMessage();
                        Log.e(TAG, "Cause : " + error.getCause().getMessage(), error.getCause());
                    } else {
                        msg = "Impossible de joindre le serveur (" + error.getMessage() + ")";
                        Log.e(TAG, "Erreur inconnue : " + error.getMessage());
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    h.put("Content-Type", "application/json");
                    h.put("Accept", "application/json");
                    return h;
                }
            };

            req.setRetryPolicy(new DefaultRetryPolicy(
                15000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(req);

        } catch (Exception e) {
            setLoading(false);
            Log.e(TAG, "Exception création requête", e);
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
