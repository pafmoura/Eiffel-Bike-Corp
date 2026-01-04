package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.*;
import fr.eiffelbikecorp.bikeapi.domain.enums.UserType;
import fr.eiffelbikecorp.bikeapi.dto.request.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.UserResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.AuthenticationException;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.mapper.UserMapper;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EmployeeRepository;
import fr.eiffelbikecorp.bikeapi.persistence.StudentRepository;
import fr.eiffelbikecorp.bikeapi.security.SecurityUtils;
import fr.eiffelbikecorp.bikeapi.security.TokenService;
import fr.eiffelbikecorp.bikeapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final CustomerRepository customerRepository;
    private final StudentRepository studentRepository;
    private final EmployeeRepository employeeRepository;
    private final TokenService tokenService;
    private final EiffelBikeCorpRepository eiffelBikeCorpRepository;

    @Override
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Email is already registered: " + request.email());
        }
        BikeProvider provider = null;
        if (request.type() == UserType.STUDENT) {
            Student s = new Student();
            provider = studentRepository.save(s);
        } else if (request.type() == UserType.EMPLOYEE) {
            Employee e = new Employee();
            provider = employeeRepository.save(e);
        } else if (request.type() == UserType.EIFFEL_BIKE_CORP) {
            EiffelBikeCorp e = new EiffelBikeCorp();
            provider = eiffelBikeCorpRepository.save(e);
        }
        Customer customer = new Customer();
        //for simplicity, the provider is always a customer
        if (provider != null) {
            customer.setId(provider.getId());
        } else {
            customer.setId(UUID.randomUUID());
        }
        customer.setFullName(request.fullName());
        customer.setEmail(request.email());
        customer.setPassword(SecurityUtils.hashSHA256(request.password()));
        Customer savedCustomer = customerRepository.save(customer);
        return UserMapper.toResponse(savedCustomer, provider, request.type().name());
    }

    @Override
    @Transactional(readOnly = true)
    public UserLoginResponse login(UserLoginRequest request) {
        Customer customer = customerRepository.findByEmailIgnoreCase_AndPassword(
                request.email(),
                SecurityUtils.hashSHA256(request.password())
        ).orElseThrow(() -> new AuthenticationException("Invalid credentials."));
        return new UserLoginResponse(
                tokenService.generateToken(customer),
                "Bearer",
                10L
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(String id) {
        UUID uuid = UUID.fromString(id);

        Customer customer = customerRepository.findById(uuid)
                .orElseThrow(() -> new BusinessRuleException("User not found with ID: " + id));

        return UserMapper.toResponse(customer, null, "MEMBER");
    }
}
