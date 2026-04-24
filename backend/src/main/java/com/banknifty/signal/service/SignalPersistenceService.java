package com.banknifty.signal.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.banknifty.signal.config.MarketProperties;
import com.banknifty.signal.dto.SignalDto;
import com.banknifty.signal.model.SignalType;
import com.banknifty.signal.model.StrategySignal;

@Service
public class SignalPersistenceService {

    private final MarketProperties marketProperties;
    private final AtomicLong ids = new AtomicLong(0);
    private final Deque<SignalDto> signals = new ArrayDeque<>();

    public SignalPersistenceService(MarketProperties marketProperties) {
        this.marketProperties = marketProperties;
    }

    public synchronized Optional<SignalDto> saveIfFresh(StrategySignal strategySignal) {
        Optional<SignalDto> latestSameType = signals.stream()
                .filter(signal -> signal.symbol().equals(strategySignal.symbol()))
                .filter(signal -> signal.type() == strategySignal.type())
                .findFirst();

        if (latestSameType.isPresent()) {
            SignalDto signal = latestSameType.get();
            Instant cutoff = Instant.now().minus(marketProperties.getDuplicateSignalCooldown());
            boolean recent = signal.timestamp().isAfter(cutoff);
            boolean nearEntry = Math.abs(signal.entry() - strategySignal.entry()) <= 5.0;
            if (recent && nearEntry) {
                return Optional.empty();
            }
        }

        SignalDto signal = SignalDto.fromStrategySignal(ids.incrementAndGet(), strategySignal);
        signals.addFirst(signal);
        while (signals.size() > 20) {
            signals.removeLast();
        }

        return Optional.of(signal);
    }

    public synchronized List<SignalDto> getRecentSignals(String symbol) {
        return signals.stream()
                .filter(signal -> signal.symbol().equals(symbol))
                .toList();
    }

    public synchronized SignalDto getLatestSignal(String symbol) {
        return signals.stream()
                .filter(signal -> signal.symbol().equals(symbol))
                .findFirst()
                .orElse(null);
    }
}
