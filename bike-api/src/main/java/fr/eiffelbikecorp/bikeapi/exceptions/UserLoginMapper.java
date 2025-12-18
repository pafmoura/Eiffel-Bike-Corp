package fr.eiffelbikecorp.bikeapi.exceptions;

import fr.eiffelbikecorp.bikeapi.domain.Customer;
import fr.eiffelbikecorp.bikeapi.dto.UserLoginResponse;

public final class UserLoginMapper {
    private UserLoginMapper() {
    }

    public static UserLoginResponse toResponse(Customer customer) {
        return new UserLoginResponse(
                customer.getId(),
                customer.getId().toString() // token = UUID
        );
    }
}
