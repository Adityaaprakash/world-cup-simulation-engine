package com.aditya.worldcup.transfers.controller;

import com.aditya.worldcup.security.JwtAuthenticationFilter;
import com.aditya.worldcup.transfers.dto.TransferRequest;
import com.aditya.worldcup.transfers.dto.TransferResponse;
import com.aditya.worldcup.transfers.service.PlayerTransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PlayerTransferController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PlayerTransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlayerTransferService playerTransferService;

    @Test
    @WithMockUser
    void validTransferRequestReturnsOk() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, 3L);
        TransferResponse response = new TransferResponse(1L, "Test Player", 2L, 3L, "Success");

        when(playerTransferService.transferPlayer(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(1L))
                .andExpect(jsonPath("$.playerName").value("Test Player"))
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    @WithMockUser
    void invalidTransferRequestReturnsBadRequest() throws Exception {
        // Missing destinationSquadId
        TransferRequest request = new TransferRequest(1L, 2L, null);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
