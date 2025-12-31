package fr.eiffelbikecorp.bikeapi.controller;

import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserResponse;
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
        // Added 4th argument: "password123"
        UserRegisterRequest req = new UserRegisterRequest(
                UserType.CUSTOMER,
                "John Customer",
                email,
                "password123"
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
        assertThat(body.fullName()).isEqualTo(req.fullName());
        assertThat(body.email()).isEqualTo(req.email());
        assertThat(customerRepository.existsById(body.customerId())).isTrue();
    }

    @Test
    void should_register_student_and_return_201_with_provider_id_equal_customer_id() {
        String email = randomEmail();
        // Added 4th argument: "password123"
        UserRegisterRequest req = new UserRegisterRequest(
                UserType.STUDENT,
                "Alice Student",
                email,
                "password123"
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
        assertThat(body.type()).isEqualTo("STUDENT");
        assertThat(customerRepository.existsById(body.customerId())).isTrue();
        assertThat(studentRepository.existsById(body.providerId())).isTrue();
    }

    @Test
    void should_register_employee_and_return_201_with_provider_id_equal_customer_id() {
        String email = randomEmail();
        // Added 4th argument: "password123"
        UserRegisterRequest req = new UserRegisterRequest(
                UserType.EMPLOYEE,
                "Bob Employee",
                email,
                "password123"
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
        assertThat(employeeRepository.existsById(body.providerId())).isTrue();
    }

    @Test
    void should_return_409_when_email_already_registered() {
        String email = randomEmail();
        // Added 4th argument to both
        UserRegisterRequest req1 = new UserRegisterRequest(UserType.CUSTOMER, "First", email, "pass1333");
        UserRegisterRequest req2 = new UserRegisterRequest(UserType.STUDENT, "Second", email, "pass2333");
        rest.exchange("/api/users/register", HttpMethod.POST, jsonEntity(req1), UserResponse.class);
        ResponseEntity<String> second = rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(req2),
                String.class
        );
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_return_400_when_register_request_is_invalid() {
        // Record constructor requires 4 arguments. Null/Empty will trigger @Valid 400
        UserRegisterRequest invalid = new UserRegisterRequest(null, "", "", "");
        ResponseEntity<String> r = rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(invalid),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_login_and_return_200_with_token_equal_customer_uuid() {
        String email = "login_" + UUID.randomUUID() + "@test.com";
        String pass = "password123";
        // Register first with password
        rest.exchange(
                "/api/users/register",
                HttpMethod.POST,
                jsonEntity(new UserRegisterRequest(UserType.CUSTOMER, "Login User", email, pass)),
                UserResponse.class
        );
        // Login with email and password (assuming UserLoginRequest now has both)
        ResponseEntity<UserLoginResponse> r = rest.exchange(
                "/api/users/login",
                HttpMethod.POST,
                jsonEntity(new UserLoginRequest(email, pass)),
                UserLoginResponse.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserLoginResponse body = r.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotNull();
        System.out.println("Login token: " + body.accessToken());
    }

    @Test
    void should_return_401_when_email_not_found() {
        String email = "missing_" + UUID.randomUUID() + "@test.com";
        ResponseEntity<String> r = rest.exchange(
                "/api/users/login",
                HttpMethod.POST,
                jsonEntity(new UserLoginRequest(email, "wrongpass")),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void should_return_400_when_login_request_is_invalid() {
        // Login request with empty fields
        ResponseEntity<String> r = rest.exchange(
                "/api/users/login",
                HttpMethod.POST,
                jsonEntity(new UserLoginRequest("", "")),
                String.class
        );
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private <T> HttpEntity<T> jsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}