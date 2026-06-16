package com.aditya.worldcup.squadplayers.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record StartingXiRequest(

        @Size(min = 11, max = 11)
        List<Long> playerIds

) {
}