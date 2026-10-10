package com.tikahome.service;

import com.tikahome.entity.Channel;
import com.tikahome.entity.ChannelType;
import com.tikahome.entity.Property;
import com.tikahome.model.SyncResult;
import com.tikahome.repository.ChannelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelSyncJobTest {

    @Mock ChannelRepository channels;
    @Mock ChannelSyncService syncService;
    @InjectMocks ChannelSyncJob job;

    final Property property = new Property("p", "P", 4, BigDecimal.TEN, BigDecimal.ZERO, "EUR", 1);

    @Test
    void oneFailingChannelDoesNotStopTheOthers() {
        Channel broken = new Channel(property, ChannelType.AIRBNB, "Airbnb", "https://a.example/cal.ics");
        Channel healthy = new Channel(property, ChannelType.BOOKING_COM, "Booking.com", "https://b.example/cal.ics");
        when(channels.findByEnabledTrue()).thenReturn(List.of(broken, healthy));
        when(syncService.sync(broken.getId())).thenThrow(new IllegalStateException("boom"));
        when(syncService.sync(healthy.getId())).thenReturn(SyncResult.success(0, 0, 0, List.of()));

        job.syncAll();

        verify(syncService).sync(healthy.getId());
    }
}
