package com.thepitch.backend.service;

import com.thepitch.backend.model.Match;
import com.thepitch.backend.model.Team;
import org.springframework.stereotype.Service;

/**
 * EloService — ELO rating calculations.
 * Direct port of console app's EloCalculator.java
 */
@Service
public class EloService {

    private static final int K_FACTOR = 32;
    private static final int HOME_ADVANTAGE = 50;

    public double getExpectedScoreWithHomeAdvantage(int homeRating, int awayRating) {
        return 1.0 / (1.0 + Math.pow(10, (awayRating - (homeRating + HOME_ADVANTAGE)) / 400.0));
    }

    public double getHomeWinProbability(Team homeTeam, Team awayTeam) {
        return getExpectedScoreWithHomeAdvantage(homeTeam.getEloRating(), awayTeam.getEloRating());
    }

    public double getAwayWinProbability(Team homeTeam, Team awayTeam) {
        return 1.0 - getExpectedScoreWithHomeAdvantage(homeTeam.getEloRating(), awayTeam.getEloRating());
    }

    public double getDrawProbability(Team homeTeam, Team awayTeam) {
        double ratingDiff = Math.abs(homeTeam.getEloRating() - awayTeam.getEloRating());
        double drawBase = 0.25;
        double reduction = Math.min(0.15, ratingDiff / 1300.0);
        return Math.max(0.10, drawBase - reduction);
    }

    /**
     * Calculate new ELO ratings after a finished match.
     * Returns int[] {newHomeElo, newAwayElo}
     */
    public int[] calculateNewRatings(Match match) {
        Team homeTeam = match.getHomeTeam();
        Team awayTeam = match.getAwayTeam();

        double expectedHome = getExpectedScoreWithHomeAdvantage(
            homeTeam.getEloRating(), awayTeam.getEloRating()
        );
        double expectedAway = 1.0 - expectedHome;

        double actualHome, actualAway;
        String result = match.getResult();

        if ("HOME_WIN".equals(result)) {
            actualHome = 1.0; actualAway = 0.0;
        } else if ("AWAY_WIN".equals(result)) {
            actualHome = 0.0; actualAway = 1.0;
        } else {
            actualHome = 0.5; actualAway = 0.5;
        }

        int homeChange = (int) Math.round(K_FACTOR * (actualHome - expectedHome));
        int awayChange = (int) Math.round(K_FACTOR * (actualAway - expectedAway));

        int newHomeElo = homeTeam.getEloRating() + homeChange;
        int newAwayElo = awayTeam.getEloRating() + awayChange;

        return new int[]{newHomeElo, newAwayElo};
    }
}
