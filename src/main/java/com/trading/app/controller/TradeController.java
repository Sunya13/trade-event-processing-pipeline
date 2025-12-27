package com.trading.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.app.dto.PageResult;
import com.trading.app.dto.TradeAggregate;
import com.trading.app.dto.TradeSubmission;
import com.trading.app.model.TradeEvent;
import com.trading.app.service.TradeExportService;
import com.trading.app.service.TradeReadService;
import com.trading.app.service.TradeWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class TradeController {

    private final TradeReadService readService;
    private final TradeWriteService writeService;
    private final TradeExportService exportService;
    private final ObjectMapper mapper;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String search,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model) {

        PageResult<TradeAggregate> result = readService.getTradeDashboard(search, page, size);

        model.addAttribute("tradeAggregates", result.getData());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("searchQuery", search);

        return "dashboard";
    }

    @GetMapping("/book")
    public String showBookForm(Model model) {
        TradeSubmission form = new TradeSubmission();
        form.setMode("BOOK");
        model.addAttribute("tradeSubmission", form); // Use DTO
        return "trade-form";
    }

    @GetMapping("/amend/{id}")
    public String showAmendForm(@PathVariable String id, Model model) {
        TradeEvent existing = readService.getTradeById(id);

        // Map existing entity to Form DTO
        TradeSubmission form = new TradeSubmission();
        form.setMode("AMEND");
        form.setOriginalId(id);
        form.setSubject(existing.getSubject());
        form.setSource(existing.getSourceSystem());

        try {
            JsonNode node = mapper.readTree(existing.getData());
            if(node.has("counterparty")) form.setCounterparty(node.get("counterparty").asText());
            if(node.has("notional_amount")) form.setNotional(node.get("notional_amount").asLong());
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("tradeSubmission", form);
        return "trade-form";
    }

    @PostMapping("/save")
    public String saveTrade(@ModelAttribute TradeSubmission submission, RedirectAttributes redirectAttributes) {
        writeService.handleSubmission(submission);

        String msg = "AMEND".equals(submission.getMode()) ? "Trade amended successfully!" : "New trade booked successfully!";
        redirectAttributes.addFlashAttribute("successMessage", msg);

        return "redirect:/";
    }

    @PostMapping("/cancel/{id}")
    public String cancelTrade(@PathVariable String id, RedirectAttributes redirectAttributes) {
        writeService.cancelTrade(id);
        redirectAttributes.addFlashAttribute("successMessage", "Trade cancelled.");
        return "redirect:/";
    }

    @PostMapping("/verify/{id}")
    public String verifyTrade(@PathVariable String id, RedirectAttributes redirectAttributes) {
        writeService.verifyTrade(id);
        redirectAttributes.addFlashAttribute("successMessage", "Trade verified.");
        return "redirect:/";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        String csvData = exportService.generateCsvExport();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trade_blotter.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csvData.getBytes());
    }
}