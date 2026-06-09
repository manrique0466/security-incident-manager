package com.securityincidentmanager.mapper;

import com.securityincidentmanager.domain.entity.Incident;
import com.securityincidentmanager.dto.request.IncidentCreateRequest;
import com.securityincidentmanager.dto.response.IncidentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IncidentMapper {

    @Mapping(source = "reporter.id", target = "reporterId")
    @Mapping(source = "assignedAnalyst.id", target = "assignedAnalystId")
    IncidentResponse toResponse(Incident incident);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "assignedAnalyst", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Incident toEntity(IncidentCreateRequest request);

}
