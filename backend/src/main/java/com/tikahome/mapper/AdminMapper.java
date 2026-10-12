package com.tikahome.mapper;

import com.tikahome.dto.admin.AdminBookingDetail;
import com.tikahome.dto.admin.AdminBookingSummary;
import com.tikahome.dto.admin.BlockSummary;
import com.tikahome.dto.admin.PriceRuleDto;
import com.tikahome.dto.admin.PropertySettings;
import com.tikahome.entity.BlockedDate;
import com.tikahome.entity.Booking;
import com.tikahome.entity.Guest;
import com.tikahome.entity.PriceRule;
import com.tikahome.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
}
