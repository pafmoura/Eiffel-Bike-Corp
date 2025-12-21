package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.entity.BikeProvider;
import fr.eiffelbikecorp.bikeapi.domain.entity.Customer;
import fr.eiffelbikecorp.bikeapi.dto.response.UserResponse;

public final class UserMapper {
    private UserMapper() {}

    public static UserResponse toResponse(Customer c, BikeProvider provider, String type) {
        return new UserResponse(
                c.getId(),
                type,
                c.getFullName(),
                c.getEmail(),
                provider != null ? provider.getId() : null
        );
    }
}
