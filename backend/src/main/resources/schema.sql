CREATE TABLE IF NOT EXISTS signals (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(24) NOT NULL,
    type VARCHAR(8) NOT NULL,
    entry_price DOUBLE PRECISION NOT NULL,
    stop_loss DOUBLE PRECISION NOT NULL,
    target DOUBLE PRECISION NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    status VARCHAR(8) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_signals_symbol_created_at ON signals(symbol, created_at DESC);
