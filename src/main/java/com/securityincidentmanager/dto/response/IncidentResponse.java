package com.securityincidentmanager.dto.response;

import com.securityincidentmanager.domain.entity.Incident;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
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
