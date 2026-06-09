package com.securityincidentmanager.dto.request;

import com.securityincidentmanager.domain.entity.Incident;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class IncidentUpdateRequest {

    @Size(max = 200)
    private String title;

    private String description;

    private Incident.Priority priority;

    private Incident.Status status;

    private UUID assignedAnalystId;

}
