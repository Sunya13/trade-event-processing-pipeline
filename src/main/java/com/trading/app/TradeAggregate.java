package com.trading.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import java.util.List;

@Data
public class TradeAggregate {
    private String tradeRef;
    private TradeEvent latestEvent;
    private List<TradeEvent> history;

    // UI Helper Fields (extracted from JSON)
    private String status;        // LIVE, VERIFIED, CANCELLED
    private String counterparty;
    private Double notional;
    private boolean isModifiable; // Can we amend/cancel this?

    private static final ObjectMapper mapper = new ObjectMapper();

    public TradeAggregate(String tradeRef, List<TradeEvent> history) {
        this.tradeRef = tradeRef;
        this.history = history;
        this.latestEvent = history.get(0); // Assumes history is sorted desc

        deriveState();
    }

    private void deriveState() {
        // 1. Determine Status based on Event Type
        switch (latestEvent.getEventType()) {
            case "TRADE_CANCELLED":
                this.status = "CANCELLED";
                this.isModifiable = false; // Dead trade
                break;
            case "TRADE_VERIFIED":
                this.status = "VERIFIED";
                this.isModifiable = true; // Can still be amended (un-verifying it)
                break;
            default:
                this.status = "LIVE";
                this.isModifiable = true;
        }

        // 2. Extract Business Data from JSON
        try {
            JsonNode node = mapper.readTree(latestEvent.getData());
            if (node.has("counterparty")) {
                this.counterparty = node.get("counterparty").asText();
            }
            if (node.has("notional_amount")) {
                this.notional = node.get("notional_amount").asDouble();
            }
        } catch (Exception e) {
            this.counterparty = "UNKNOWN";
            this.notional = 0.0;
        }
    }
}