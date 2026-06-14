package com.thepitch.backend.repository;

import com.thepitch.backend.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Integer> {

    // All matches for a team (home or away)
    @Query("SELECT m FROM Match m WHERE m.homeTeam.teamId = :teamId OR m.awayTeam.teamId = :teamId ORDER BY m.matchDate DESC")
    List<Match> findAllByTeamId(@Param("teamId") Integer teamId);

    // Upcoming scheduled matches
    @Query("SELECT m FROM Match m WHERE m.status = 'SCHEDULED' ORDER BY m.matchDate ASC")
    List<Match> findUpcomingMatches();

    // Last N finished matches
    @Query("SELECT m FROM Match m WHERE m.status = 'FINISHED' ORDER BY m.matchDate DESC")
    List<Match> findRecentMatches();

    // Finished matches for a specific team
    @Query("SELECT m FROM Match m WHERE m.status = 'FINISHED' AND (m.homeTeam.teamId = :teamId OR m.awayTeam.teamId = :teamId) ORDER BY m.matchDate DESC")
    List<Match> findFinishedMatchesByTeam(@Param("teamId") Integer teamId);

    // Matches by league
    List<Match> findByLeagueIdOrderByMatchDateDesc(Integer leagueId);

    // Count finished matches
    long countByStatus(String status);
}
