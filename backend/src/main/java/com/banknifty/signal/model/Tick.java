package com.banknifty.signal.model;

import java.time.Instant;

public record Tick(String symbol, double price, double volume, Instant timestamp) {
}
