package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.entity.Bike;
import fr.eiffelbikecorp.bikeapi.domain.entity.BikeProvider;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.entity.Student;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.dto.ProviderRef;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;

public final class BikeMapper {

    private BikeMapper() {
    }

    public static BikeResponse toResponse(Bike b) {
        if (b == null) return null;
        BikeProvider bikeProvider = b.getOfferedBy();
        String providerType = (bikeProvider instanceof EiffelBikeCorp)
                ? ProviderType.EIFFEL_BIKE_CORP.name()
                : ((bikeProvider instanceof Student) ? ProviderType.STUDENT.name() : ProviderType.EMPLOYEE.name());
        return new BikeResponse(
                b.getId(),
                b.getDescription(),
                b.getStatus().name(),
                new ProviderRef(providerType, bikeProvider.getId()),
                b.getRentalDailyRateEur()
        );
    }
}
