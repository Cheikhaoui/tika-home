package com.tikahome.dto.admin;

import com.tikahome.entity.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create: all fields required. Update: {@code importUrl} may be empty to keep the current URL;
 * {@code type} is ignored.
 */
public record ChannelRequest(
        @NotNull ChannelType type,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) @Pattern(regexp = "^$|^https?://\\S+$", message = "must be an http(s) URL") String importUrl,
        Boolean enabled) {
}
