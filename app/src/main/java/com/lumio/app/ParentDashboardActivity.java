package com.lumio.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class ParentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ParentDashboard";

    private RecyclerView recyclerChildren;
    private View emptyChildren;
    private ProgressBar progressBar;
    private RequestQueue queue;
    private ChildrenAdapter adapter;
    private final List<JSONObject> children = new ArrayList<>();
    private String authHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        authHeader = SessionManager.getInstance(this).getAuthHeader();
        queue      = Volley.newRequestQueue(this);

        ((LinearLayout) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());

        recyclerChildren = findViewById(R.id.recyclerChildren);
        emptyChildren    = findViewById(R.id.emptyChildren);
        progressBar      = findViewById(R.id.progressBar);

        adapter = new ChildrenAdapter();
        recyclerChildren.setLayoutManager(new LinearLayoutManager(this));
        recyclerChildren.setAdapter(adapter);

        findViewById(R.id.btnAddChild).setOnClickListener(v -> showAddChildDialog());

        loadChildren();
    }

    @Override protected void onResume() { super.onResume(); loadChildren(); }

    private void loadChildren() {
        progressBar.setVisibility(View.VISIBLE);
        JsonArrayRequest req = new JsonArrayRequest(
            Request.Method.GET, ApiClient.CHILDREN, null,
            response -> {
                progressBar.setVisibility(View.GONE);
                children.clear();
                for (int i = 0; i < response.length(); i++) {
                    try { children.add(response.getJSONObject(i)); }
                    catch (Exception ignored) {}
                }
                adapter.notifyDataSetChanged();
                updateState();
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Load error: " + error.getMessage());
            }
        ) {
            @Override public Map<String, String> getHeaders() {
                return Collections.singletonMap("Authorization", authHeader);
            }
        };
        queue.add(req);
    }

    private void updateState() {
        if (children.isEmpty()) {
            recyclerChildren.setVisibility(View.GONE);
            emptyChildren.setVisibility(View.VISIBLE);
        } else {
            recyclerChildren.setVisibility(View.VISIBLE);
            emptyChildren.setVisibility(View.GONE);
        }
    }

    private void showAddChildDialog() {
        View dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_child, null);

        new AlertDialog.Builder(this)
            .setTitle("Créer un compte enfant")
            .setView(dialogView)
            .setPositiveButton("Créer", (d, w) -> {
                EditText etNom    = dialogView.findViewById(R.id.etChildNom);
                EditText etPrenom = dialogView.findViewById(R.id.etChildPrenom);
                EditText etEmail  = dialogView.findViewById(R.id.etChildEmail);
                EditText etPwd    = dialogView.findViewById(R.id.etChildPassword);

                String nom    = etNom.getText().toString().trim();
                String prenom = etPrenom.getText().toString().trim();
                String email  = etEmail.getText().toString().trim();
                String pwd    = etPwd.getText().toString().trim();

                if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || pwd.length() < 6) {
                    Toast.makeText(this, "Veuillez remplir tous les champs (6 car. min.)",
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                createChild(nom, prenom, email, pwd);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void createChild(String nom, String prenom, String email, String pwd) {
        try {
            JSONObject body = new JSONObject();
            body.put("nom", nom);
            body.put("prenom", prenom);
            body.put("email", email);
            body.put("password", pwd);
            body.put("role", "ENFANT");

            JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, ApiClient.CHILDREN, body,
                response -> {
                    Toast.makeText(this, "✅ Compte enfant créé !", Toast.LENGTH_SHORT).show();
                    loadChildren();
                },
                error -> {
                    String msg = "Erreur lors de la création";
                    if (error.networkResponse != null && error.networkResponse.data != null)
                        msg = new String(error.networkResponse.data);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            ) {
                @Override public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    h.put("Authorization", authHeader);
                    h.put("Content-Type", "application/json");
                    return h;
                }
            };
            queue.add(req);
        } catch (Exception e) {
            Log.e(TAG, "Create child error", e);
        }
    }

    private void updateRestriction(long childId, int level, int position) {
        try {
            String url = ApiClient.CHILDREN + "/" + childId + "/restriction";
            JSONObject body = new JSONObject();
            body.put("level", level + 1); // slider 0-2 → level 1-3

            JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT, url, body,
                response -> { /* silent update */ },
                error -> Log.e(TAG, "Restriction update error")
            ) {
                @Override public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    h.put("Authorization", authHeader);
                    h.put("Content-Type", "application/json");
                    return h;
                }
            };
            queue.add(req);
        } catch (Exception e) {
            Log.e(TAG, "updateRestriction error", e);
        }
    }

    private void deleteChild(long childId, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Supprimer")
            .setMessage("Supprimer ce compte enfant ?")
            .setPositiveButton("Supprimer", (d, w) -> {
                String url = ApiClient.CHILDREN + "/" + childId;
                StringRequest req = new StringRequest(
                    Request.Method.DELETE, url,
                    response -> {
                        if (position < children.size()) children.remove(position);
                        adapter.notifyDataSetChanged();
                        updateState();
                        Toast.makeText(this, "Compte supprimé", Toast.LENGTH_SHORT).show();
                    },
                    error -> Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show()
                ) {
                    @Override public Map<String, String> getHeaders() {
                        return Collections.singletonMap("Authorization", authHeader);
                    }
                };
                queue.add(req);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class ChildrenAdapter extends RecyclerView.Adapter<ChildrenAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            try {
                JSONObject child = children.get(position);
                String nom    = child.optString("nom", "");
                String prenom = child.optString("prenom", "");
                String email  = child.optString("email", "");
                long id       = child.optLong("id", 0);
                int restrict  = child.optInt("restrictionLevel", 1);
                String[] labels = {"Strict", "Modéré", "Permissif"};

                String initials = "";
                if (!prenom.isEmpty()) initials += prenom.charAt(0);
                if (!nom.isEmpty())    initials += nom.charAt(0);
                holder.tvAvatar.setText(initials.toUpperCase());
                holder.tvName.setText(prenom + " " + nom);
                holder.tvEmail.setText(email);

                holder.seekRestriction.setOnSeekBarChangeListener(null);
                holder.seekRestriction.setProgress(Math.max(0, restrict - 1));
                holder.tvRestrictionLabel.setText(labels[Math.max(0, restrict - 1)]);

                holder.seekRestriction.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                        @Override public void onProgressChanged(SeekBar sb, int prog, boolean user) {
                            holder.tvRestrictionLabel.setText(labels[prog]);
                        }
                        @Override public void onStartTrackingTouch(SeekBar sb) {}
                        @Override public void onStopTrackingTouch(SeekBar sb) {
                            updateRestriction(id, sb.getProgress(), position);
                        }
                    });

                holder.btnDelete.setOnClickListener(v -> deleteChild(id, position));

            } catch (Exception ignored) {}
        }

        @Override public int getItemCount() { return children.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvEmail, tvRestrictionLabel;
            SeekBar seekRestriction;
            ImageView btnDelete;
            VH(@NonNull View v) {
                super(v);
                tvAvatar            = v.findViewById(R.id.tvChildAvatar);
                tvName              = v.findViewById(R.id.tvChildName);
                tvEmail             = v.findViewById(R.id.tvChildEmail);
                seekRestriction     = v.findViewById(R.id.seekRestriction);
                tvRestrictionLabel  = v.findViewById(R.id.tvRestrictionLabel);
                btnDelete           = v.findViewById(R.id.btnDeleteChild);
            }
        }
    }
}
