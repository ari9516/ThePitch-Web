package com.thepitch.backend.controller;

import com.thepitch.backend.model.Team;
import com.thepitch.backend.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * TeamController — public REST endpoints for team data.
 * Base URL: /api/teams
 */
@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class TeamController {

    @Autowired
    private TeamRepository teamRepository;

    // GET /api/teams — all teams sorted by ELO
    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        List<Team> teams = teamRepository.findTop20ByOrderByEloRatingDesc();
        return ResponseEntity.ok(teams);
    }

    // GET /api/teams/{id} — single team by ID
    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeamById(@PathVariable Integer id) {
        Optional<Team> team = teamRepository.findById(id);
        return team.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/teams/search?name=arsenal — search by name
    @GetMapping("/search")
    public ResponseEntity<List<Team>> searchTeams(@RequestParam String name) {
        List<Team> teams = teamRepository.searchByName(name);
        return ResponseEntity.ok(teams);
    }

    // GET /api/teams/league/{leagueId} — teams by league
    @GetMapping("/league/{leagueId}")
    public ResponseEntity<List<Team>> getTeamsByLeague(@PathVariable Integer leagueId) {
        List<Team> teams = teamRepository.findByLeagueIdOrderByEloRatingDesc(leagueId);
        return ResponseEntity.ok(teams);
    }
}