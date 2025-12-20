package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.domain.*;
import fr.eiffelbikecorp.bikeapi.dto.UserLoginRequest;
import fr.eiffelbikecorp.bikeapi.dto.UserLoginResponse;
import fr.eiffelbikecorp.bikeapi.dto.UserRegisterRequest;
import fr.eiffelbikecorp.bikeapi.dto.UserResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.AuthenticationException;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.mapper.UserLoginMapper;
import fr.eiffelbikecorp.bikeapi.mapper.UserMapper;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EmployeeRepository;
import fr.eiffelbikecorp.bikeapi.persistence.StudentRepository;
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
        Customer savedCustomer = customerRepository.save(customer);
        return UserMapper.toResponse(savedCustomer, provider, request.type().name());
    }

    @Override
    @Transactional(readOnly = true)
    public UserLoginResponse login(UserLoginRequest request) {
        Customer customer = customerRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials."));
        return UserLoginMapper.toResponse(customer);
    }
}
