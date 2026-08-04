package com.aditya.worldcup.admin.dto;

import java.util.List;

public record ValidationResponse(
        boolean valid,
        List<ValidationMessage> messages
) {
}
