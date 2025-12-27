package com.trading.app;

import com.trading.app.model.TradeEvent;
import com.trading.app.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class TradePipelineIntegrationTest {

    // Spin up a real Postgres DB in Docker
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_trading_db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TradeRepository tradeRepository;

    // Connect Spring Boot to the Testcontainer DB
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update"); // Create schema
    }

    @BeforeEach
    void setup() {
        tradeRepository.deleteAll(); // Clean DB before each test
    }

    @Test
    void testFullTradeLifecycle() throws Exception {
        // ==========================================
        // 1. BOOK A NEW TRADE
        // ==========================================
        mockMvc.perform(post("/save")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("mode", "BOOK")
                        .param("subject", "VANILLA_SWAPTION")
                        .param("source", "INTERNAL_UI")
                        .param("counterparty", "GOLDMAN_SACHS")
                        .param("notional", "1000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Verify DB State
        List<TradeEvent> events = tradeRepository.findAllByOrderByEventTimeDesc();
        assertThat(events).hasSize(1);
        TradeEvent bookedEvent = events.get(0);
        assertThat(bookedEvent.getEventType()).isEqualTo("TRADE_BOOKED");

        String eventId = bookedEvent.getEventId(); // e.g., VANILLA...:BOOK

        // ==========================================
        // 2. VIEW DASHBOARD
        // ==========================================
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("GOLDMAN_SACHS")))
                .andExpect(content().string(containsString("1,000,000")));

        // ==========================================
        // 3. AMEND THE TRADE
        // ==========================================
        mockMvc.perform(post("/save")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("mode", "AMEND")
                        .param("originalId", eventId) // Link to original
                        .param("subject", "VANILLA_SWAPTION")
                        .param("source", "INTERNAL_UI")
                        .param("counterparty", "GOLDMAN_SACHS_NEW") // Changed CP
                        .param("notional", "2000000"))              // Changed Amt
                .andExpect(status().is3xxRedirection());

        // Verify Amendment in DB
        events = tradeRepository.findAllByOrderByEventTimeDesc();
        assertThat(events).hasSize(2); // Book + Amend
        assertThat(events.get(0).getEventType()).isEqualTo("TRADE_AMENDED");
        assertThat(events.get(0).getData()).contains("GOLDMAN_SACHS_NEW");

        // ==========================================
        // 4. VERIFY THE TRADE
        // ==========================================
        // We must verify the *latest* event ID
        String amendEventId = events.get(0).getEventId();

        mockMvc.perform(post("/verify/" + amendEventId))
                .andExpect(status().is3xxRedirection());

        // Check Dashboard for "VERIFIED" badge
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("VERIFIED")));

        // ==========================================
        // 5. CANCEL THE TRADE
        // ==========================================
        // Verify generates a new event, so get the newest ID
        String verifyEventId = tradeRepository.findAllByOrderByEventTimeDesc().get(0).getEventId();

        mockMvc.perform(post("/cancel/" + verifyEventId))
                .andExpect(status().is3xxRedirection());

        // Check Dashboard for "CANCELLED" status
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("CANCELLED")));
    }

    @Test
    void testCsvExport() throws Exception {
        // Seed data
        testFullTradeLifecycle();

        MvcResult result = mockMvc.perform(get("/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=trade_blotter.csv"))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andReturn();

        String csvContent = result.getResponse().getContentAsString();
        assertThat(csvContent).contains("TradeRef,CurrentStatus,Subject"); // Header
        assertThat(csvContent).contains("GOLDMAN_SACHS_NEW"); // Data
    }
}