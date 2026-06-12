package com.securityincidentmanager.domain.repository;

import com.securityincidentmanager.domain.entity.Incident;
import com.securityincidentmanager.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IncidentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("incident_manager_test")
            .withUsername("appuser")
            .withPassword("apppassword");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    private User reporter;
    private User analyst;

    @BeforeEach
    void setup() {
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        reporter = new User();
        reporter.setUsername("reporter");
        reporter.setEmail("reporter@example.com");
        reporter.setPassword("hashedpassword");
        reporter.setRole(User.Role.ANALYST);
        reporter = userRepository.save(reporter);

        analyst = new User();
        analyst.setUsername("analyst");
        analyst.setEmail("analyst@example.com");
        analyst.setPassword("hashedpassword");
        analyst.setRole(User.Role.ANALYST);
        analyst = userRepository.save(analyst);
    }

    private Incident buildIncident(String title, User reporter) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setReporter(reporter);
        incident.setPriority(Incident.Priority.HIGH);
        incident.setStatus(Incident.Status.OPEN);
        return incident;
    }

    @Test
    void shouldSaveAndFindIncidentById() {
        Incident saved = incidentRepository.save(buildIncident("Test incident", reporter));

        assertThat(saved.getId()).isNotNull();
        assertThat(incidentRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void shouldFindAllNonDeletedIncidents() {
        incidentRepository.save(buildIncident("Active incident", reporter));
        Incident deleted = buildIncident("Deleted incident", reporter);
        deleted.setDeletedAt(LocalDateTime.now());
        incidentRepository.save(deleted);

        Page<Incident> result = incidentRepository.findAllByDeletedAtIsNull(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Active incident");
    }

    @Test
    void shouldFindIncidentByIdWhenNotDeleted() {
        Incident saved = incidentRepository.save(buildIncident("Active incident", reporter));

        Optional<Incident> result = incidentRepository.findByIdAndDeletedAtIsNull(saved.getId());

        assertThat(result).isPresent();
    }

    @Test
    void shouldNotFindIncidentByIdWhenDeleted() {
        Incident deleted = buildIncident("Deleted incident", reporter);
        deleted.setDeletedAt(LocalDateTime.now());
        Incident saved = incidentRepository.save(deleted);

        Optional<Incident> result = incidentRepository.findByIdAndDeletedAtIsNull(saved.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void ShouldFindIncidentByReporter() {
        incidentRepository.save(buildIncident("Reporter's incident", reporter));
        incidentRepository.save(buildIncident("Other's reporter's incident", analyst));

        List<Incident> result = incidentRepository
                .findAllByReporterAndDeletedAtIsNull(reporter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Reporter's incident");
    }

    @Test
    void shouldFindIncidentsByAssignedAnalyst() {
        Incident assigned = buildIncident("Assigned incident", reporter);
        assigned.setAssignedAnalyst(analyst);
        incidentRepository.save(assigned);
        incidentRepository.save(buildIncident("Unassigned incident", reporter));

        List<Incident> result = incidentRepository
                .findAllByAssignedAnalystAndDeletedAtIsNull(analyst);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Assigned incident");
    }

    @Test
    void shouldNotReturnDeletedIncidentsForReporter() {
        Incident deleted = buildIncident("Deleted reporter incident", reporter);
        deleted.setDeletedAt(LocalDateTime.now());
        incidentRepository.save(deleted);

        List<Incident> result = incidentRepository
                .findAllByReporterAndDeletedAtIsNull(reporter);

        assertThat(result).isEmpty();
    }
}
