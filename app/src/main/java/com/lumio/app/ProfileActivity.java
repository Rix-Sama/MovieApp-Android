package com.lumio.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SessionManager session = SessionManager.getInstance(this);

        // Back
        LinearLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Avatar
        TextView tvAvatar = findViewById(R.id.tvAvatarBig);
        String initials = "";
        if (!session.getPrenom().isEmpty()) initials += session.getPrenom().charAt(0);
        if (!session.getNom().isEmpty())    initials += session.getNom().charAt(0);
        tvAvatar.setText(initials.toUpperCase());

        // Name
        TextView tvFullName = findViewById(R.id.tvFullName);
        tvFullName.setText(session.getPrenom() + " " + session.getNom());

        // Role badge
        TextView tvRole = findViewById(R.id.tvRoleBadge);
        tvRole.setText(session.getRole());
        int badgeColor;
        switch (session.getRole()) {
            case "ADMIN":  badgeColor = 0xFFE50914; break;
            case "PARENT": badgeColor = 0xFF1A56F5; break;
            default:       badgeColor = 0xFF0F6E56; break;
        }
        tvRole.getBackground().setTint(badgeColor);

        // Email
        TextView tvEmail = findViewById(R.id.tvEmail);
        tvEmail.setText(session.getEmail());

        // Restriction level
        TextView tvRestriction = findViewById(R.id.tvRestriction);
        String[] levels = {"Strict (tout filtrer)", "Modéré", "Permissif"};
        int lvl = session.getRestrictionLevel();
        tvRestriction.setText(levels[Math.max(0, Math.min(lvl - 1, 2))]);

        // Dashboard button (parent + admin)
        Button btnDashboard = findViewById(R.id.btnDashboard);
        if (session.isParent() || session.isAdmin()) {
            btnDashboard.setVisibility(View.VISIBLE);
            btnDashboard.setOnClickListener(v -> {
                Class<?> target = session.isAdmin()
                    ? AdminDashboardActivity.class
                    : ParentDashboardActivity.class;
                startActivity(new Intent(this, target));
            });
        }

        // Logout
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vous déconnecter ?")
                .setPositiveButton("Oui", (d, w) -> {
                    session.clearSession();
                    startActivity(new Intent(this, LoginActivity.class));
                    finishAffinity();
                })
                .setNegativeButton("Annuler", null)
                .show()
        );
    }
}
