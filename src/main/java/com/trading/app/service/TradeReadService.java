package com.trading.app.service;

import com.trading.app.dto.PageResult;
import com.trading.app.dto.TradeAggregate;
import com.trading.app.model.TradeEvent;

public interface TradeReadService {
    PageResult<TradeAggregate> getTradeDashboard(String searchQuery, int page, int size);
    TradeEvent getTradeById(String eventId);
}