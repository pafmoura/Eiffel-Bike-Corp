package fr.eiffelbikecorp.bikeapi.mapper;

import fr.eiffelbikecorp.bikeapi.domain.Bike;
import fr.eiffelbikecorp.bikeapi.domain.EiffelBikeCorp;
import fr.eiffelbikecorp.bikeapi.domain.Provider;
import fr.eiffelbikecorp.bikeapi.dto.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.ProviderRef;

public final class BikeMapper {

    private BikeMapper() {
    }

    public static BikeResponse toResponse(Bike b) {
        if (b == null) return null;
        Provider provider = b.getOfferedBy();
        String providerType = (provider instanceof EiffelBikeCorp) ? "CORP" : "CUSTOMER";
        return new BikeResponse(
                b.getId(),
                b.getDescription(),
                b.getStatus().name(),
                new ProviderRef(providerType, provider.getId()),
                b.getRentalDailyRateEur()
        );
    }
}
