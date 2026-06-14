package com.thepitch.backend.controller;

import com.thepitch.backend.model.Match;
import com.thepitch.backend.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * MatchController — public REST endpoints for match data.
 * Base URL: /api/matches
 */
@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class MatchController {

    @Autowired
    private MatchRepository matchRepository;

    // GET /api/matches — all matches
    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok(matchRepository.findAll());
    }

    // GET /api/matches/upcoming — scheduled fixtures
    @GetMapping("/upcoming")
    public ResponseEntity<List<Match>> getUpcoming() {
        return ResponseEntity.ok(matchRepository.findUpcomingMatches());
    }

    // GET /api/matches/recent — finished matches
    @GetMapping("/recent")
    public ResponseEntity<List<Match>> getRecent() {
        return ResponseEntity.ok(matchRepository.findRecentMatches());
    }

    // GET /api/matches/{id} — single match
    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable Integer id) {
        Optional<Match> match = matchRepository.findById(id);
        return match.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/matches/team/{teamId} — all matches for a team
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Match>> getMatchesByTeam(@PathVariable Integer teamId) {
        return ResponseEntity.ok(matchRepository.findAllByTeamId(teamId));
    }

    // GET /api/matches/team/{teamId}/recent — finished matches for a team
    @GetMapping("/team/{teamId}/recent")
    public ResponseEntity<List<Match>> getRecentByTeam(@PathVariable Integer teamId) {
        return ResponseEntity.ok(matchRepository.findFinishedMatchesByTeam(teamId));
    }
}
