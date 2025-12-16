package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.domain.Bike;
import fr.eiffelbikecorp.bikeapi.domain.BikeStatus;
import fr.eiffelbikecorp.bikeapi.domain.Provider;
import fr.eiffelbikecorp.bikeapi.dto.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.BikeUpdateRequest;
import fr.eiffelbikecorp.bikeapi.dto.ProviderType;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.exceptions.ValidationException;
import fr.eiffelbikecorp.bikeapi.mapper.BikeMapper;
import fr.eiffelbikecorp.bikeapi.persistence.BikeRepository;
import fr.eiffelbikecorp.bikeapi.persistence.CustomerRepository;
import fr.eiffelbikecorp.bikeapi.persistence.EiffelBikeCorpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BikeCatalogServiceImpl implements BikeCatalogService {

    private final BikeRepository bikeRepository;
    private final CustomerRepository customerRepository;
    private final EiffelBikeCorpRepository eiffelBikeCorpRepository;

    @Override
    @Transactional
    public BikeResponse offerBikeForRent(BikeCreateRequest request) {
        Provider offeredBy = resolveProvider(request.offeredByType(), request.offeredById());
        Bike bike = Bike.builder()
                .description(request.description())
                .status(BikeStatus.AVAILABLE)
                .offeredBy(offeredBy)
                .rentalDailyRateEur(request.rentalDailyRateEur())
                .build();
        Bike saved = bikeRepository.save(bike);
        return BikeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BikeResponse updateBike(Long bikeId, BikeUpdateRequest request) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new NotFoundException("Bike not found: " + bikeId));
        if (request.description() != null && !request.description().isBlank()) {
            bike.setDescription(request.description());
        }
        if (request.status() != null && !request.status().isBlank()) {
            try {
                bike.setStatus(BikeStatus.valueOf(request.status().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid bike status: " + request.status());
            }
        }
        if (request.rentalDailyRateEur() != null) {
            bike.setRentalDailyRateEur(request.rentalDailyRateEur());
        }
        return BikeMapper.toResponse(bikeRepository.save(bike));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BikeResponse> searchBikesToRent(String status, String q, Long offeredById) {
        BikeStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = BikeStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid bike status: " + status);
            }
        }
        List<Bike> bikes;
        if (st != null && q != null && !q.isBlank()) {
            bikes = bikeRepository.findByStatusAndDescriptionContainingIgnoreCase(st, q);
        } else if (st != null) {
            bikes = bikeRepository.findByStatus(st);
        } else if (q != null && !q.isBlank()) {
            bikes = bikeRepository.findByDescriptionContainingIgnoreCase(q);
        } else {
            bikes = bikeRepository.findAll();
        }
        if (offeredById != null) {
            bikes = bikes.stream()
                    .filter(b -> b.getOfferedBy() != null && offeredById.equals(b.getOfferedBy().getId()))
                    .toList();
        }
        return bikes.stream().map(BikeMapper::toResponse).toList();
    }

    private Provider resolveProvider(ProviderType type, Long id) {
        return switch (type) {
            case CUSTOMER -> customerRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
            case CORP -> eiffelBikeCorpRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("EiffelBikeCorp not found: " + id));
        };
    }
}
