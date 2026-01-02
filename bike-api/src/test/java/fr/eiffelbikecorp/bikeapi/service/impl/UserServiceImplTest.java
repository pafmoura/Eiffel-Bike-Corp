package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.AuthenticationException;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EmployeeRepository;
import fr.eiffelbikecorp.bikeapi.persistence.StudentRepository;
import fr.eiffelbikecorp.bikeapi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceImplTest {

    @Autowired
    UserService userService;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    EmployeeRepository employeeService;

    @Autowired
    EiffelBikeCorpRepository eiffelBikeCorpRepository;

    @Test
    void should_register_student_and_return_user_response() {
        String email = uniqueEmail();
        String password = "123456";
        UserRegisterRequest req = new UserRegisterRequest(
                UserType.STUDENT,
                "Alice Student",
                email,
                password
        );
        UserResponse resp = userService.register(req);
        assertThat(resp).isNotNull();
        assertThat(resp.customerId()).isNotNull();
        assertThat(resp.type()).isEqualTo("STUDENT");
        assertThat(resp.fullName()).isEqualTo("Alice Student");
        assertThat(resp.email()).isEqualTo(email);
        // For providers, UserServiceImpl sets customer.id == provider.id (employee id)
        assertThat(resp.providerId()).isNotNull();
        assertThat(resp.providerId()).isEqualTo(resp.customerId());
        // check if saved in DB
        assertThat(customerRepository.findById(resp.customerId())).isPresent();
        assertThat(studentRepository.findById(resp.providerId())).isPresent();
        // Password should be stored hashed (not the raw password)
        var savedCustomer = customerRepository.findById(resp.customerId()).orElseThrow();
        assertThat(savedCustomer.getPassword()).isNotEqualTo(password);
        assertThat(savedCustomer.getPassword()).isNotBlank();
    }

    @Test
    void should_register_employee_and_return_user_response() {
        String email = uniqueEmail();
        String password = "123456";
        UserRegisterRequest req = new UserRegisterRequest(
                UserType.EMPLOYEE,
                "Bob Employee",
                email,
                password
        );
        UserResponse resp = userService.register(req);
        assertThat(resp).isNotNull();
        assertThat(resp.customerId()).isNotNull();
        assertThat(resp.type()).isEqualTo("EMPLOYEE");
        assertThat(resp.fullName()).isEqualTo("Bob Employee");
        assertThat(resp.email()).isEqualTo(email);
        // For providers, UserServiceImpl sets customer.id == provider.id (student id)
        assertThat(resp.providerId()).isNotNull();
        assertThat(resp.providerId()).isEqualTo(resp.customerId());
        // check if saved in DB
        assertThat(customerRepository.findById(resp.customerId())).isPresent();
        assertThat(employeeService.findById(resp.providerId())).isPresent();
        // Password should be stored hashed (not the raw password)
        var savedCustomer = customerRepository.findById(resp.customerId()).orElseThrow();
        assertThat(savedCustomer.getPassword()).isNotEqualTo(password);
        assertThat(savedCustomer.getPassword()).isNotBlank();
    }

    @Test
    void should_register_eiffel_corp_user_and_return_user_response() {
        String email = uniqueEmail();
        String password = "123456";
        UserRegisterRequest req = new UserRegisterRequest(
                UserType.EIFFEL_BIKE_CORP,
                "Eiffel Bike Corp User",
                email,
                password
        );
        UserResponse resp = userService.register(req);
        assertThat(resp).isNotNull();
        assertThat(resp.customerId()).isNotNull();
        assertThat(resp.type()).isEqualTo("EIFFEL_BIKE_CORP");
        assertThat(resp.fullName()).isEqualTo("Eiffel Bike Corp User");
        assertThat(resp.email()).isEqualTo(email);
        // For providers, UserServiceImpl sets customer.id == provider.id (student id)
        assertThat(resp.providerId()).isNotNull();
        assertThat(resp.providerId()).isEqualTo(resp.customerId());
        // check if saved in DB
        assertThat(customerRepository.findById(resp.customerId())).isPresent();
        assertThat(eiffelBikeCorpRepository.findById(resp.providerId())).isPresent();
        // Password should be stored hashed (not the raw password)
        var savedCustomer = customerRepository.findById(resp.customerId()).orElseThrow();
        assertThat(savedCustomer.getPassword()).isNotEqualTo(password);
        assertThat(savedCustomer.getPassword()).isNotBlank();
    }

    @Test
    void should_login_student_and_return_jwt_token() {
        // Arrange: register first
        String email = uniqueEmail();
        String password = "123456";
        userService.register(new UserRegisterRequest(
                UserType.STUDENT,
                "Bob Student",
                email,
                password
        ));
        UserLoginResponse loginResp = userService.login(new UserLoginRequest(email, password));
        assertThat(loginResp).isNotNull();
        assertThat(loginResp.tokenType()).isEqualTo("Bearer");
        assertThat(loginResp.expiresIn()).isEqualTo(10L);
        assertThat(loginResp.accessToken()).isNotBlank();
        // LOG
        log.info("JWT token (truncated)={}", loginResp.accessToken().substring(0, Math.min(25, loginResp.accessToken().length())) + "...");
    }

    @Test
    void should_throw_BusinessRuleException_when_email_already_registered() {
        String email = uniqueEmail();
        UserRegisterRequest first = new UserRegisterRequest(
                UserType.STUDENT,
                "Alice Student",
                email,
                "123456"
        );
        userService.register(first);
        UserRegisterRequest duplicate = new UserRegisterRequest(
                UserType.STUDENT,
                "Alice Student Duplicate",
                email,
                "123456"
        );
        assertThatThrownBy(() -> userService.register(duplicate))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Email is already registered")
                .hasMessageContaining(email);
    }

    @Test
    void should_throw_AuthenticationException_when_credentials_are_invalid() {
        String email = uniqueEmail();
        userService.register(new UserRegisterRequest(
                UserType.STUDENT,
                "Bob Student",
                email,
                "123456"
        ));
        assertThatThrownBy(() -> userService.login(new UserLoginRequest(email, "wrong-password")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid credentials");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@bike.com";
    }
}

