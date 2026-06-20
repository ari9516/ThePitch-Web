package com.thepitch.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * PredictionResponse — DTO returned by the prediction endpoint.
 */
@Data
public class PredictionResponse {
    private Integer matchId;
    private String homeTeamName;
    private String awayTeamName;
    private double homeWinProb;
    private double drawProb;
    private double awayWinProb;
    private String confidence;
    private double confidenceScore;
    private List<String> insights;

    // Team form summary
    private String homeForm;
    private String awayForm;
    private double homeFormPct;
    private double awayFormPct;

    // ELO
    private int homeElo;
    private int awayElo;
}