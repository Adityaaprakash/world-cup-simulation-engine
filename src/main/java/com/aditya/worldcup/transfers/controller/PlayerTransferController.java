package com.aditya.worldcup.transfers.controller;

import com.aditya.worldcup.transfers.dto.TransferRequest;
import com.aditya.worldcup.transfers.dto.TransferResponse;
import com.aditya.worldcup.transfers.service.PlayerTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class PlayerTransferController {

    private final PlayerTransferService playerTransferService;

    @PostMapping
    public ResponseEntity<TransferResponse> transferPlayer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(playerTransferService.transferPlayer(request, authentication));
    }
}
