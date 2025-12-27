package com.trading.app.dto;

import com.trading.app.model.TradeEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor  // <--- This fixes the error by allowing "new TradeAggregate()"
@AllArgsConstructor
public class TradeAggregate {
    private String tradeRef;
    private TradeEvent latestEvent;
    private List<TradeEvent> history;

    // UI Helper Fields
    private String status;        // LIVE, VERIFIED, CANCELLED
    private String counterparty;
    private Double notional;
    private boolean isModifiable;
}