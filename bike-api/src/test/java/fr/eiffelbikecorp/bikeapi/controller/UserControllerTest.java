package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.UserType;
import fr.eiffelbikecorp.bikeapi.dto.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.UserResponse;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EmployeeRepository;
import fr.eiffelbikecorp.bikeapi.persistence.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static fr.eiffelbikecorp.bikeapi.Utils.randomEmail;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MockitoExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Test
    void should_register_customer_and_return_201() {
        String email = "customer_" + UUID.randomUUID() + "@test.com";
        UserRegisterRequest req = new UserRegisterRequest(
                UserType.CUSTOMER,
                "John Customer",
                email
        );
        ResponseEntity<UserResponse> r = rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(req),
                UserResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        UserResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.customerId()).isNotNull();
        assertThat(body.type()).isEqualTo("CUSTOMER");
        assertThat(body.fullName()).isEqualTo(req.fullName());
        assertThat(body.email()).isEqualTo(req.email());
        assertThat(body.providerId()).isNull();
        assertThat(customerRepository.existsById(body.customerId())).isTrue();
        assertThat(customerRepository.existsByEmailIgnoreCase(email)).isTrue();
    }

    @Test
    void should_register_student_and_return_201_with_provider_id_equal_customer_id() {
        String email = randomEmail();
        UserRegisterRequest req = new UserRegisterRequest(
                UserType.STUDENT,
                "Alice Student",
                email
        );
        ResponseEntity<UserResponse> r = rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(req),
                UserResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UserResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.customerId()).isNotNull();
        assertThat(body.providerId()).isNotNull();
        assertThat(body.type()).isEqualTo("STUDENT");
        // your invariant: provider is always a customer -> same UUID
        assertThat(body.providerId()).isEqualTo(body.customerId());
        // persisted in both tables
        assertThat(customerRepository.existsById(body.customerId())).isTrue();
        assertThat(studentRepository.existsById(body.providerId())).isTrue();
    }

    @Test
    void should_register_employee_and_return_201_with_provider_id_equal_customer_id() {
        String email = randomEmail();
        UserRegisterRequest req = new UserRegisterRequest(
                UserType.EMPLOYEE,
                "Bob Employee",
                email
        );
        ResponseEntity<UserResponse> r = rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(req),
                UserResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UserResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.customerId()).isNotNull();
        assertThat(body.providerId()).isNotNull();
        assertThat(body.type()).isEqualTo("EMPLOYEE");
        // same UUID
        assertThat(body.providerId()).isEqualTo(body.customerId());
        assertThat(customerRepository.existsById(body.customerId())).isTrue();
        assertThat(employeeRepository.existsById(body.providerId())).isTrue();
    }

    @Test
    void should_return_409_when_email_already_registered() {
        String email = randomEmail();
        UserRegisterRequest req1 = new UserRegisterRequest(UserType.CUSTOMER, "First", email);
        UserRegisterRequest req2 = new UserRegisterRequest(UserType.STUDENT, "Second", email);
        ResponseEntity<UserResponse> first = rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(req1),
                UserResponse.class
        );
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<String> second = rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(req2),
                String.class
        );
        // BusinessRuleException -> 409 (via your ExceptionMapper)
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getHeaders().getContentType()).isNotNull();
        assertThat(second.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(second.getBody()).isNotBlank();
    }

    @Test
    void should_return_400_when_register_request_is_invalid() {
        // invalid: type null, fullName blank, email blank
        UserRegisterRequest invalid = new UserRegisterRequest(null, "", "");
        ResponseEntity<String> r = rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(invalid),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
    }

    @Test
    void should_login_and_return_200_with_token_equal_customer_uuid() {
        String email = "login_" + UUID.randomUUID() + "@test.com";

        // Register first
        ResponseEntity<UserResponse> registered = rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(new UserRegisterRequest(UserType.CUSTOMER, "Login User", email)),
                UserResponse.class
        );
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UserResponse regBody = registered.getBody();
        assertThat(regBody).isNotNull();

        // Login
        ResponseEntity<UserLoginResponse> r = rest.exchange(
                "/api/users/login",
                HttpMethod.POST,
                jsonEntity(new UserLoginRequest(email)),
                UserLoginResponse.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");

        UserLoginResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.customerId()).isEqualTo(regBody.customerId());
        assertThat(body.token()).isEqualTo(regBody.customerId().toString());
    }

    @Test
    void should_return_401_when_email_not_found() {
        String email = "missing_" + UUID.randomUUID() + "@test.com";

        ResponseEntity<String> r = rest.exchange(
                "/api/users/login",
                HttpMethod.POST,
                jsonEntity(new UserLoginRequest(email)),
                String.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
    }

    @Test
    void should_return_400_when_login_request_is_invalid() {
        ResponseEntity<String> r = rest.exchange(
                "/api/users/login",
                HttpMethod.POST,
                jsonEntity(new UserLoginRequest("")),
                String.class
        );

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType()).isNotNull();
        assertThat(r.getHeaders().getContentType().toString()).contains("application/json");
        assertThat(r.getBody()).isNotBlank();
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
