package com.trading.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class TradeController {

    private final TradeBookingService service;
    private final ObjectMapper mapper = new ObjectMapper();

    public TradeController(TradeBookingService service) {
        this.service = service;
    }

    // Updated: Accepts optional 'search' param
    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("tradeAggregates", service.getTradeDashboard(search));
        model.addAttribute("searchQuery", search); // Pass back to UI to keep input filled
        return "dashboard";
    }

    @GetMapping("/book")
    public String showBookForm(Model model) {
        model.addAttribute("mode", "BOOK");
        model.addAttribute("trade", new TradeEvent());
        return "trade-form";
    }

    @GetMapping("/amend/{id}")
    public String showAmendForm(@PathVariable String id, Model model) {
        TradeEvent existing = service.getTradeById(id);
        try {
            JsonNode node = mapper.readTree(existing.getData());
            model.addAttribute("counterparty", node.get("counterparty").asText());
            model.addAttribute("notional", node.get("notional_amount").asLong());
        } catch (Exception e) {
            e.printStackTrace();
        }
        model.addAttribute("mode", "AMEND");
        model.addAttribute("trade", existing);
        return "trade-form";
    }

    @PostMapping("/save")
    public String saveTrade(@RequestParam String mode,
                            @RequestParam(required = false) String originalId,
                            @RequestParam String subject,
                            @RequestParam String source,
                            @RequestParam String counterparty,
                            @RequestParam Long notional) {
        if ("AMEND".equals(mode)) {
            service.amendTrade(originalId, subject, source, counterparty, notional);
        } else {
            service.bookTrade(subject, source, counterparty, notional);
        }
        return "redirect:/";
    }

    // --- NEW ACTIONS ---

    @PostMapping("/cancel/{id}")
    public String cancelTrade(@PathVariable String id) {
        service.cancelTrade(id);
        return "redirect:/";
    }

    @PostMapping("/verify/{id}")
    public String verifyTrade(@PathVariable String id) {
        service.verifyTrade(id);
        return "redirect:/";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        String csvData = service.generateCsvExport();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trade_blotter.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csvData.getBytes());
    }
}