package com.lumio.app;

public class MyMovieData {
    private int movieId;
    private String movieName;
    private String movieDate;
    private String movieImage;      // poster_path (TMDB)
    private String backdropPath;    // backdrop_path (TMDB)
    private String overview;
    private double voteAverage;
    private boolean isAdult;
    private String certification;   // "R", "NC-17", etc.

    public MyMovieData(int movieId, String movieName, String movieDate,
                       String movieImage, String backdropPath,
                       String overview, double voteAverage, boolean isAdult) {
        this.movieId = movieId;
        this.movieName = movieName;
        this.movieDate = movieDate;
        this.movieImage = movieImage;
        this.backdropPath = backdropPath;
        this.overview = overview;
        this.voteAverage = voteAverage;
        this.isAdult = isAdult;
        this.certification = "";
    }

    // Getters
    public int getMovieId()       { return movieId; }
    public String getMovieName()  { return movieName; }
    public String getMovieDate()  { return movieDate; }
    public String getMovieImage() { return movieImage; }
    public String getBackdropPath(){ return backdropPath; }
    public String getOverview()   { return overview; }
    public double getVoteAverage(){ return voteAverage; }
    public boolean isAdult()      { return isAdult; }
    public String getCertification(){ return certification; }

    // Setters
    public void setAdult(boolean adult)               { isAdult = adult; }
    public void setCertification(String certification) { this.certification = certification; }

    /** Retourne l'année à partir de la date "YYYY-MM-DD" */
    public String getYear() {
        if (movieDate != null && movieDate.length() >= 4) return movieDate.substring(0, 4);
        return "—";
    }

    /** Score formaté "8.5" */
    public String getFormattedScore() {
        return String.format("%.1f", voteAverage);
    }
}
