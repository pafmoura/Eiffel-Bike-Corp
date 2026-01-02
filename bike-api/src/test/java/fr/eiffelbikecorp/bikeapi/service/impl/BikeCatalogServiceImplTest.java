package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.entity.Employee;
import fr.eiffelbikecorp.bikeapi.domain.entity.Student;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeUpdateRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.exceptions.ValidationException;
import fr.eiffelbikecorp.bikeapi.persistence.BikeRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EmployeeRepository;
import fr.eiffelbikecorp.bikeapi.persistence.StudentRepository;
import fr.eiffelbikecorp.bikeapi.service.BikeCatalogService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BikeCatalogServiceImplTest {

    @Autowired
    BikeCatalogService bikeCatalogService;

    @Autowired
    EiffelBikeCorpRepository eiffelBikeCorpRepository;
    @Autowired
    StudentRepository studentRepository;
    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    BikeRepository bikeRepository;

    private UUID corpId;
    private UUID studentId;
    private UUID employeeId;

    @BeforeEach
    void setup() {
        EiffelBikeCorp corp = new EiffelBikeCorp();
        corp.setId(UUID.randomUUID());
        corpId = eiffelBikeCorpRepository.saveAndFlush(corp).getId();
        Student student = new Student();
        student.setId(UUID.randomUUID());
        studentId = studentRepository.saveAndFlush(student).getId();
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employeeId = employeeRepository.saveAndFlush(employee).getId();

    }

    @Test
    void should_offer_bike_for_rent_by_eiffel_corp_and_return_bike_response() {
        BikeCreateRequest req = new BikeCreateRequest(
                "City bike - good condition",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("2.50")
        );
        BikeResponse resp = bikeCatalogService.offerBikeForRent(req, req.offeredById());
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isNotNull();
        assertThat(resp.description()).isEqualTo(req.description());
        assertThat(resp.status()).isEqualTo("AVAILABLE");
        assertThat(resp.rentalDailyRateEur()).isEqualByComparingTo(req.rentalDailyRateEur());
        assertThat(resp.offeredBy()).isNotNull();
        assertThat(resp.offeredBy().type()).isEqualTo("EIFFEL_BIKE_CORP");
        assertThat(resp.offeredBy().id()).isEqualTo(corpId);
        assertThat(bikeRepository.findById(resp.id())).isPresent();
    }

    @Test
    void should_offer_bike_for_rent_by_student_and_return_bike_response() {
        BikeCreateRequest req = new BikeCreateRequest(
                "City bike - good condition",
                ProviderType.STUDENT,
                studentId,
                new BigDecimal("2.50")
        );
        BikeResponse resp = bikeCatalogService.offerBikeForRent(req, req.offeredById());
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isNotNull();
        assertThat(resp.description()).isEqualTo(req.description());
        assertThat(resp.status()).isEqualTo("AVAILABLE");
        assertThat(resp.rentalDailyRateEur()).isEqualByComparingTo(req.rentalDailyRateEur());
        assertThat(resp.offeredBy()).isNotNull();
        assertThat(resp.offeredBy().type()).isEqualTo("STUDENT");
        assertThat(resp.offeredBy().id()).isEqualTo(studentId);
        assertThat(bikeRepository.findById(resp.id())).isPresent();
    }

    @Test
    void should_offer_bike_for_rent_by_employee_and_return_bike_response() {
        BikeCreateRequest req = new BikeCreateRequest(
                "City bike - good condition",
                ProviderType.EMPLOYEE,
                employeeId,
                new BigDecimal("2.50")
        );
        BikeResponse resp = bikeCatalogService.offerBikeForRent(req, req.offeredById());
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isNotNull();
        assertThat(resp.description()).isEqualTo(req.description());
        assertThat(resp.status()).isEqualTo("AVAILABLE");
        assertThat(resp.rentalDailyRateEur()).isEqualByComparingTo(req.rentalDailyRateEur());
        assertThat(resp.offeredBy()).isNotNull();
        assertThat(resp.offeredBy().type()).isEqualTo("EMPLOYEE");
        assertThat(resp.offeredBy().id()).isEqualTo(employeeId);
        assertThat(bikeRepository.findById(resp.id())).isPresent();
    }

    @Test
    void should_throw_NotFoundException_when_provider_does_not_exist() {
        UUID missingCorpId = UUID.randomUUID();
        BikeCreateRequest req = new BikeCreateRequest(
                "Bike with missing provider",
                ProviderType.EIFFEL_BIKE_CORP,
                missingCorpId,
                new BigDecimal("3.00")
        );
        assertThatThrownBy(() -> bikeCatalogService.offerBikeForRent(req, req.offeredById()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("EiffelBikeCorp not found")
                .hasMessageContaining(missingCorpId.toString());
    }

    @Test
    void should_throw_NotFoundException_when_update_bike_not_found() {
        BikeUpdateRequest req = new BikeUpdateRequest(
                "new desc",
                "AVAILABLE",
                new BigDecimal("4.00")
        );
        assertThatThrownBy(() -> bikeCatalogService.updateBike(999999L, req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Bike not found");
    }

    @Test
    void should_throw_ValidationException_when_update_bike_with_invalid_status() {
        BikeCreateRequest create = new BikeCreateRequest(
                "Bike to update",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("3.00")
        );
        BikeResponse created = bikeCatalogService.offerBikeForRent(create, create.offeredById());
        BikeUpdateRequest invalid = new BikeUpdateRequest(
                null,
                "NOT_A_REAL_STATUS",
                null
        );
        assertThatThrownBy(() -> bikeCatalogService.updateBike(created.id(), invalid))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid bike status");
    }

    @Test
    void should_throw_ValidationException_when_search_with_invalid_status() {
        assertThatThrownBy(() -> bikeCatalogService.searchBikesToRent("BAD_STATUS", null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid bike status");
    }

    @Test
    void should_findAll_include_created_bike() {
        BikeCreateRequest req = new BikeCreateRequest(
                "FindAll bike",
                ProviderType.EIFFEL_BIKE_CORP,
                corpId,
                new BigDecimal("1.50")
        );
        BikeResponse created = bikeCatalogService.offerBikeForRent(req, req.offeredById());
        List<BikeResponse> all = bikeCatalogService.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all).anySatisfy(b -> assertThat(b.id()).isEqualTo(created.id()));
    }
}
