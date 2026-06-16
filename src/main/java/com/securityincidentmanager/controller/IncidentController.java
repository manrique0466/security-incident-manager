package com.securityincidentmanager.controller;

import com.securityincidentmanager.dto.request.IncidentCreateRequest;
import com.securityincidentmanager.dto.request.IncidentUpdateRequest;
import com.securityincidentmanager.dto.response.IncidentResponse;
import com.securityincidentmanager.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> create(
            @Valid @RequestBody IncidentCreateRequest request,
            @RequestParam UUID reporterId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(request, reporterId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(incidentService.getAll(pageable));
    }

    @GetMapping("reporter/{reporterId}")
    public ResponseEntity<List<IncidentResponse>> getByReporter(@PathVariable UUID reporterId) {
        return ResponseEntity.ok(incidentService.getByReporter(reporterId));
    }

    @GetMapping("/analyst/{analystId}")
    public ResponseEntity<List<IncidentResponse>> getByAnalyst(@PathVariable UUID analystId) {
        return ResponseEntity.ok(incidentService.getByAnalyst(analystId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentUpdateRequest request) {
        return ResponseEntity.ok(incidentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        incidentService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}