package com.tikahome.dto.admin;

import java.util.List;

/** {@code exportUrl}: our calendar, to paste into Airbnb / Booking.com ("import calendar"). */
public record ChannelsResponse(String exportUrl, List<ChannelDto> channels) {
}
