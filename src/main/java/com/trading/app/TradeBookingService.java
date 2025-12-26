package com.trading.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TradeBookingService {

    private final TradeRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TradeBookingService(TradeRepository repository) {
        this.repository = repository;
    }

    /**
     * Main Dashboard Data Fetcher
     * Now supports textual search/filtering
     */
    public List<TradeAggregate> getTradeDashboard(String searchQuery) {
        // 1. Fetch & Group
        List<TradeEvent> allEvents = repository.findAllByOrderByEventTimeDesc();
        Map<String, List<TradeEvent>> grouped = allEvents.stream()
                .collect(Collectors.groupingBy(this::extractTradeRef));

        // 2. Convert to Aggregates
        List<TradeAggregate> aggregates = grouped.values().stream()
                .map(events -> new TradeAggregate(extractTradeRef(events.get(0)), events))
                .sorted(Comparator.comparing((TradeAggregate t) -> t.getLatestEvent().getEventTime()).reversed())
                .collect(Collectors.toList());

        // 3. Apply Search Filter (if exists)
        if (searchQuery != null && !searchQuery.isBlank()) {
            String q = searchQuery.toLowerCase();
            return aggregates.stream()
                    .filter(agg ->
                            agg.getTradeRef().toLowerCase().contains(q) ||
                                    agg.getCounterparty().toLowerCase().contains(q) ||
                                    agg.getLatestEvent().getSubject().toLowerCase().contains(q) ||
                                    agg.getStatus().toLowerCase().contains(q)
                    )
                    .collect(Collectors.toList());
        }

        return aggregates;
    }

    public void bookTrade(String subject, String source, String counterparty, Long notional) {
        String baseId = subject + ":" + source + ":" + UUID.randomUUID().toString().substring(0, 8);
        saveEvent(baseId + ":BOOK", "TRADE_BOOKED", subject, source, counterparty, notional, baseId);
    }

    public void amendTrade(String originalEventId, String subject, String source, String counterparty, Long notional) {
        String tradeRef = extractRefFromId(originalEventId);
        String newEventId = tradeRef + ":AMEND:" + UUID.randomUUID().toString().substring(0,4);
        saveEvent(newEventId, "TRADE_AMENDED", subject, source, counterparty, notional, tradeRef);
    }

    // --- NEW LIFECYCLE METHODS ---

    public void cancelTrade(String tradeId) {
        TradeEvent latest = getTradeById(tradeId);
        String tradeRef = extractTradeRef(latest);

        // Copy existing data but mark status as Cancelled in JSON too
        saveEvent(tradeRef + ":CANCEL:" + UUID.randomUUID().toString().substring(0,4),
                "TRADE_CANCELLED",
                latest.getSubject(),
                latest.getSourceSystem(),
                extractJsonField(latest.getData(), "counterparty"),
                extractJsonLong(latest.getData(), "notional_amount"),
                tradeRef);
    }

    public void verifyTrade(String tradeId) {
        TradeEvent latest = getTradeById(tradeId);
        String tradeRef = extractTradeRef(latest);

        saveEvent(tradeRef + ":VERIFY:" + UUID.randomUUID().toString().substring(0,4),
                "TRADE_VERIFIED",
                latest.getSubject(),
                latest.getSourceSystem(),
                extractJsonField(latest.getData(), "counterparty"),
                extractJsonLong(latest.getData(), "notional_amount"),
                tradeRef);
    }

    // --- CSV EXPORT ---
    public String generateCsvExport() {
        List<TradeAggregate> trades = getTradeDashboard(null);
        StringBuilder csv = new StringBuilder();
        csv.append("TradeRef,CurrentStatus,Subject,Source,Counterparty,Notional,LastUpdate\n");

        for (TradeAggregate t : trades) {
            csv.append(t.getTradeRef()).append(",")
                    .append(t.getStatus()).append(",")
                    .append(t.getLatestEvent().getSubject()).append(",")
                    .append(t.getLatestEvent().getSourceSystem()).append(",")
                    .append(t.getCounterparty()).append(",")
                    .append(t.getNotional()).append(",")
                    .append(t.getLatestEvent().getEventTime()).append("\n");
        }
        return csv.toString();
    }

    // --- HELPERS ---

    public TradeEvent getTradeById(String eventId) {
        return repository.findById(eventId).orElseThrow(() -> new RuntimeException("Trade not found"));
    }

    private void saveEvent(String eventId, String type, String subject, String source, String cp, Long notional, String tradeRef) {
        String jsonPayload = String.format(
                "{\"trade_ref\": \"%s\", \"counterparty\": \"%s\", \"notional_amount\": %d, \"currency\": \"USD\"}",
                tradeRef, cp, notional
        );
        TradeEvent event = new TradeEvent(eventId, type, subject, source, LocalDate.now(), LocalDateTime.now(), jsonPayload);
        repository.save(event);
    }

    private String extractTradeRef(TradeEvent event) {
        try {
            JsonNode node = objectMapper.readTree(event.getData());
            if (node.has("trade_ref")) return node.get("trade_ref").asText();
        } catch (Exception e) {}
        return event.getEventId().split(":")[0];
    }

    private String extractRefFromId(String id) {
        String[] parts = id.split(":");
        // Basic heuristic to reconstruct Ref from ID if strictly formatted
        if (parts.length >= 3) return parts[0] + ":" + parts[1] + ":" + parts[2];
        return id;
    }

    private String extractJsonField(String json, String field) {
        try { return objectMapper.readTree(json).get(field).asText(); } catch (Exception e) { return ""; }
    }
    private Long extractJsonLong(String json, String field) {
        try { return objectMapper.readTree(json).get(field).asLong(); } catch (Exception e) { return 0L; }
    }
}