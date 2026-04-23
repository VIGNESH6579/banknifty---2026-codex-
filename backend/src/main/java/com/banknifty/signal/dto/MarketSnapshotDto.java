package com.banknifty.signal.dto;

import java.time.Instant;
import java.util.List;

public record MarketSnapshotDto(
        String symbol,
        double livePrice,
        Instant lastUpdated,
        SignalDto latestSignal,
        List<CandleDto> candles
) {
}
