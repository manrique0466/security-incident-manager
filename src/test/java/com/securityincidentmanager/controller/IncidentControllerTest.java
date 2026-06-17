package com.securityincidentmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securityincidentmanager.domain.entity.Incident;
import com.securityincidentmanager.dto.request.IncidentCreateRequest;
import com.securityincidentmanager.dto.request.IncidentUpdateRequest;
import com.securityincidentmanager.dto.response.IncidentResponse;
import com.securityincidentmanager.auth.CustomUserDetailsService;
import com.securityincidentmanager.auth.JwtTokenService;
import com.securityincidentmanager.service.IncidentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = IncidentController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_shouldReturn201_withResponse() throws Exception {
        UUID reporterId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setTitle("Server down");
        request.setPriority(Incident.Priority.HIGH);

        IncidentResponse response = new IncidentResponse();
        response.setId(incidentId);
        response.setTitle("Server down");
        response.setPriority(Incident.Priority.HIGH);
        response.setStatus(Incident.Status.OPEN);

        when(incidentService.create(any(IncidentCreateRequest.class), eq(reporterId))).thenReturn(response);

        mockMvc.perform(post("/api/incidents")
                        .param("reporterId", reporterId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(incidentId.toString()))
                .andExpect(jsonPath("$.title").value("Server down"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_shouldReturn200_withResponse() throws Exception {
        UUID id = UUID.randomUUID();

        IncidentResponse response = new IncidentResponse();
        response.setId(id);
        response.setTitle("Server down");

        when(incidentService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/incidents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Server down"));
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_shouldReturn200_withList() throws Exception {
        IncidentResponse response = new IncidentResponse();
        response.setTitle("Server down");

        when(incidentService.getAll(any(Pageable.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Server down"));
    }

    // ── getByReporter ─────────────────────────────────────────────────────────

    @Test
    void getByReporter_shouldReturn200_withList() throws Exception {
        UUID reporterId = UUID.randomUUID();

        IncidentResponse response = new IncidentResponse();
        response.setTitle("Reporter incident");

        when(incidentService.getByReporter(reporterId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/incidents/reporter/{reporterId}", reporterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Reporter incident"));
    }

    // ── getByAnalyst ──────────────────────────────────────────────────────────

    @Test
    void getByAnalyst_shouldReturn200_withList() throws Exception {
        UUID analystId = UUID.randomUUID();

        IncidentResponse response = new IncidentResponse();
        response.setTitle("Analyst incident");

        when(incidentService.getByAnalyst(analystId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/incidents/analyst/{analystId}", analystId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Analyst incident"));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_shouldReturn200_withResponse() throws Exception {
        UUID id = UUID.randomUUID();

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setTitle("Updated title");

        IncidentResponse response = new IncidentResponse();
        response.setId(id);
        response.setTitle("Updated title");

        when(incidentService.update(eq(id), any(IncidentUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/incidents/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Updated title"));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/incidents/{id}", id))
                .andExpect(status().isNoContent());

        verify(incidentService).softDelete(id);
    }
}