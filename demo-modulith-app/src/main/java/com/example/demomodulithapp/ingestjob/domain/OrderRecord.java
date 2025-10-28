package com.example.demomodulithapp.ingestjob.domain;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class OrderRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String client;
    private final String side;
    private final String ticker;
    private final BigDecimal price;
    private final long quantity;
    private final String sourceSystem;

    public OrderRecord(String client, String side, String ticker, BigDecimal price, long quantity, String sourceSystem) {
        this.client = client;
        this.side = side;
        this.ticker = ticker;
        this.price = price;
        this.quantity = quantity;
        this.sourceSystem = sourceSystem;
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

    public BigDecimal getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    @Override
    public String toString() {
        return "OrderRecord{" +
                "client='" + client + '\'' +
                ", side='" + side + '\'' +
                ", ticker='" + ticker + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", sourceSystem='" + sourceSystem + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderRecord that)) {
            return false;
        }
        return quantity == that.quantity && Objects.equals(client, that.client) && Objects.equals(side, that.side)
                && Objects.equals(ticker, that.ticker) && Objects.equals(price, that.price)
                && Objects.equals(sourceSystem, that.sourceSystem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, side, ticker, price, quantity, sourceSystem);
    }
}
