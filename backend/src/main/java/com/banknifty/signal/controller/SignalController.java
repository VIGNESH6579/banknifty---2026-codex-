package com.banknifty.signal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banknifty.signal.config.MarketProperties;
import com.banknifty.signal.dto.MarketSnapshotDto;
import com.banknifty.signal.dto.SignalDto;
import com.banknifty.signal.service.MarketBroadcastService;
import com.banknifty.signal.service.SignalPersistenceService;

@RestController
@RequestMapping("/api")
public class SignalController {

    private final SignalPersistenceService signalPersistenceService;
    private final MarketBroadcastService marketBroadcastService;
    private final MarketProperties marketProperties;

    public SignalController(
            SignalPersistenceService signalPersistenceService,
            MarketBroadcastService marketBroadcastService,
            MarketProperties marketProperties) {
        this.signalPersistenceService = signalPersistenceService;
        this.marketBroadcastService = marketBroadcastService;
        this.marketProperties = marketProperties;
    }

    @GetMapping("/signals")
    public List<SignalDto> recentSignals() {
        return signalPersistenceService.getRecentSignals(marketProperties.getSymbol());
    }

    @GetMapping("/market/snapshot")
    public MarketSnapshotDto snapshot() {
        return marketBroadcastService.getSnapshot();
    }
}
