package com.aditya.worldcup.simulation.dto;

public record TeamStrengthResponse(
        Integer attack,
        Integer midfield,
        Integer defense,
        Integer goalkeeper,
        Integer overall,
        Integer chemistry
) {
}