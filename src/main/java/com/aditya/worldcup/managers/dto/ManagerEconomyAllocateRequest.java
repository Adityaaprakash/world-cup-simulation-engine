package com.aditya.worldcup.managers.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManagerEconomyAllocateRequest {
    private Integer trainingAllocation;
    private Integer medicalAllocation;
    private Integer scoutingAllocation;
}
