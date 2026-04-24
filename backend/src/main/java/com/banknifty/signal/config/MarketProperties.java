package com.banknifty.signal.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.market")
public class MarketProperties {

    private String symbol = "BANKNIFTY";
    private String tradingSymbol = "BANKNIFTY";
    private String token = "26009";
    private int candleCapacity = 50;
    private boolean streamEnabled = true;
    private Duration reconnectDelay = Duration.ofSeconds(5);
    private Duration duplicateSignalCooldown = Duration.ofMinutes(10);
    private double priceScale = 100.0;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTradingSymbol() {
        return tradingSymbol;
    }

    public void setTradingSymbol(String tradingSymbol) {
        this.tradingSymbol = tradingSymbol;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getCandleCapacity() {
        return candleCapacity;
    }

    public void setCandleCapacity(int candleCapacity) {
        this.candleCapacity = candleCapacity;
    }

    public boolean isStreamEnabled() {
        return streamEnabled;
    }

    public void setStreamEnabled(boolean streamEnabled) {
        this.streamEnabled = streamEnabled;
    }

    public Duration getReconnectDelay() {
        return reconnectDelay;
    }

    public void setReconnectDelay(Duration reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    public Duration getDuplicateSignalCooldown() {
        return duplicateSignalCooldown;
    }

    public void setDuplicateSignalCooldown(Duration duplicateSignalCooldown) {
        this.duplicateSignalCooldown = duplicateSignalCooldown;
    }

    public double getPriceScale() {
        return priceScale;
    }

    public void setPriceScale(double priceScale) {
        this.priceScale = priceScale;
    }
}
