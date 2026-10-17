package com.situnes.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * JSON endpoints for the matchup arena (called via HTMX/fetch from matchup.html).
 * TODO: implement with MatchupService. Resolve the current user from the session
 * once auth lands (SecurityConfig).
 */
@RestController
@RequestMapping("/api/matchups")
public class MatchupController {

    public record VoteRequest(Long winnerSongId, Long loserSongId, String genre) {}

    @PostMapping("/vote")
    public ResponseEntity<Void> vote(@RequestBody VoteRequest request) {
        throw new UnsupportedOperationException("TODO: matchupService.recordResult(...)");
    }
}
