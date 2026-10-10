package com.tikahome.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Price of one night; {@code rule} is the price rule name, or null when the base price applies. */
public record NightPrice(LocalDate date, BigDecimal price, String rule) {
}
