package com.thepitch.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Team entity — maps to "teams" table in PostgreSQL.
 * Equivalent to your console app's Team model + teams table.
 */
@Entity
@Table(name = "teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(name = "league_id")
    private Integer leagueId;

    @Column(name = "elo_rating")
    private Integer eloRating = 1500;

    @Column(name = "last_updated")
    private String lastUpdated;
}