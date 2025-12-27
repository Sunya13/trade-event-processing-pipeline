package com.trading.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trading.app.dto.TradeSubmission;
import com.trading.app.model.TradeEvent;
import com.trading.app.repository.TradeRepository;
import com.trading.app.service.TradeReadService;
import com.trading.app.service.TradeWriteService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeWriteServiceImpl implements TradeWriteService {

    private final TradeRepository repository;
    private final TradeReadService readService; // For looking up existing trades
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void handleSubmission(TradeSubmission submission) {
        if ("AMEND".equals(submission.getMode())) {
            amendTrade(submission);
        } else {
            bookNewTrade(submission);
        }
    }

    @Override
    @Transactional
    public void cancelTrade(String tradeId) {
        TradeEvent latest = readService.getTradeById(tradeId);
        String tradeRef = extractTradeRef(latest);

        saveEvent(tradeRef + ":CANCEL:" + UUID.randomUUID().toString().substring(0, 4),
                "TRADE_CANCELLED",
                latest.getSubject(),
                latest.getSourceSystem(),
                copyDataWithStatus(latest.getData(), "CANCELLED", tradeRef));
    }

    @Override
    @Transactional
    public void verifyTrade(String tradeId) {
        TradeEvent latest = readService.getTradeById(tradeId);
        String tradeRef = extractTradeRef(latest);

        saveEvent(tradeRef + ":VERIFY:" + UUID.randomUUID().toString().substring(0, 4),
                "TRADE_VERIFIED",
                latest.getSubject(),
                latest.getSourceSystem(),
                copyDataWithStatus(latest.getData(), "VERIFIED", tradeRef));
    }

    // --- Private Business Logic ---

    private void bookNewTrade(TradeSubmission sub) {
        String tradeRef = sub.getSubject() + ":" + sub.getSource() + ":" + UUID.randomUUID().toString().substring(0, 8);
        String eventId = tradeRef + ":BOOK";
        String jsonData = createPayload(tradeRef, sub.getCounterparty(), sub.getNotional(), "LIVE");

        saveEvent(eventId, "TRADE_BOOKED", sub.getSubject(), sub.getSource(), jsonData);
    }

    private void amendTrade(TradeSubmission sub) {
        // Recover tradeRef from the original event ID
        String tradeRef = extractRefFromId(sub.getOriginalId());
        String eventId = tradeRef + ":AMEND:" + UUID.randomUUID().toString().substring(0, 4);
        String jsonData = createPayload(tradeRef, sub.getCounterparty(), sub.getNotional(), "LIVE");

        saveEvent(eventId, "TRADE_AMENDED", sub.getSubject(), sub.getSource(), jsonData);
    }

    private void saveEvent(String eventId, String type, String subject, String source, String jsonData) {
        TradeEvent event = new TradeEvent(
                eventId, type, subject, source, LocalDate.now(), LocalDateTime.now(), jsonData
        );
        repository.save(event);
    }

    @SneakyThrows
    private String createPayload(String tradeRef, String counterparty, Long notional, String status) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("trade_ref", tradeRef);
        node.put("counterparty", counterparty);
        node.put("notional_amount", notional);
        node.put("currency", "USD");
        node.put("status", status);
        return objectMapper.writeValueAsString(node);
    }

    @SneakyThrows
    private String copyDataWithStatus(String originalJson, String newStatus, String tradeRef) {
        JsonNode node = objectMapper.readTree(originalJson);
        if (node instanceof ObjectNode) {
            ((ObjectNode) node).put("status", newStatus);
            ((ObjectNode) node).put("trade_ref", tradeRef); // Ensure ref persists
            return objectMapper.writeValueAsString(node);
        }
        return originalJson;
    }

    @SneakyThrows
    private String extractTradeRef(TradeEvent event) {
        JsonNode node = objectMapper.readTree(event.getData());
        return node.has("trade_ref") ? node.get("trade_ref").asText() : extractRefFromId(event.getEventId());
    }

    private String extractRefFromId(String id) {
        String[] parts = id.split(":");
        if (parts.length >= 3) return parts[0] + ":" + parts[1] + ":" + parts[2];
        return id;
    }
}