package com.trading.app;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "trading_pipeline_tracker")
@Getter
@Setter
public class TradeEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type")
    private String eventType;

    private String subject;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "trading_date")
    private LocalDate tradingDate;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String data;

    public TradeEvent() {}

    public TradeEvent(String eventId, String eventType, String subject, String sourceSystem, LocalDate tradingDate, LocalDateTime eventTime, String data) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.subject = subject;
        this.sourceSystem = sourceSystem;
        this.tradingDate = tradingDate;
        this.eventTime = eventTime;
        this.data = data;
    }
}
