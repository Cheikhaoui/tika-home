package com.tikahome.dto.admin;

import java.util.List;

/** {@code warnings}: channels that could not be refreshed before confirming (e.g. "Airbnb: HTTP 503"). */
public record ConfirmResponse(AdminBookingDetail booking, List<String> warnings) {
}
