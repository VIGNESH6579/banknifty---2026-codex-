package com.banknifty.signal.dto;

import java.time.Instant;
import java.time.LocalDateTime;

import com.banknifty.signal.model.SignalStatus;
import com.banknifty.signal.model.SignalType;
import com.banknifty.signal.model.StrategySignal;

public record SignalDto(
        Long id,
        String symbol,
        SignalType type,
        double entry,
        double stopLoss,
        double target,
        double confidence,
        LocalDateTime expiry,
        SignalStatus status,
        Instant timestamp
) {
    public static SignalDto fromStrategySignal(Long id, StrategySignal signal) {
        return new SignalDto(
                id,
                signal.symbol(),
                signal.type(),
                signal.entry(),
                signal.stopLoss(),
                signal.target(),
                signal.confidence(),
                signal.expiry(),
                SignalStatus.OPEN,
                signal.timestamp()
        );
    }
}
