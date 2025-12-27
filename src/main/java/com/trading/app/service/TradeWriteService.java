package com.trading.app.service;

import com.trading.app.dto.TradeSubmission;

public interface TradeWriteService {
    void handleSubmission(TradeSubmission submission);
    void cancelTrade(String tradeId);
    void verifyTrade(String tradeId);
}