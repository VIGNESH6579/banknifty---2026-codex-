package com.banknifty.signal.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.banknifty.signal.config.MarketProperties;
import com.banknifty.signal.dto.CandleDto;
import com.banknifty.signal.dto.MarketSnapshotDto;
import com.banknifty.signal.dto.SignalDto;
import com.banknifty.signal.model.Candle;

@Service
public class MarketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MarketProperties marketProperties;
    private final AtomicReference<MarketSnapshotDto> snapshotRef;

    public MarketBroadcastService(SimpMessagingTemplate messagingTemplate, MarketProperties marketProperties) {
        this.messagingTemplate = messagingTemplate;
        this.marketProperties = marketProperties;
        this.snapshotRef = new AtomicReference<>(new MarketSnapshotDto(
                marketProperties.getSymbol(),
                0.0,
                Instant.now(),
                null,
                List.of()
        ));
    }

    public void publishPrice(double livePrice, List<Candle> candles, SignalDto latestSignal, Instant lastUpdated) {
        MarketSnapshotDto snapshot = new MarketSnapshotDto(
                marketProperties.getSymbol(),
                livePrice,
                lastUpdated,
                latestSignal,
                candles.stream()
                        .map(candle -> new CandleDto(
                                candle.getBucketStart(),
                                candle.getOpen(),
                                candle.getHigh(),
                                candle.getLow(),
                                candle.getClose(),
                                candle.getVolume()))
                        .toList()
        );
        snapshotRef.set(snapshot);
        messagingTemplate.convertAndSend("/topic/market", snapshot);
    }

    public MarketSnapshotDto getSnapshot() {
        return snapshotRef.get();
    }
}
