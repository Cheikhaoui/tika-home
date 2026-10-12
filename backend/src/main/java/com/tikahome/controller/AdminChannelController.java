package com.tikahome.controller;

import com.tikahome.dto.admin.ChannelDto;
import com.tikahome.dto.admin.ChannelRequest;
import com.tikahome.dto.admin.ChannelsResponse;
import com.tikahome.dto.admin.SyncNowResponse;
import com.tikahome.service.AdminChannelService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminChannelController {

    private final AdminChannelService service;

    public AdminChannelController(AdminChannelService service) {
        this.service = service;
    }

    @Operation(summary = "Channels with sync status, and our export URL to paste into the platforms")
    @GetMapping("/properties/{propertyId}/channels")
    public ChannelsResponse list(@PathVariable UUID propertyId) {
        return service.list(propertyId);
    }

    @Operation(summary = "Add an Airbnb / Booking.com calendar URL; imported immediately")
    @PostMapping("/properties/{propertyId}/channels")
    @ResponseStatus(HttpStatus.CREATED)
    public SyncNowResponse create(@PathVariable UUID propertyId, @Valid @RequestBody ChannelRequest request) {
        return service.create(propertyId, request);
    }

    @PutMapping("/channels/{channelId}")
    public ChannelDto update(@PathVariable UUID channelId, @Valid @RequestBody ChannelRequest request) {
        return service.update(channelId, request);
    }

    @Operation(summary = "Remove a channel and its imported blocks")
    @DeleteMapping("/channels/{channelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID channelId) {
        service.delete(channelId);
    }

    @PostMapping("/channels/{channelId}/sync")
    public SyncNowResponse syncNow(@PathVariable UUID channelId) {
        return service.syncNow(channelId);
    }
}
