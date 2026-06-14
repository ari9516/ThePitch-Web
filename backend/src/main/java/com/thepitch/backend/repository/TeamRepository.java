package com.thepitch.backend.repository;

import com.thepitch.backend.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TeamRepository — Spring Data JPA repository for Team entity.
 * No implementation needed — Spring generates all SQL automatically.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {

    // Find team by exact name
    Optional<Team> findByTeamName(String teamName);

    // Find all teams in a specific league, ordered by ELO descending
    List<Team> findByLeagueIdOrderByEloRatingDesc(Integer leagueId);

    // Find top N teams by ELO rating
    List<Team> findTop20ByOrderByEloRatingDesc();

    // Search teams by name (case-insensitive, partial match)
    @Query("SELECT t FROM Team t WHERE LOWER(t.teamName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Team> searchByName(String name);
}
