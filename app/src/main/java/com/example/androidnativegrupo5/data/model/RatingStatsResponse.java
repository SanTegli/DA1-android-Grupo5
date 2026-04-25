package com.example.androidnativegrupo5.data.model;

public class RatingStatsResponse {
    private Double averageActivityScore;
    private Double averageGuideScore;
    private Long totalRatings;

    public Double getAverageActivityScore() { return averageActivityScore; }
    public void setAverageActivityScore(Double averageActivityScore) { this.averageActivityScore = averageActivityScore; }
    public Double getAverageGuideScore() { return averageGuideScore; }
    public void setAverageGuideScore(Double averageGuideScore) { this.averageGuideScore = averageGuideScore; }
    public Long getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Long totalRatings) { this.totalRatings = totalRatings; }
}
