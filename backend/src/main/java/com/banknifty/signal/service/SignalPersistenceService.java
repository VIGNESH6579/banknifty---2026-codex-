package com.banknifty.signal.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.banknifty.signal.config.MarketProperties;
import com.banknifty.signal.dto.SignalDto;
import com.banknifty.signal.model.SignalEntity;
import com.banknifty.signal.model.SignalType;
import com.banknifty.signal.model.StrategySignal;
import com.banknifty.signal.repository.SignalRepository;

@Service
public class SignalPersistenceService {

    private final SignalRepository signalRepository;
    private final MarketProperties marketProperties;

    public SignalPersistenceService(SignalRepository signalRepository, MarketProperties marketProperties) {
        this.signalRepository = signalRepository;
        this.marketProperties = marketProperties;
    }

    public Optional<SignalDto> saveIfFresh(StrategySignal strategySignal) {
        Optional<SignalEntity> latestSameType = signalRepository.findTopBySymbolAndTypeOrderByCreatedAtDesc(
                strategySignal.symbol(), strategySignal.type());

        if (latestSameType.isPresent()) {
            SignalEntity entity = latestSameType.get();
            Instant cutoff = Instant.now().minus(marketProperties.getDuplicateSignalCooldown());
            boolean recent = entity.getCreatedAt().isAfter(cutoff);
            boolean nearEntry = Math.abs(entity.getEntryPrice() - strategySignal.entry()) <= 5.0;
            if (recent && nearEntry) {
                return Optional.empty();
            }
        }

        SignalEntity entity = new SignalEntity();
        entity.setSymbol(strategySignal.symbol());
        entity.setType(strategySignal.type());
        entity.setEntryPrice(strategySignal.entry());
        entity.setStopLoss(strategySignal.stopLoss());
        entity.setTarget(strategySignal.target());
        entity.setConfidence(strategySignal.confidence());
        entity.setExpiryTime(strategySignal.expiry());

        return Optional.of(SignalDto.fromEntity(signalRepository.save(entity)));
    }

    public List<SignalDto> getRecentSignals(String symbol) {
        return signalRepository.findTop20BySymbolOrderByCreatedAtDesc(symbol).stream()
                .map(SignalDto::fromEntity)
                .toList();
    }

    public SignalDto getLatestSignal(String symbol) {
        return signalRepository.findTopBySymbolOrderByCreatedAtDesc(symbol)
                .map(SignalDto::fromEntity)
                .orElse(null);
    }
}
