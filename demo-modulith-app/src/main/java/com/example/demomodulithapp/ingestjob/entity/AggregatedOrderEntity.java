package com.example.demomodulithapp.ingestjob.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "aggregated_orders", indexes = {
        @Index(name = "ix_aggr_date", columnList = "data_date")
})
public class AggregatedOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_date", nullable = false)
    private LocalDate dataDate;

    @Column(nullable = false, length = 64)
    private String client;

    @Column(nullable = false, length = 4)
    private String side;

    @Column(nullable = false, length = 32)
    private String ticker;

    @Column(name = "total_quantity", nullable = false)
    private long totalQuantity;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal vwap;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public AggregatedOrderEntity() {
    }

    public AggregatedOrderEntity(LocalDate dataDate, String client, String side, String ticker, long totalQuantity, BigDecimal vwap) {
        this.dataDate = dataDate;
        this.client = client;
        this.side = side;
        this.ticker = ticker;
        this.totalQuantity = totalQuantity;
        this.vwap = vwap;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDataDate(LocalDate dataDate) {
        this.dataDate = dataDate;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public void setTotalQuantity(long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public void setVwap(BigDecimal vwap) {
        this.vwap = vwap;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
