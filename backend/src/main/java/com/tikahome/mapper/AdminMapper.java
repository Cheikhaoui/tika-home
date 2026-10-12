package com.tikahome.mapper;

import com.tikahome.dto.admin.AdminBookingDetail;
import com.tikahome.dto.admin.AdminBookingSummary;
import com.tikahome.dto.admin.BlockSummary;
import com.tikahome.dto.admin.ChannelDto;
import com.tikahome.dto.admin.PriceRuleDto;
import com.tikahome.dto.admin.PropertySettings;
import com.tikahome.entity.BlockedDate;
import com.tikahome.entity.Channel;
import com.tikahome.entity.Booking;
import com.tikahome.entity.Guest;
import com.tikahome.entity.PriceRule;
import com.tikahome.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * Entity -> admin DTO mappings, implemented by MapStruct at compile time (see target/generated-sources).
 * Fields with the same name are copied automatically; only differences are declared. The build fails
 * if a target field is left unmapped (unmappedTargetPolicy=ERROR in pom.xml).
 */
@Mapper
public interface AdminMapper {

    @Mapping(target = "guestName", source = "guest.fullName")
    @Mapping(target = "guestEmail", source = "guest.email")
    AdminBookingSummary toSummary(Booking booking);

    @Mapping(target = "conflicts", source = "conflicts")
    AdminBookingDetail toDetail(Booking booking, List<BlockSummary> conflicts);

    @Mapping(target = "language", source = "preferredLanguage")
    AdminBookingDetail.GuestInfo toGuestInfo(Guest guest);

    @Mapping(target = "channelName", source = "channel.name")
    BlockSummary toBlockSummary(BlockedDate block);

    List<BlockSummary> toBlockSummaries(List<BlockedDate> blocks);

    PropertySettings toSettings(Property property);

    PriceRuleDto toRuleDto(PriceRule rule);

    List<PriceRuleDto> toRuleDtos(List<PriceRule> rules);

    @Mapping(target = "importUrlMasked", source = "importUrl", qualifiedByName = "maskUrl")
    ChannelDto toChannelDto(Channel channel);

    /**
     * "https://www.airbnb.com/calendar/ical/123.ics?s=SECRET" -> "https://www.airbnb.com/…CRET".
     * @Named is essential: without it MapStruct would use this String -> String method for EVERY
     * String field it maps (names, errors...), silently masking them all.
     */
    @Named("maskUrl")
    default String maskUrl(String url) {
        if (url == null) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            String tail = url.length() > 4 ? url.substring(url.length() - 4) : "";
            return uri.getScheme() + "://" + uri.getHost() + "/…" + tail;
        } catch (IllegalArgumentException e) {
            return "…";
        }
    }
}
