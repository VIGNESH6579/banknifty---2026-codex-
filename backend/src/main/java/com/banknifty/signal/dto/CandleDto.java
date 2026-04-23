package com.banknifty.signal.dto;

import java.time.Instant;

public record CandleDto(
        Instant startTime,
        double open,
        double high,
        double low,
        double close,
        double volume
) {
}
