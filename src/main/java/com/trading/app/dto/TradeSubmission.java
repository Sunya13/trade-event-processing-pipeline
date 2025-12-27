package com.trading.app.dto;

import lombok.Data;

/**
 * Captures form data for Booking and Amending trades.
 */
@Data
public class TradeSubmission {
    private String mode; // BOOK or AMEND
    private String originalId; // Only for AMEND

    private String subject;
    private String source;
    private String counterparty;
    private Long notional;
}