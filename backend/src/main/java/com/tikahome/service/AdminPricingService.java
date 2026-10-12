package com.tikahome.service;

import com.tikahome.dto.admin.PriceRuleDto;
import com.tikahome.dto.admin.PricingResponse;
import com.tikahome.dto.admin.PropertySettings;
import com.tikahome.entity.PriceRule;
import com.tikahome.entity.Property;
import com.tikahome.exception.NotFoundException;
import com.tikahome.mapper.AdminMapper;
import com.tikahome.model.DateRange;
import com.tikahome.repository.PriceRuleRepository;
import com.tikahome.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminPricingService {

    private final PropertyRepository properties;
    private final PriceRuleRepository rules;
    private final AdminMapper mapper;

    public AdminPricingService(PropertyRepository properties, PriceRuleRepository rules, AdminMapper mapper) {
        this.properties = properties;
        this.rules = rules;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PricingResponse get(UUID propertyId) {
        Property property = property(propertyId);
        return new PricingResponse(property.getCurrency(), mapper.toSettings(property),
                mapper.toRuleDtos(rules.findByPropertyIdOrderByStartDate(propertyId)));
    }

    @Transactional
    public PricingResponse updateSettings(UUID propertyId, PropertySettings settings) {
        property(propertyId).updateSettings(settings.baseNightlyPrice(), settings.cleaningFee(),
                settings.minNights(), settings.maxGuests());
        return get(propertyId);
    }

    @Transactional
    public PriceRuleDto createRule(UUID propertyId, PriceRuleDto dto) {
        DateRange range = new DateRange(dto.startDate(), dto.endDate()); // 400 if end <= start
        PriceRule rule = new PriceRule(property(propertyId), dto.name().strip(), range.start(), range.end(),
                dto.nightlyPrice(), dto.minNights(), dto.priority());
        return mapper.toRuleDto(rules.save(rule));
    }

    @Transactional
    public PriceRuleDto updateRule(UUID ruleId, PriceRuleDto dto) {
        DateRange range = new DateRange(dto.startDate(), dto.endDate());
        PriceRule rule = rule(ruleId);
        rule.update(dto.name().strip(), range.start(), range.end(), dto.nightlyPrice(), dto.minNights(), dto.priority());
        return mapper.toRuleDto(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        rules.delete(rule(ruleId));
    }

    private Property property(UUID id) {
        return properties.findById(id).orElseThrow(() -> new NotFoundException("Property " + id + " not found"));
    }

    private PriceRule rule(UUID id) {
        return rules.findById(id).orElseThrow(() -> new NotFoundException("Price rule " + id + " not found"));
    }
}
