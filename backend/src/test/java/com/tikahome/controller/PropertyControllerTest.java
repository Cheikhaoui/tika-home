package com.tikahome.controller;

import com.tikahome.config.SecurityConfig;
import com.tikahome.entity.Property;
import com.tikahome.exception.NotFoundException;
import com.tikahome.service.PropertyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PropertyController.class)
@Import(SecurityConfig.class)
class PropertyControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean PropertyService propertyService;

    @Test
    void returnsPublicSummaryWithoutSecrets() throws Exception {
        Property property = new Property("tika", "Tika Home", 6, new BigDecimal("120.00"), new BigDecimal("40.00"), "EUR", 2);
        when(propertyService.get(property.getId())).thenReturn(property);

        mvc.perform(get("/api/properties/" + property.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tika Home"))
                .andExpect(jsonPath("$.maxGuests").value(6))
                .andExpect(jsonPath("$.baseNightlyPrice").value(120.00))
                .andExpect(jsonPath("$.minNights").value(2))
                .andExpect(jsonPath("$.icalExportToken").doesNotExist());
    }

    @Test
    void unknownPropertyIs404() throws Exception {
        UUID id = UUID.randomUUID();
        when(propertyService.get(id)).thenThrow(new NotFoundException("Property " + id + " not found"));

        mvc.perform(get("/api/properties/" + id)).andExpect(status().isNotFound());
    }
}
