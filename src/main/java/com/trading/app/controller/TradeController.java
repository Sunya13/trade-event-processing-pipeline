package com.trading.app.controller;

import com.trading.app.dto.PageResult;
import com.trading.app.dto.TradeAggregate;
import com.trading.app.dto.TradeSubmission;
import com.trading.app.service.TradeExportService;
import com.trading.app.service.TradeReadService;
import com.trading.app.service.TradeWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeReadService readService;
    private final TradeWriteService writeService;
    private final TradeExportService exportService;

    // GET /api/trades?search=...&page=0&size=10
    @GetMapping
    public ResponseEntity<PageResult<TradeAggregate>> getTrades(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) { // Defaulting to 100 to simplify UI integration

        PageResult<TradeAggregate> result = readService.getTradeDashboard(search, page, size);
        return ResponseEntity.ok(result);
    }

    // POST /api/trades (Handles both BOOK and AMEND via mode field)
    @PostMapping
    public ResponseEntity<Void> saveTrade(@RequestBody TradeSubmission submission) {
        writeService.handleSubmission(submission);
        return ResponseEntity.ok().build();
    }

    // POST /api/trades/{id}/cancel
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelTrade(@PathVariable String id) {
        writeService.cancelTrade(id);
        return ResponseEntity.ok().build();
    }

    // POST /api/trades/{id}/verify
    @PostMapping("/{id}/verify")
    public ResponseEntity<Void> verifyTrade(@PathVariable String id) {
        writeService.verifyTrade(id);
        return ResponseEntity.ok().build();
    }

    // GET /api/trades/export
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        String csvData = exportService.generateCsvExport();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trade_blotter.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csvData.getBytes());
    }
}