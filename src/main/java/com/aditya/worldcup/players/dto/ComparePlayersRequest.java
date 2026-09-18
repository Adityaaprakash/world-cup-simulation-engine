package com.aditya.worldcup.players.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ComparePlayersRequest(
        @NotEmpty
        @Size(min = 2, max = 5, message = "Must compare between 2 and 5 players")
        List<Long> playerIds
) {}
