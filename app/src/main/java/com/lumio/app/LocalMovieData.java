package com.lumio.app;

import android.net.Uri;

public class LocalMovieData {
    private long id;
    private String displayName;   // nom du fichier sans extension
    private String filePath;
    private Uri fileUri;
    private long fileSizeBytes;
    private String mimeType;
    private boolean blurEnabled;  // l'utilisateur peut activer/désactiver le blur sur ce film

    public LocalMovieData(long id, String displayName, String filePath,
                          Uri fileUri, long fileSizeBytes, String mimeType) {
        this.id = id;
        this.displayName = displayName;
        this.filePath = filePath;
        this.fileUri = fileUri;
        this.fileSizeBytes = fileSizeBytes;
        this.mimeType = mimeType;
        this.blurEnabled = true; // blur activé par défaut
    }

    public long getId()           { return id; }
    public String getDisplayName(){ return displayName; }
    public String getFilePath()   { return filePath; }
    public Uri getFileUri()       { return fileUri; }
    public long getFileSizeBytes(){ return fileSizeBytes; }
    public String getMimeType()   { return mimeType; }
    public boolean isBlurEnabled(){ return blurEnabled; }
    public void setBlurEnabled(boolean enabled) { blurEnabled = enabled; }

    /** Taille formatée : "1.2 GB", "345 MB" etc. */
    public String getFormattedSize() {
        if (fileSizeBytes <= 0) return "Taille inconnue";
        double mb = fileSizeBytes / (1024.0 * 1024.0);
        if (mb >= 1024) return String.format("%.1f GB", mb / 1024);
        return String.format("%.0f MB", mb);
    }

    /** Extension du fichier en majuscules */
    public String getExtension() {
        int dot = displayName.lastIndexOf('.');
        if (dot >= 0 && dot < displayName.length() - 1)
            return displayName.substring(dot + 1).toUpperCase();
        return "VID";
    }
}
