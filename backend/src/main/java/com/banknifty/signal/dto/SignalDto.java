package com.banknifty.signal.dto;

import java.time.Instant;
import java.time.LocalDateTime;

import com.banknifty.signal.model.SignalEntity;
import com.banknifty.signal.model.SignalStatus;
import com.banknifty.signal.model.SignalType;

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
    public static SignalDto fromEntity(SignalEntity entity) {
        return new SignalDto(
                entity.getId(),
                entity.getSymbol(),
                entity.getType(),
                entity.getEntryPrice(),
                entity.getStopLoss(),
                entity.getTarget(),
                entity.getConfidence(),
                entity.getExpiryTime(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
