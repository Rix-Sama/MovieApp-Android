package com.lumio.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";

    private RecyclerView recyclerUsers;
    private TextView tvUserCount, tvStatAdmins, tvStatParents, tvStatEnfants;
    private ProgressBar progressBar;
    private EditText etFilter;
    private RequestQueue queue;
    private UsersAdapter adapter;
    private final List<JSONObject> allUsers  = new ArrayList<>();
    private final List<JSONObject> displayed = new ArrayList<>();
    private String authHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        authHeader = SessionManager.getInstance(this).getAuthHeader();
        queue      = Volley.newRequestQueue(this);

        ((LinearLayout) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());

        recyclerUsers   = findViewById(R.id.recyclerUsers);
        tvUserCount     = findViewById(R.id.tvUserCount);
        tvStatAdmins    = findViewById(R.id.tvStatAdmins);
        tvStatParents   = findViewById(R.id.tvStatParents);
        tvStatEnfants   = findViewById(R.id.tvStatEnfants);
        progressBar     = findViewById(R.id.progressBar);
        etFilter        = findViewById(R.id.etFilterUsers);

        adapter = new UsersAdapter();
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(adapter);

        etFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterUsers(s.toString());
            }
        });

        loadUsers();
    }

    @Override protected void onResume() { super.onResume(); loadUsers(); }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        JsonArrayRequest req = new JsonArrayRequest(
            Request.Method.GET, ApiClient.ADMIN_USERS, null,
            response -> {
                progressBar.setVisibility(View.GONE);
                allUsers.clear();
                for (int i = 0; i < response.length(); i++) {
                    try { allUsers.add(response.getJSONObject(i)); }
                    catch (Exception ignored) {}
                }
                updateStats();
                filterUsers(etFilter.getText().toString());
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Load error: " + error.getMessage());
                Toast.makeText(this, "Erreur chargement", Toast.LENGTH_SHORT).show();
            }
        ) {
            @Override public Map<String, String> getHeaders() {
                return Collections.singletonMap("Authorization", authHeader);
            }
        };
        queue.add(req);
    }

    private void updateStats() {
        int admins = 0, parents = 0, enfants = 0;
        for (JSONObject u : allUsers) {
            String role = u.optString("role", "");
            if ("ADMIN".equals(role))   admins++;
            else if ("PARENT".equals(role)) parents++;
            else enfants++;
        }
        tvUserCount.setText(allUsers.size() + " utilisateurs");
        tvStatAdmins.setText(String.valueOf(admins));
        tvStatParents.setText(String.valueOf(parents));
        tvStatEnfants.setText(String.valueOf(enfants));
    }

    private void filterUsers(String query) {
        displayed.clear();
        String q = query.toLowerCase().trim();
        for (JSONObject u : allUsers) {
            String nom    = u.optString("nom", "").toLowerCase();
            String prenom = u.optString("prenom", "").toLowerCase();
            String email  = u.optString("email", "").toLowerCase();
            String role   = u.optString("role", "").toLowerCase();
            if (q.isEmpty() || nom.contains(q) || prenom.contains(q)
                    || email.contains(q) || role.contains(q)) {
                displayed.add(u);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void toggleUser(long userId, int position) {
        String url = ApiClient.ADMIN_TOGGLE + userId + "/toggle";
        StringRequest req = new StringRequest(
            Request.Method.PUT, url,
            response -> {
                Toast.makeText(this, "Statut mis à jour", Toast.LENGTH_SHORT).show();
                loadUsers();
            },
            error -> Toast.makeText(this, "Erreur toggle", Toast.LENGTH_SHORT).show()
        ) {
            @Override public Map<String, String> getHeaders() {
                return Collections.singletonMap("Authorization", authHeader);
            }
        };
        queue.add(req);
    }

    private void confirmDeleteUser(long userId, String name) {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer l'utilisateur")
            .setMessage("Supprimer le compte de " + name + " ?")
            .setPositiveButton("Supprimer", (d, w) -> deleteUser(userId))
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void deleteUser(long userId) {
        String url = ApiClient.ADMIN_TOGGLE + userId;
        StringRequest req = new StringRequest(
            Request.Method.DELETE, url,
            response -> {
                Toast.makeText(this, "Utilisateur supprimé", Toast.LENGTH_SHORT).show();
                loadUsers();
            },
            error -> Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show()
        ) {
            @Override public Map<String, String> getHeaders() {
                return Collections.singletonMap("Authorization", authHeader);
            }
        };
        queue.add(req);
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_admin, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            try {
                JSONObject user  = displayed.get(position);
                String nom       = user.optString("nom", "");
                String prenom    = user.optString("prenom", "");
                String email     = user.optString("email", "");
                String role      = user.optString("role", "");
                boolean active   = user.optBoolean("active", true);
                long id          = user.optLong("id", 0);

                String initials = "";
                if (!prenom.isEmpty()) initials += prenom.charAt(0);
                if (!nom.isEmpty())    initials += nom.charAt(0);

                holder.tvAvatar.setText(initials.toUpperCase());
                holder.tvName.setText(prenom + " " + nom);
                holder.tvEmail.setText(email + (active ? "" : " — Désactivé"));
                holder.tvRole.setText(role);

                // Role badge color
                int color;
                switch (role) {
                    case "ADMIN":  color = 0xFFE50914; break;
                    case "PARENT": color = 0xFF1A56F5; break;
                    default:       color = 0xFF0F6E56; break;
                }
                holder.tvRole.getBackground().setTint(color);

                // Dim if inactive
                holder.itemView.setAlpha(active ? 1f : 0.5f);

                // Toggle on icon click
                holder.btnToggle.setOnClickListener(v ->
                    new AlertDialog.Builder(AdminDashboardActivity.this)
                        .setTitle(active ? "Désactiver" : "Activer")
                        .setMessage((active ? "Désactiver" : "Activer") + " le compte de " + prenom + " ?")
                        .setPositiveButton("Confirmer", (d, w) -> toggleUser(id, position))
                        .setNegativeButton("Annuler", null)
                        .show()
                );

                // Long press → delete
                holder.itemView.setOnLongClickListener(v -> {
                    confirmDeleteUser(id, prenom + " " + nom);
                    return true;
                });

            } catch (Exception ignored) {}
        }

        @Override public int getItemCount() { return displayed.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvEmail, tvRole;
            ImageView btnToggle;
            VH(@NonNull View v) {
                super(v);
                tvAvatar  = v.findViewById(R.id.tvUserAvatar);
                tvName    = v.findViewById(R.id.tvUserName);
                tvEmail   = v.findViewById(R.id.tvUserEmail);
                tvRole    = v.findViewById(R.id.tvUserRole);
                btnToggle = v.findViewById(R.id.btnToggleUser);
            }
        }
    }
}
