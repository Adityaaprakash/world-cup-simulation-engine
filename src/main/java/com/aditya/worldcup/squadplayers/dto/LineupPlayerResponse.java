package com.aditya.worldcup.squadplayers.dto;

public record LineupPlayerResponse(

        Long playerId,

        String playerName,

        String positionSlot,

        Boolean startingXi,

        Boolean captain

) {
}