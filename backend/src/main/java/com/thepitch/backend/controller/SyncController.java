package com.thepitch.backend.controller;

import com.thepitch.backend.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SyncController — admin endpoint to trigger data sync.
 * POST /api/admin/sync
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class SyncController {

    @Autowired
    private SyncService syncService;

    // POST /api/admin/sync — trigger full Premier League sync
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sync() {
        Map<String, Object> result = syncService.syncPremierLeague();
        boolean success = (boolean) result.get("success");
        return success
            ? ResponseEntity.ok(result)
            : ResponseEntity.internalServerError().body(result);
    }
}