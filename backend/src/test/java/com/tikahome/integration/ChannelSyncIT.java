package com.tikahome.integration;

import com.tikahome.TestcontainersConfiguration;
import com.tikahome.dto.CreateBookingRequest;
import com.tikahome.dto.GuestRequest;
import com.tikahome.entity.BlockSource;
import com.tikahome.entity.BlockedDate;
import com.tikahome.entity.Channel;
import com.tikahome.entity.ChannelType;
import com.tikahome.entity.EmailType;
import com.tikahome.entity.Property;
import com.tikahome.entity.SyncStatus;
import com.tikahome.exception.BookingRejectedException;
import com.tikahome.model.DateRange;
import com.tikahome.model.SyncResult;
import com.tikahome.repository.BlockedDateRepository;
import com.tikahome.repository.ChannelRepository;
import com.tikahome.repository.PropertyRepository;
import com.tikahome.service.AvailabilityService;
import com.tikahome.service.BookingService;
import com.tikahome.service.ChannelSyncService;
import com.tikahome.support.FakeFeedServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static com.tikahome.support.FakeFeedServer.calendar;
import static com.tikahome.support.FakeFeedServer.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real Postgres + a fake Airbnb feed served over HTTP. Dates are relative to today so tests never expire. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ChannelSyncIT {

    @Autowired ChannelSyncService syncService;
    @Autowired BookingService bookingService;
    @Autowired AvailabilityService availabilityService;
    @Autowired PropertyRepository properties;
    @Autowired ChannelRepository channels;
    @Autowired BlockedDateRepository blockedDates;
    @Autowired JdbcTemplate jdbc;

    FakeFeedServer airbnb;
    Property property;
    Channel channel;
    LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("delete from email_outbox");
        airbnb = new FakeFeedServer();
        property = properties.save(new Property("sync-" + UUID.randomUUID(), "Sync house", 6,
                new BigDecimal("100.00"), BigDecimal.ZERO, "EUR", 1));
        channel = channels.save(new Channel(property, ChannelType.AIRBNB, "Airbnb", airbnb.url()));
        today = LocalDate.now(ZoneId.of("Africa/Casablanca"));
    }

    @AfterEach
    void tearDown() {
        airbnb.close();
    }

    LocalDate day(int daysFromToday) {
        return today.plusDays(daysFromToday);
    }

    @Test
    void firstSyncImportsReservationsAsBlockedDates() {
        airbnb.serve(calendar(
                event("a@airbnb.com", day(10), day(15), "Reserved"),
                event("b@airbnb.com", day(30), day(32), "Airbnb (Not available)")));

        SyncResult result = syncService.sync(channel.getId());

        assertThat(result.status()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(result.created()).isEqualTo(2);
        assertThat(blockedDates.findByChannelId(channel.getId()))
                .allSatisfy(b -> assertThat(b.getSource()).isEqualTo(BlockSource.ICAL))
                .extracting(BlockedDate::getReason)
                .containsExactlyInAnyOrder("Airbnb: Reserved", "Airbnb: Airbnb (Not available)");
        Channel saved = channels.findById(channel.getId()).orElseThrow();
        assertThat(saved.getLastSyncStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(saved.getLastSyncedAt()).isNotNull();
    }

    @Test
    void importedDatesAreUnavailableAndCannotBeBookedDirectly() {
        airbnb.serve(calendar(event("a@airbnb.com", day(10), day(15), "Reserved")));
        syncService.sync(channel.getId());

        var availability = availabilityService.getAvailability(property.getId(), new DateRange(day(8), day(18)));
        assertThat(availability.unavailable()).containsExactly(new DateRange(day(10), day(15)));

        assertThatThrownBy(() -> bookingService.request(booking(day(12), day(14))))
                .isInstanceOf(BookingRejectedException.class)
                .satisfies(e -> assertThat(((BookingRejectedException) e).isDateConflict()).isTrue());
    }

    @Test
    void resyncUpdatesMovedEventsAndDeletesCancelledOnes() {
        airbnb.serve(calendar(
                event("moved@airbnb.com", day(10), day(15), "Reserved"),
                event("cancelled@airbnb.com", day(20), day(22), "Reserved")));
        syncService.sync(channel.getId());

        airbnb.serve(calendar(
                event("moved@airbnb.com", day(11), day(16), "Reserved"),
                event("new@airbnb.com", day(40), day(44), "Reserved")));
        SyncResult result = syncService.sync(channel.getId());

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.deleted()).isEqualTo(1);
        assertThat(blockedDates.findByChannelId(channel.getId()))
                .extracting(b -> new DateRange(b.getStartDate(), b.getEndDate()))
                .containsExactlyInAnyOrder(new DateRange(day(11), day(16)), new DateRange(day(40), day(44)));
    }

    @Test
    void unchangedFeedChangesNothing() {
        airbnb.serve(calendar(event("a@airbnb.com", day(10), day(15), "Reserved")));
        syncService.sync(channel.getId());

        SyncResult again = syncService.sync(channel.getId());

        assertThat(again.created() + again.updated() + again.deleted()).isZero();
    }

    @Test
    void pastStaysAreIgnored() {
        airbnb.serve(calendar(
                event("past@airbnb.com", day(-10), day(-5), "Reserved"),
                event("ongoing@airbnb.com", day(-2), day(3), "Reserved")));

        assertThat(syncService.sync(channel.getId()).created()).isEqualTo(1);
        assertThat(blockedDates.findByChannelId(channel.getId()))
                .extracting(BlockedDate::getExternalUid)
                .containsExactly("ongoing@airbnb.com");
    }

    @Test
    void failedDownloadKeepsExistingBlocks() {
        airbnb.serve(calendar(event("a@airbnb.com", day(10), day(15), "Reserved")));
        syncService.sync(channel.getId());

        airbnb.respond(503, "Service Unavailable");
        SyncResult result = syncService.sync(channel.getId());

        assertThat(result.status()).isEqualTo(SyncStatus.FAILED);
        assertThat(result.error()).isEqualTo("HTTP 503");
        assertThat(blockedDates.findByChannelId(channel.getId())).hasSize(1);
        Channel saved = channels.findById(channel.getId()).orElseThrow();
        assertThat(saved.getLastSyncStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(saved.getLastError()).isEqualTo("HTTP 503").doesNotContain("secret-token");
    }

    @Test
    void invalidFeedKeepsExistingBlocks() {
        airbnb.serve(calendar(event("a@airbnb.com", day(10), day(15), "Reserved")));
        syncService.sync(channel.getId());

        airbnb.serve("<html>Please log in</html>");

        assertThat(syncService.sync(channel.getId()).status()).isEqualTo(SyncStatus.FAILED);
        assertThat(blockedDates.findByChannelId(channel.getId())).hasSize(1);
    }

    @Test
    void reportsDoubleBookingWhenPlatformReservationOverlapsDirectBooking() {
        UUID direct = bookingService.request(booking(day(10), day(14))).getId();

        airbnb.serve(calendar(event("late@airbnb.com", day(12), day(16), "Reserved")));
        SyncResult result = syncService.sync(channel.getId());

        // The Airbnb reservation is real and paid: we import it anyway and flag the conflict.
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.conflictingBookingIds()).containsExactly(direct);
        assertThat(channels.findById(channel.getId()).orElseThrow().getLastConflictCount()).isEqualTo(1);
    }

    @Test
    void doubleBookingAlertIsSentOnlyOnceAcrossSyncs() {
        var direct = bookingService.request(booking(day(10), day(14)));
        airbnb.serve(calendar(event("late@airbnb.com", day(12), day(16), "Reserved")));

        syncService.sync(channel.getId());
        syncService.sync(channel.getId()); // same conflict seen again 15 minutes later

        assertThat(queued(EmailType.DOUBLE_BOOKING_OWNER)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select subject from email_outbox where type = 'DOUBLE_BOOKING_OWNER'",
                String.class)).isEqualTo("Double booking: Airbnb reservation overlaps booking " + direct.getReference());
    }

    @Test
    void ownerIsAlertedOnceAfterThreeFailuresAndOnceOnRecovery() {
        airbnb.respond(500, "oops");
        syncService.sync(channel.getId());
        syncService.sync(channel.getId());
        assertThat(queued(EmailType.SYNC_FAILING_OWNER)).isZero(); // 2 failures: could be a blip

        syncService.sync(channel.getId());
        syncService.sync(channel.getId());
        assertThat(queued(EmailType.SYNC_FAILING_OWNER)).isEqualTo(1); // 3rd failure alerts, 4th doesn't

        airbnb.serve(calendar());
        syncService.sync(channel.getId());
        syncService.sync(channel.getId());
        assertThat(queued(EmailType.SYNC_RECOVERED_OWNER)).isEqualTo(1);
        assertThat(channels.findById(channel.getId()).orElseThrow().getConsecutiveFailures()).isZero();
    }

    @Test
    void shortBlipDoesNotAlert() {
        airbnb.respond(503, "busy");
        syncService.sync(channel.getId());
        syncService.sync(channel.getId());
        airbnb.serve(calendar());
        syncService.sync(channel.getId());

        assertThat(queued(EmailType.SYNC_FAILING_OWNER) + queued(EmailType.SYNC_RECOVERED_OWNER)).isZero();
    }

    private long queued(EmailType type) {
        return jdbc.queryForObject("select count(*) from email_outbox where type = ?", Long.class, type.name());
    }

    private CreateBookingRequest booking(LocalDate checkIn, LocalDate checkOut) {
        return new CreateBookingRequest(property.getId(), checkIn, checkOut, 2,
                new GuestRequest("Direct Guest", "direct@example.com", null, "en"), null);
    }
}
