CREATE TABLE IF NOT EXISTS aggregated_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  data_date DATE NOT NULL,
  client VARCHAR(64) NOT NULL,
  side VARCHAR(4) NOT NULL,
  ticker VARCHAR(32) NOT NULL,
  total_quantity BIGINT NOT NULL,
  vwap DECIMAL(20,8) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_aggr_key UNIQUE (data_date, client, side, ticker)
);
CREATE INDEX IF NOT EXISTS ix_aggr_date ON aggregated_orders(data_date);
