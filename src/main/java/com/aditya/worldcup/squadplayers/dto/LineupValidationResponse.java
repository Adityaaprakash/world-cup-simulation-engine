package com.aditya.worldcup.squadplayers.dto;

public record LineupValidationResponse(

        Boolean valid,

        String message

) {
}