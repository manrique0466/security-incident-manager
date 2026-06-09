package com.securityincidentmanager.dto.response;

import com.securityincidentmanager.domain.entity.Incident;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponse {

    private UUID id;
    private String title;
    private String description;
    private Incident.Priority priority;
    private Incident.Status status;
    private UUID reporterId;
    private UUID assignedAnalystId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
