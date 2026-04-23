package com.banknifty.signal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banknifty.signal.model.SignalEntity;
import com.banknifty.signal.model.SignalStatus;
import com.banknifty.signal.model.SignalType;

public interface SignalRepository extends JpaRepository<SignalEntity, Long> {

    Optional<SignalEntity> findTopBySymbolOrderByCreatedAtDesc(String symbol);

    Optional<SignalEntity> findTopBySymbolAndTypeOrderByCreatedAtDesc(String symbol, SignalType type);

    List<SignalEntity> findTop20BySymbolOrderByCreatedAtDesc(String symbol);

    List<SignalEntity> findBySymbolAndStatus(String symbol, SignalStatus status);
}
