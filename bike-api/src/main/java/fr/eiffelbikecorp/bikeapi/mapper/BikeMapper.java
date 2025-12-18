package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.Bike;
import fr.eiffelbikecorp.bikeapi.domain.BikeProvider;
import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.dto.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.ProviderRef;

public final class BikeMapper {

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
