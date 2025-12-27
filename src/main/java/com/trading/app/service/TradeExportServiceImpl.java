package com.trading.app.service;

import com.trading.app.dto.PageResult;
import com.trading.app.dto.TradeAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeExportServiceImpl implements TradeExportService {

    private final TradeReadService readService;

    @Override
    public String generateCsvExport() {
        // Fetch ALL items (bypass pagination)
        PageResult<TradeAggregate> result = readService.getTradeDashboard(null, 0, Integer.MAX_VALUE);

        StringBuilder csv = new StringBuilder();
        csv.append("TradeRef,CurrentStatus,Subject,Source,Counterparty,Notional,LastUpdate\n");

        for (TradeAggregate t : result.getData()) {
            csv.append(safe(t.getTradeRef())).append(",")
                    .append(safe(t.getStatus())).append(",")
                    .append(safe(t.getLatestEvent().getSubject())).append(",")
                    .append(safe(t.getLatestEvent().getSourceSystem())).append(",")
                    .append(safe(t.getCounterparty())).append(",")
                    .append(t.getNotional()).append(",")
                    .append(t.getLatestEvent().getEventTime()).append("\n");
        }
        return csv.toString();
    }

    private String safe(String input) {
        return input == null ? "" : input.replace(",", " "); // Basic CSV sanitization
    }
}