package com.lumio.app;

public class ApiClient {

    // ⚠️ Changer selon votre environnement
    // Émulateur : http://10.0.2.2:8080/api/
    // Téléphone physique : http://192.168.1.X:8080/api/
    public static final String BASE_URL = "http://192.168.1.12:8080/api/";

    // Auth
    public static final String LOGIN    = BASE_URL + "auth/login";
    public static final String REGISTER = BASE_URL + "auth/register";

    // User
    public static final String PROFILE  = BASE_URL + "user/profile";
    public static final String HISTORY  = BASE_URL + "user/history";

    // Watchlist
    public static final String WATCHLIST       = BASE_URL + "watchlist";
    public static final String WATCHLIST_CHECK = BASE_URL + "watchlist/check/";

    // Parent
    public static final String CHILDREN        = BASE_URL + "parent/children";

    // Admin
    public static final String ADMIN_USERS     = BASE_URL + "admin/users";
    public static final String ADMIN_TOGGLE    = BASE_URL + "admin/users/";
}
