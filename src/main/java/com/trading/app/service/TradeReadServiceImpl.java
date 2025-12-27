package com.trading.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.app.dto.PageResult;
import com.trading.app.dto.TradeAggregate;
import com.trading.app.model.TradeEvent;
import com.trading.app.repository.TradeRepository;
import com.trading.app.service.TradeReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeReadServiceImpl implements TradeReadService {

    private final TradeRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<TradeAggregate> getTradeDashboard(String searchQuery, int page, int size) {
        List<TradeEvent> allEvents = repository.findAllByOrderByEventTimeDesc();

        // Group by Trade Ref
        Map<String, List<TradeEvent>> grouped = allEvents.stream()
                .collect(Collectors.groupingBy(this::extractTradeRef));

        // Create Aggregates
        List<TradeAggregate> allAggregates = grouped.values().stream()
                .map(this::buildAggregate)
                .sorted(Comparator.comparing((TradeAggregate t) -> t.getLatestEvent().getEventTime()).reversed())
                .collect(Collectors.toList());

        // Filter
        if (searchQuery != null && !searchQuery.isBlank()) {
            String q = searchQuery.toLowerCase();
            allAggregates = allAggregates.stream()
                    .filter(agg ->
                            agg.getTradeRef().toLowerCase().contains(q) ||
                                    agg.getCounterparty().toLowerCase().contains(q) ||
                                    agg.getLatestEvent().getSubject().toLowerCase().contains(q) ||
                                    agg.getStatus().toLowerCase().contains(q)
                    )
                    .collect(Collectors.toList());
        }

        // Pagination
        int totalItems = allAggregates.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int start = Math.min(page * size, totalItems);
        int end = Math.min(start + size, totalItems);

        return new PageResult<>(allAggregates.subList(start, end), page, totalPages, totalItems);
    }

    @Override
    public TradeEvent getTradeById(String eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Trade not found: " + eventId));
    }

    // --- Helpers ---

    private TradeAggregate buildAggregate(List<TradeEvent> events) {
        TradeAggregate agg = new TradeAggregate();
        agg.setHistory(events);

        // Assumes list is sorted DESC by time, so index 0 is latest
        TradeEvent latest = events.get(0);
        agg.setLatestEvent(latest);
        agg.setTradeRef(extractTradeRef(latest));

        // Derive Status
        switch (latest.getEventType()) {
            case "TRADE_CANCELLED":
                agg.setStatus("CANCELLED");
                agg.setModifiable(false);
                break;
            case "TRADE_VERIFIED":
                agg.setStatus("VERIFIED");
                agg.setModifiable(true);
                break;
            default:
                agg.setStatus("LIVE");
                agg.setModifiable(true);
        }

        // Extract Business Data
        try {
            JsonNode node = objectMapper.readTree(latest.getData());
            agg.setCounterparty(node.has("counterparty") ? node.get("counterparty").asText() : "UNKNOWN");
            agg.setNotional(node.has("notional_amount") ? node.get("notional_amount").asDouble() : 0.0);
        } catch (Exception e) {
            agg.setCounterparty("ERROR");
            agg.setNotional(0.0);
        }

        return agg;
    }

    private String extractTradeRef(TradeEvent event) {
        try {
            JsonNode node = objectMapper.readTree(event.getData());
            if (node.has("trade_ref")) return node.get("trade_ref").asText();
        } catch (Exception e) {}
        return event.getEventId().split(":")[0];
    }
}