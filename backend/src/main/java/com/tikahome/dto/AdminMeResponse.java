package com.tikahome.dto;

import java.time.Instant;

public record AdminMeResponse(String email, Instant expiresAt) {
}
