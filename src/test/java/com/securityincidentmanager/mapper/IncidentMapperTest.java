package com.securityincidentmanager.mapper;

import com.securityincidentmanager.domain.entity.Incident;
import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.dto.request.IncidentCreateRequest;
import com.securityincidentmanager.dto.response.IncidentResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentMapperTest {

    private final IncidentMapper incidentMapper = Mappers.getMapper(IncidentMapper.class);

    private User buildReporter() {
        User user = new User();
        user.setUsername("reporter");
        user.setEmail("reporter@example.com");
        user.setPassword("secret");
        user.setRole(User.Role.ANALYST);
        return user;
    }

    @Test
    void shouldMapIncidentFieldsToResponse() {
        Incident incident = new Incident();
        incident.setTitle("Server down");
        incident.setDescription("Production server not responding");
        incident.setPriority(Incident.Priority.CRITICAL);
        incident.setStatus(Incident.Status.OPEN);
        incident.setReporter(buildReporter());

        IncidentResponse response = incidentMapper.toResponse(incident);

        assertThat(response.getTitle()).isEqualTo("Server down");
        assertThat(response.getDescription()).isEqualTo("Production server not responding");
        assertThat(response.getPriority()).isEqualTo(Incident.Priority.CRITICAL);
        assertThat(response.getStatus()).isEqualTo(Incident.Status.OPEN);
    }

    @Test
    void shouldMapReporterIdFromReporter() {
        User reporter = buildReporter();
        Incident incident = new Incident();
        incident.setTitle("Test");
        incident.setPriority(Incident.Priority.LOW);
        incident.setStatus(Incident.Status.OPEN);
        incident.setReporter(reporter);

        IncidentResponse response = incidentMapper.toResponse(incident);

        assertThat(response.getReporterId()).isEqualTo(reporter.getId());
    }

    @Test
    void shouldMapAssignedAnalystIdAsNullWhenNotAssigned() {
        Incident incident = new Incident();
        incident.setTitle("Test");
        incident.setPriority(Incident.Priority.LOW);
        incident.setStatus(Incident.Status.OPEN);
        incident.setReporter(buildReporter());
        incident.setAssignedAnalyst(null);

        IncidentResponse response = incidentMapper.toResponse(incident);

        assertThat(response.getAssignedAnalystId()).isNull();
    }

    @Test
    void shouldMapAssignedAnalystIdWhenAssigned() {
        User analyst = new User();
        analyst.setUsername("analyst");
        analyst.setEmail("analyst@example.com");
        analyst.setPassword("secret");
        analyst.setRole(User.Role.ANALYST);

        Incident incident = new Incident();
        incident.setTitle("Test");
        incident.setPriority(Incident.Priority.LOW);
        incident.setStatus(Incident.Status.OPEN);
        incident.setReporter(buildReporter());
        incident.setAssignedAnalyst(analyst);

        IncidentResponse response = incidentMapper.toResponse(incident);

        assertThat(response.getAssignedAnalystId()).isEqualTo(analyst.getId());
    }

    @Test
    void shouldMapCreateRequestToIncident() {
        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setTitle("New incident");
        request.setDescription("Something went wrong");
        request.setPriority(Incident.Priority.HIGH);

        Incident incident = incidentMapper.toEntity(request);

        assertThat(incident.getTitle()).isEqualTo("New incident");
        assertThat(incident.getDescription()).isEqualTo("Something went wrong");
        assertThat(incident.getPriority()).isEqualTo(Incident.Priority.HIGH);
    }

    @Test
    void shouldLeaveServiceManagedFieldsNullOnCreate() {
        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setTitle("Test");
        request.setPriority(Incident.Priority.LOW);

        Incident incident = incidentMapper.toEntity(request);

        assertThat(incident.getId()).isNull();
        assertThat(incident.getReporter()).isNull();
        assertThat(incident.getAssignedAnalyst()).isNull();
        assertThat(incident.getDeletedAt()).isNull();
        assertThat(incident.getResolvedAt()).isNull();
    }

}
