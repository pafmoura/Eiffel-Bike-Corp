package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.entity.Bike;
import fr.eiffelbikecorp.bikeapi.domain.entity.BikeProvider;
import fr.eiffelbikecorp.bikeapi.domain.entity.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.ProviderRef;

public final class  BikeMapper {

    private BikeMapper() {
    }

    public static BikeResponse toResponse(Bike b) {
        if (b == null) return null;
        BikeProvider bikeProvider = b.getOfferedBy();
        String providerType = (bikeProvider instanceof EiffelBikeCorp) ? "CORP" : "CUSTOMER";
        return new BikeResponse(
                b.getId(),
                b.getDescription(),
                b.getStatus().name(),
                new ProviderRef(providerType, bikeProvider.getId()),
                b.getRentalDailyRateEur()
        );
    }
}
