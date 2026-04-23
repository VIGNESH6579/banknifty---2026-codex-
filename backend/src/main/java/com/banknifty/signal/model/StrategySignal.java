package com.banknifty.signal.model;

import java.time.Instant;
import java.time.LocalDateTime;

public record StrategySignal(
        String symbol,
        SignalType type,
        double entry,
        double stopLoss,
        double target,
        double confidence,
        LocalDateTime expiry,
        Instant timestamp
) {
}
