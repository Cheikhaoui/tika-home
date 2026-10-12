package com.tikahome.service;

import com.tikahome.config.IcalProperties;
import com.tikahome.config.NotificationProperties;
import com.tikahome.dto.admin.ChannelDto;
import com.tikahome.dto.admin.ChannelRequest;
import com.tikahome.dto.admin.ChannelsResponse;
import com.tikahome.dto.admin.SyncNowResponse;
import com.tikahome.entity.Channel;
import com.tikahome.entity.Property;
import com.tikahome.exception.BadRequestException;
import com.tikahome.exception.NotFoundException;
import com.tikahome.mapper.AdminMapper;
import com.tikahome.repository.ChannelRepository;
import com.tikahome.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
public class AdminChannelService {

    private final ChannelRepository channels;
    private final PropertyRepository properties;
    private final ChannelSyncService syncService;
    private final AdminMapper mapper;
    private final TransactionTemplate tx;
    private final IcalProperties icalProperties;
    private final NotificationProperties notificationProperties;

    public AdminChannelService(ChannelRepository channels, PropertyRepository properties,
                               ChannelSyncService syncService, AdminMapper mapper, TransactionTemplate tx,
                               IcalProperties icalProperties, NotificationProperties notificationProperties) {
        this.channels = channels;
        this.properties = properties;
        this.syncService = syncService;
        this.mapper = mapper;
        this.tx = tx;
        this.icalProperties = icalProperties;
        this.notificationProperties = notificationProperties;
    }

    public ChannelsResponse list(UUID propertyId) {
        return tx.execute(s -> {
            Property property = property(propertyId);
            return new ChannelsResponse(exportUrl(property),
                    channels.findByPropertyIdOrderByName(propertyId).stream().map(mapper::toChannelDto).toList());
        });
    }

    /** Creates the channel, then imports it right away so the owner immediately sees if the URL works. */
    public SyncNowResponse create(UUID propertyId, ChannelRequest request) {
        if (request.importUrl() == null || request.importUrl().isBlank()) {
            throw new BadRequestException("The calendar URL is required");
        }
        requireAllowedScheme(request.importUrl());
        UUID id = tx.execute(s -> channels.save(new Channel(property(propertyId), request.type(),
                request.name().strip(), request.importUrl().strip())).getId());
        return syncNow(id);
    }

    public ChannelDto update(UUID channelId, ChannelRequest request) {
        if (request.importUrl() != null && !request.importUrl().isBlank()) {
            requireAllowedScheme(request.importUrl());
        }
        return tx.execute(s -> {
            Channel channel = channel(channelId);
            channel.update(request.name().strip(), request.importUrl(),
                    request.enabled() == null ? channel.isEnabled() : request.enabled());
            return mapper.toChannelDto(channel);
        });
    }

    /** Deleting a channel also deletes its imported blocks (ON DELETE CASCADE): their dates become free. */
    public void delete(UUID channelId) {
        tx.executeWithoutResult(s -> channels.delete(channel(channelId)));
    }

    public SyncNowResponse syncNow(UUID channelId) {
        var result = syncService.sync(channelId); // HTTP outside our transaction; sync manages its own
        return new SyncNowResponse(result, tx.execute(s -> mapper.toChannelDto(channel(channelId))));
    }

    private String exportUrl(Property property) {
        return notificationProperties.siteUrl() + "/ical/" + property.getIcalExportToken() + ".ics";
    }

    private void requireAllowedScheme(String url) {
        boolean https = url.strip().toLowerCase().startsWith("https://");
        if (!https && !icalProperties.allowHttp()) {
            throw new BadRequestException("The calendar URL must start with https://");
        }
    }

    private Property property(UUID id) {
        return properties.findById(id).orElseThrow(() -> new NotFoundException("Property " + id + " not found"));
    }

    private Channel channel(UUID id) {
        return channels.findById(id).orElseThrow(() -> new NotFoundException("Channel " + id + " not found"));
    }
}
