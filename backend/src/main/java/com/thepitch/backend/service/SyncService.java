package com.thepitch.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thepitch.backend.model.Match;
import com.thepitch.backend.model.Team;
import com.thepitch.backend.repository.MatchRepository;
import com.thepitch.backend.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * SyncService — fetches Premier League data from football-data.org
 * and saves teams + matches to Neon PostgreSQL.
 */
@Service
public class SyncService {

    private static final String BASE_URL   = "https://api.football-data.org/v4";
    private static final int    LEAGUE_ID  = 2021; // Premier League

    @Value("${football.api.key}")
    private String apiKey;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper   = new ObjectMapper();

    // ── Main sync entry point ─────────────────────────────────────────────────

    public Map<String, Object> syncPremierLeague() {
        Map<String, Object> result = new HashMap<>();

        try {
            int teamsCount   = syncTeams();
            int matchesCount = syncMatches();

            result.put("success", true);
            result.put("teams",   teamsCount);
            result.put("matches", matchesCount);
            result.put("message", "Sync complete: " + teamsCount + " teams, " + matchesCount + " matches");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Sync failed: " + e.getMessage());
        }

        return result;
    }

    // ── Sync teams ────────────────────────────────────────────────────────────

    private int syncTeams() throws Exception {
        String url  = BASE_URL + "/competitions/" + LEAGUE_ID + "/teams";
        String json = fetchFromApi(url);

        JsonNode root  = mapper.readTree(json);
        JsonNode teams = root.get("teams");

        if (teams == null || !teams.isArray()) return 0;

        int count = 0;
        for (JsonNode t : teams) {
            Team team = new Team();
            team.setTeamId(t.get("id").asInt());
            team.setTeamName(t.get("name").asText());
            team.setLeagueId(LEAGUE_ID);

            // Keep existing ELO if team already exists
            teamRepository.findById(team.getTeamId()).ifPresent(existing ->
                team.setEloRating(existing.getEloRating())
            );

            if (team.getEloRating() == null) team.setEloRating(1500);
            team.setLastUpdated(LocalDateTime.now().toString());

            teamRepository.save(team);
            count++;
        }

        return count;
    }

    // ── Sync matches ──────────────────────────────────────────────────────────

    private int syncMatches() throws Exception {
        String url  = BASE_URL + "/competitions/" + LEAGUE_ID + "/matches";
        String json = fetchFromApi(url);

        JsonNode root    = mapper.readTree(json);
        JsonNode matches = root.get("matches");

        if (matches == null || !matches.isArray()) return 0;

        int count = 0;
        for (JsonNode m : matches) {
            try {
                int matchId = m.get("id").asInt();

                // Get home/away team IDs
                int homeTeamId = m.get("homeTeam").get("id").asInt();
                int awayTeamId = m.get("awayTeam").get("id").asInt();

                // Only save if both teams exist in our DB
                Team homeTeam = teamRepository.findById(homeTeamId).orElse(null);
                Team awayTeam = teamRepository.findById(awayTeamId).orElse(null);
                if (homeTeam == null || awayTeam == null) continue;

                Match match = new Match();
                match.setMatchId(matchId);
                match.setHomeTeam(homeTeam);
                match.setAwayTeam(awayTeam);
                match.setLeagueId(LEAGUE_ID);

                // Parse date
                String utcDate = m.get("utcDate").asText();
                match.setMatchDate(LocalDateTime.parse(
                    utcDate, DateTimeFormatter.ISO_DATE_TIME
                ));

                // Status
                String status = m.get("status").asText();
                if ("FINISHED".equals(status))   match.setStatus("FINISHED");
                else if ("TIMED".equals(status) || "SCHEDULED".equals(status))
                                                  match.setStatus("SCHEDULED");
                else                              match.setStatus(status);

                // Score
                JsonNode score = m.get("score");
                if (score != null) {
                    JsonNode full = score.get("fullTime");
                    if (full != null) {
                        JsonNode home = full.get("home");
                        JsonNode away = full.get("away");
                        if (home != null && !home.isNull()) match.setHomeScore(home.asInt());
                        if (away != null && !away.isNull()) match.setAwayScore(away.asInt());
                    }
                }

                matchRepository.save(match);
                count++;

            } catch (Exception e) {
                // Skip problematic match, continue
                System.err.println("Skipping match: " + e.getMessage());
            }
        }

        return count;
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private String fetchFromApi(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("X-Auth-Token", apiKey)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("API error: HTTP " + response.statusCode());
        }

        return response.body();
    }
}