package com.tikahome.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A seasonal price for nights in [startDate, endDate). */
@Entity
@Table(name = "price_rule")
public class PriceRule {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id")
    private Property property;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "nightly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal nightlyPrice;

    /** Null means "use the property's default minimum". */
    @Column(name = "min_nights")
    private Integer minNights;

    @Column(nullable = false)
    private int priority;

    protected PriceRule() {
        // for JPA
    }

    public PriceRule(Property property, String name, LocalDate startDate, LocalDate endDate,
                     BigDecimal nightlyPrice, Integer minNights, int priority) {
        this.id = UUID.randomUUID();
        this.property = property;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.nightlyPrice = nightlyPrice;
        this.minNights = minNights;
        this.priority = priority;
    }

    public void update(String name, LocalDate startDate, LocalDate endDate, BigDecimal nightlyPrice,
                       Integer minNights, int priority) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.nightlyPrice = nightlyPrice;
        this.minNights = minNights;
        this.priority = priority;
    }

    public boolean covers(LocalDate night) {
        return !night.isBefore(startDate) && night.isBefore(endDate);
    }

    public UUID getId() { return id; }
    public Property getProperty() { return property; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getNightlyPrice() { return nightlyPrice; }
    public Integer getMinNights() { return minNights; }
    public int getPriority() { return priority; }
}
