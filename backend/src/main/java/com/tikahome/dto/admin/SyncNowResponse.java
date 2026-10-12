package com.tikahome.dto.admin;

import com.tikahome.model.SyncResult;

public record SyncNowResponse(SyncResult result, ChannelDto channel) {
}
