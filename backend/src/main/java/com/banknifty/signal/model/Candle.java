package com.banknifty.signal.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Candle {

    private final Instant bucketStart;
    private final Instant bucketEnd;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;

    public Candle(Instant bucketStart, double open, double high, double low, double close, double volume) {
        this.bucketStart = bucketStart.truncatedTo(ChronoUnit.MINUTES);
        this.bucketEnd = this.bucketStart.plus(1, ChronoUnit.MINUTES);
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public Instant getBucketStart() {
        return bucketStart;
    }

    public Instant getBucketEnd() {
        return bucketEnd;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public double getVolume() {
        return volume;
    }

    public double getRange() {
        return high - low;
    }

    public boolean isBullish() {
        return close > open;
    }

    public boolean isBearish() {
        return close < open;
    }

    public double getBodySize() {
        return Math.abs(close - open);
    }
}
