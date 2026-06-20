package com.thepitch.backend.service;

import com.thepitch.backend.dto.PredictionResponse;
import com.thepitch.backend.model.Match;
import com.thepitch.backend.model.Team;
import com.thepitch.backend.repository.MatchRepository;
import com.thepitch.backend.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * PredictionService — generates match predictions using ELO + recent form.
 * Simplified port of console app's EnhancedPredictionEngine for the web version.
 * Advanced stats (xG, tactical AI) will be layered in next.
 */
@Service
public class PredictionService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EloService eloService;

    public PredictionResponse generatePrediction(Integer matchId) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

        Team homeTeam = match.getHomeTeam();
        Team awayTeam = match.getAwayTeam();

        // ── Base ELO probabilities ────────────────────────────────────────────
        double baseHomeProb = eloService.getHomeWinProbability(homeTeam, awayTeam);
        double baseDrawProb = eloService.getDrawProbability(homeTeam, awayTeam);
        double baseAwayProb = eloService.getAwayWinProbability(homeTeam, awayTeam);

        double total = baseHomeProb + baseDrawProb + baseAwayProb;
        baseHomeProb /= total;
        baseDrawProb /= total;
        baseAwayProb /= total;

        // ── Recent form (last 5 matches) ──────────────────────────────────────
        FormResult homeForm = calculateForm(homeTeam.getTeamId());
        FormResult awayForm = calculateForm(awayTeam.getTeamId());

        double formAdjustment = (homeForm.formPct - awayForm.formPct) / 100.0 * 0.15;

        double finalHomeProb = baseHomeProb + formAdjustment;
        double finalAwayProb = baseAwayProb - formAdjustment;
        double finalDrawProb = baseDrawProb;

        finalHomeProb = clamp(finalHomeProb, 0.10, 0.80);
        finalAwayProb = clamp(finalAwayProb, 0.05, 0.70);
        finalDrawProb = clamp(finalDrawProb, 0.10, 0.35);

        double sum = finalHomeProb + finalDrawProb + finalAwayProb;
        finalHomeProb /= sum;
        finalDrawProb /= sum;
        finalAwayProb /= sum;

        // ── Build insights ────────────────────────────────────────────────────
        List<String> insights = new ArrayList<>();
        if (homeForm.formPct >= 70) {
            insights.add(homeTeam.getTeamName() + " are in excellent form (" +
                String.format("%.0f", homeForm.formPct) + "% points in last 5)");
        }
        if (awayForm.formPct >= 70) {
            insights.add(awayTeam.getTeamName() + " are in excellent away form (" +
                String.format("%.0f", awayForm.formPct) + "% points in last 5)");
        }
        if (homeForm.formPct <= 30) {
            insights.add(homeTeam.getTeamName() + " are out of form (" +
                String.format("%.0f", homeForm.formPct) + "% points in last 5)");
        }
        if (awayForm.formPct <= 30) {
            insights.add(awayTeam.getTeamName() + " are struggling away (" +
                String.format("%.0f", awayForm.formPct) + "% points in last 5)");
        }
        int eloDiff = homeTeam.getEloRating() - awayTeam.getEloRating();
        if (Math.abs(eloDiff) > 100) {
            String stronger = eloDiff > 0 ? homeTeam.getTeamName() : awayTeam.getTeamName();
            insights.add(stronger + " rated significantly stronger by ELO (diff: " +
                Math.abs(eloDiff) + " points)");
        }

        // ── Confidence ─────────────────────────────────────────────────────────
        double maxProb = Math.max(finalHomeProb, Math.max(finalDrawProb, finalAwayProb));
        double[] sorted = {finalHomeProb, finalDrawProb, finalAwayProb};
        java.util.Arrays.sort(sorted);
        double spread = sorted[2] - sorted[1];

        String confidence;
        if (maxProb >= 0.65 && spread >= 0.20) confidence = "VERY HIGH";
        else if (maxProb >= 0.55 && spread >= 0.15) confidence = "HIGH";
        else if (maxProb >= 0.45 && spread >= 0.10) confidence = "MEDIUM";
        else confidence = "LOW";

        // ── Build response ────────────────────────────────────────────────────
        PredictionResponse response = new PredictionResponse();
        response.setMatchId(matchId);
        response.setHomeTeamName(homeTeam.getTeamName());
        response.setAwayTeamName(awayTeam.getTeamName());
        response.setHomeWinProb(round(finalHomeProb * 100));
        response.setDrawProb(round(finalDrawProb * 100));
        response.setAwayWinProb(round(finalAwayProb * 100));
        response.setConfidence(confidence);
        response.setConfidenceScore(round(maxProb * 100));
        response.setInsights(insights);
        response.setHomeForm(homeForm.formString);
        response.setAwayForm(awayForm.formString);
        response.setHomeFormPct(homeForm.formPct);
        response.setAwayFormPct(awayForm.formPct);
        response.setHomeElo(homeTeam.getEloRating());
        response.setAwayElo(awayTeam.getEloRating());

        return response;
    }

    // ── Form calculation helper ───────────────────────────────────────────────

    private FormResult calculateForm(Integer teamId) {
        List<Match> matches = matchRepository.findFinishedMatchesByTeam(teamId);
        List<Match> last5 = matches.subList(0, Math.min(5, matches.size()));

        StringBuilder form = new StringBuilder();
        int points = 0;

        for (Match m : last5) {
            boolean isHome = m.getHomeTeam().getTeamId().equals(teamId);
            int scored   = isHome ? m.getHomeScore() : m.getAwayScore();
            int conceded = isHome ? m.getAwayScore() : m.getHomeScore();

            if (scored > conceded)      { form.append("W"); points += 3; }
            else if (scored == conceded) { form.append("D"); points += 1; }
            else                          { form.append("L"); }
        }

        double formPct = last5.isEmpty() ? 0 : (points / (double)(last5.size() * 3)) * 100;

        FormResult result = new FormResult();
        result.formString = form.toString();
        result.formPct = formPct;
        return result;
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private static class FormResult {
        String formString;
        double formPct;
    }
}
