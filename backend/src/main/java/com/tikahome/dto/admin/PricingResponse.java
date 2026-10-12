package com.tikahome.dto.admin;

import java.util.List;

public record PricingResponse(String currency, PropertySettings settings, List<PriceRuleDto> rules) {
}
