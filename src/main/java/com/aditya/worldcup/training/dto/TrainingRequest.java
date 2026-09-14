package com.aditya.worldcup.training.dto;

import com.aditya.worldcup.training.entity.TrainingCategory;
import com.aditya.worldcup.training.entity.TrainingIntensity;
import jakarta.validation.constraints.NotNull;

public record TrainingRequest(
        @NotNull(message = "Training category is required")
        TrainingCategory category,
        
        @NotNull(message = "Training intensity is required")
        TrainingIntensity intensity
) {
}
