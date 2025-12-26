package com.trading.app;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<TradeEvent, String> {
    // Fetch ALL events, newest first
    List<TradeEvent> findAllByOrderByEventTimeDesc();
}