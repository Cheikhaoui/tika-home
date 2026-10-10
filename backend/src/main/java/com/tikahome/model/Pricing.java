package com.tikahome.model;

import java.math.BigDecimal;
import java.util.List;

public record Pricing(List<NightPrice> nights, BigDecimal subtotal, BigDecimal cleaningFee,
                      BigDecimal total, int minNights) {
}
