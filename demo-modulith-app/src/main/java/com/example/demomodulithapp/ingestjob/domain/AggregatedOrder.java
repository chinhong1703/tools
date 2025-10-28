package com.example.demomodulithapp.ingestjob.domain;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class AggregatedOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final LocalDate dataDate;
    private final String client;
    private final String side;
    private final String ticker;
    private final long totalQuantity;
    private final BigDecimal vwap;

    public AggregatedOrder(LocalDate dataDate, String client, String side, String ticker, long totalQuantity, BigDecimal vwap) {
        this.dataDate = dataDate;
        this.client = client;
        this.side = side;
        this.ticker = ticker;
        this.totalQuantity = totalQuantity;
        this.vwap = vwap;
    }

    public LocalDate getDataDate() {
        return dataDate;
    }

    public String getClient() {
        return client;
    }

    public String getSide() {
        return side;
    }

    public String getTicker() {
        return ticker;
    }

    public long getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getVwap() {
        return vwap;
    }

    @Override
    public String toString() {
        return "AggregatedOrder{" +
                "dataDate=" + dataDate +
                ", client='" + client + '\'' +
                ", side='" + side + '\'' +
                ", ticker='" + ticker + '\'' +
                ", totalQuantity=" + totalQuantity +
                ", vwap=" + vwap +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AggregatedOrder that)) {
            return false;
        }
        return totalQuantity == that.totalQuantity && Objects.equals(dataDate, that.dataDate)
                && Objects.equals(client, that.client) && Objects.equals(side, that.side)
                && Objects.equals(ticker, that.ticker) && Objects.equals(vwap, that.vwap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataDate, client, side, ticker, totalQuantity, vwap);
    }
}
