package com.aditya.worldcup.saves.controller;

import com.aditya.worldcup.saves.dto.CreateSaveSlotRequest;
import com.aditya.worldcup.saves.dto.ImportSaveRequest;
import com.aditya.worldcup.saves.dto.ResumeSaveResponse;
import com.aditya.worldcup.saves.dto.SaveExportResponse;
import com.aditya.worldcup.saves.dto.SaveImportResponse;
import com.aditya.worldcup.saves.dto.SaveSlotResponse;
import com.aditya.worldcup.saves.dto.UpdateSaveSlotRequest;
import com.aditya.worldcup.saves.service.SaveExportService;
import com.aditya.worldcup.saves.service.SaveGameService;
import com.aditya.worldcup.saves.service.SaveImportService;
import com.aditya.worldcup.saves.service.SaveResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/saves")
@RequiredArgsConstructor
@Validated
@Tag(name = "Saves", description = "Manager career save slots and autosaves")
public class SaveGameController {

    private final SaveGameService saveGameService;
    private final SaveExportService saveExportService;
    private final SaveImportService saveImportService;
    private final SaveResumeService saveResumeService;

    @GetMapping
    @Operation(summary = "List save slots", description = "Returns save slots owned by the authenticated manager.")
    @ApiResponse(responseCode = "200", description = "Save slots returned")
    public List<SaveSlotResponse> listSaves(Authentication authentication) {
        return saveGameService.listSaves(authentication);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get save slot", description = "Returns one save slot owned by the authenticated manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Save slot returned"),
            @ApiResponse(responseCode = "404", description = "Save slot not found")
    })
    public SaveSlotResponse getSave(
            @Parameter(description = "Save slot id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return saveGameService.getSave(id, authentication);
    }

    @PostMapping
    @Operation(summary = "Create save slot", description = "Creates a manual save slot metadata snapshot.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Save slot created"),
            @ApiResponse(responseCode = "400", description = "Invalid save request"),
            @ApiResponse(responseCode = "409", description = "Duplicate save slot number")
    })
    public ResponseEntity<SaveSlotResponse> createSave(
            @Valid @RequestBody CreateSaveSlotRequest request,
            Authentication authentication) {

        SaveSlotResponse response = saveGameService.createSave(
                request,
                authentication
        );
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Overwrite save slot", description = "Updates save slot metadata and refreshes the persisted career snapshot.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Save slot updated"),
            @ApiResponse(responseCode = "400", description = "Invalid save request"),
            @ApiResponse(responseCode = "404", description = "Save slot not found")
    })
    public SaveSlotResponse overwriteSave(
            @Parameter(description = "Save slot id")
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateSaveSlotRequest request,
            Authentication authentication) {

        return saveGameService.overwriteSave(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete save slot", description = "Deletes a save slot owned by the authenticated manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Save slot deleted"),
            @ApiResponse(responseCode = "404", description = "Save slot not found"),
            @ApiResponse(responseCode = "409", description = "Active autosave cannot be deleted")
    })
    public void deleteSave(
            @Parameter(description = "Save slot id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        saveGameService.deleteSave(id, authentication);
    }

    @PostMapping("/autosave")
    @Operation(summary = "Create or overwrite autosave", description = "Refreshes the authenticated manager's reserved autosave slot.")
    @ApiResponse(responseCode = "200", description = "Autosave refreshed")
    public SaveSlotResponse autosave(Authentication authentication) {
        return saveGameService.autosave(authentication);
    }

    @PostMapping("/import")
    @Operation(summary = "Import save", description = "Validates a JSON save export and creates a manual save slot for the authenticated manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Save imported"),
            @ApiResponse(responseCode = "400", description = "Invalid save export"),
            @ApiResponse(responseCode = "409", description = "Duplicate slot or manager conflict")
    })
    public ResponseEntity<SaveImportResponse> importSave(
            @Valid @RequestBody ImportSaveRequest request,
            Authentication authentication) {

        SaveImportResponse response = saveImportService.importSave(
                request,
                authentication
        );
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .replacePath("/api/saves/{id}")
                .buildAndExpand(response.saveId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate save slot", description = "Marks the selected save slot as active and deactivates other slots for the manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Save slot activated"),
            @ApiResponse(responseCode = "404", description = "Save slot not found")
    })
    public SaveSlotResponse activateSave(
            @Parameter(description = "Save slot id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return saveGameService.activateSave(id, authentication);
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume save slot", description = "Restores manager progression from the selected save and marks it active.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Save resumed"),
            @ApiResponse(responseCode = "400", description = "Invalid save state"),
            @ApiResponse(responseCode = "404", description = "Save slot not found")
    })
    public ResumeSaveResponse resumeSave(
            @Parameter(description = "Save slot id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return saveResumeService.resumeSave(id, authentication);
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Export save slot", description = "Exports a versioned JSON snapshot for the selected save slot.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Save export returned"),
            @ApiResponse(responseCode = "404", description = "Save slot not found")
    })
    public SaveExportResponse exportSave(
            @Parameter(description = "Save slot id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return saveExportService.exportSave(id, authentication);
    }

    @PostMapping("/{id}/backup")
    @Operation(summary = "Create save backup metadata", description = "Marks the selected save slot with latest backup metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Backup metadata refreshed"),
            @ApiResponse(responseCode = "404", description = "Save slot not found")
    })
    public SaveSlotResponse backupSave(
            @Parameter(description = "Save slot id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return saveGameService.createBackup(id, authentication);
    }
}
