package com.tikahome.dto.admin;

import jakarta.validation.constraints.Size;

/** Optional personal message included in the email to the guest. */
public record DeclineRequest(@Size(max = 1000) String message) {
}
