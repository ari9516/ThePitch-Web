package com.thepitch.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Match entity — maps to "matches" table in PostgreSQL.
 */
@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @Column(name = "match_id")
    private Integer matchId;

    @Column(name = "match_date")
    private LocalDateTime matchDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    @Column(name = "league_id")
    private Integer leagueId;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "status")
    private String status = "SCHEDULED";

    // ── Helper methods ────────────────────────────────────────────────────────

    public boolean isFinished() {
        return "FINISHED".equalsIgnoreCase(status);
    }

    public boolean isScheduled() {
        return "SCHEDULED".equalsIgnoreCase(status);
    }

    public String getResult() {
        if (!isFinished() || homeScore == null || awayScore == null) return "N/A";
        if (homeScore > awayScore) return "HOME_WIN";
        if (awayScore > homeScore) return "AWAY_WIN";
        return "DRAW";
    }

    public int getTotalGoals() {
        if (homeScore == null || awayScore == null) return 0;
        return homeScore + awayScore;
    }
}