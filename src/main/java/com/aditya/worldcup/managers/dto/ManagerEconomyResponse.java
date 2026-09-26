package com.aditya.worldcup.managers.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ManagerEconomyResponse {
    private Integer balance;
    private Integer trainingAllocation;
    private Integer medicalAllocation;
    private Integer scoutingAllocation;
    private List<ResourceTransactionDto> recentTransactions;

    @Getter
    @Setter
    @Builder
    public static class ResourceTransactionDto {
        private String date;
        private Integer amount;
        private String reason;
    }
}
