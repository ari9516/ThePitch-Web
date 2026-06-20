package com.thepitch.backend.controller;

import com.thepitch.backend.dto.PredictionResponse;
import com.thepitch.backend.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PredictionController — public REST endpoint for match predictions.
 * Base URL: /api/predictions
 */
@RestController
@RequestMapping("/api/predictions")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    // GET /api/predictions/{matchId} — generate prediction for a match
    @GetMapping("/{matchId}")
    public ResponseEntity<?> getPrediction(@PathVariable Integer matchId) {
        try {
            PredictionResponse prediction = predictionService.generatePrediction(matchId);
            return ResponseEntity.ok(prediction);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}