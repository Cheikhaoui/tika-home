package com.tikahome.controller;

import com.tikahome.service.IcalExportService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IcalController {

    static final MediaType TEXT_CALENDAR = MediaType.parseMediaType("text/calendar;charset=UTF-8");

    private final IcalExportService exportService;

    public IcalController(IcalExportService exportService) {
        this.exportService = exportService;
    }

    @Operation(summary = "iCal export for Airbnb / Booking.com. The token is secret: share the URL only with the platforms.")
    @GetMapping("/ical/{token}.ics")
    public ResponseEntity<String> export(@PathVariable String token) {
        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .cacheControl(CacheControl.noCache())
                .body(exportService.export(token));
    }
}
