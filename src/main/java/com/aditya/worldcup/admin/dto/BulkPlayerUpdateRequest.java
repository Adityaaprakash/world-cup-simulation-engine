package com.aditya.worldcup.admin.dto;

import java.util.List;

public record BulkPlayerUpdateRequest(
        List<BulkPlayerUpdateItem> updates
) {
}
