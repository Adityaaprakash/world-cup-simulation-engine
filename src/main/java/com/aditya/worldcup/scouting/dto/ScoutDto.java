package com.aditya.worldcup.scouting.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoutDto {
    private Long id;
    private String name;
    private Integer evaluationSkill;
    private Integer potentialEvaluationSkill;
    private Integer tacticalKnowledge;
    private String regionSpecialization;
}
