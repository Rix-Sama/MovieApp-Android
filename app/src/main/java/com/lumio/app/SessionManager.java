package com.lumio.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestionnaire de session : stocke le token JWT et les infos utilisateur
 * dans SharedPreferences après connexion.
 */
public class SessionManager {

    private static final String PREFS_NAME   = "lumio_session";
    private static final String KEY_TOKEN    = "token";
    private static final String KEY_USER_ID  = "user_id";
    private static final String KEY_NOM      = "nom";
    private static final String KEY_PRENOM   = "prenom";
    private static final String KEY_EMAIL    = "email";
    private static final String KEY_ROLE     = "role";
    private static final String KEY_RESTRICT = "restriction_level";

    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) instance = new SessionManager(context);
        return instance;
    }

    public void saveSession(String token, long userId, String nom, String prenom,
                            String email, String role, int restrictionLevel) {
        prefs.edit()
             .putString(KEY_TOKEN, token)
             .putLong(KEY_USER_ID, userId)
             .putString(KEY_NOM, nom)
             .putString(KEY_PRENOM, prenom)
             .putString(KEY_EMAIL, email)
             .putString(KEY_ROLE, role)
             .putInt(KEY_RESTRICT, restrictionLevel)
             .apply();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn()         { return getToken() != null; }
    public String getToken()            { return prefs.getString(KEY_TOKEN, null); }
    public long getUserId()             { return prefs.getLong(KEY_USER_ID, -1); }
    public String getNom()              { return prefs.getString(KEY_NOM, ""); }
    public String getPrenom()           { return prefs.getString(KEY_PRENOM, ""); }
    public String getEmail()            { return prefs.getString(KEY_EMAIL, ""); }
    public String getRole()             { return prefs.getString(KEY_ROLE, "ENFANT"); }
    public int getRestrictionLevel()    { return prefs.getInt(KEY_RESTRICT, 1); }

    public boolean isAdmin()            { return "ADMIN".equals(getRole()); }
    public boolean isParent()           { return "PARENT".equals(getRole()); }
    public boolean isEnfant()           { return "ENFANT".equals(getRole()); }

    /** Retourne le header Authorization pour les appels API */
    public String getAuthHeader()       { return "Bearer " + getToken(); }
}
