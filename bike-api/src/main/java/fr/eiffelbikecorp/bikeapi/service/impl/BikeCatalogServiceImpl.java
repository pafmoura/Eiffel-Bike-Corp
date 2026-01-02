package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.Bike;
import fr.eiffelbikecorp.bikeapi.domain.entity.BikeProvider;
import fr.eiffelbikecorp.bikeapi.domain.enums.BikeStatus;
import fr.eiffelbikecorp.bikeapi.domain.enums.ProviderType;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeCreateRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.BikeUpdateRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.BikeResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.ReturnNoteResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.exceptions.ValidationException;
import fr.eiffelbikecorp.bikeapi.mapper.BikeMapper;
import fr.eiffelbikecorp.bikeapi.persistence.*;
import fr.eiffelbikecorp.bikeapi.service.BikeCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BikeCatalogServiceImpl implements BikeCatalogService {

    private final BikeRepository bikeRepository;
    private final EiffelBikeCorpRepository eiffelBikeCorpRepository;
    private final StudentRepository studentRepository;
    private final EmployeeRepository employeeRepository;
    private final ReturnNoteRepository returnNoteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReturnNoteResponse> getReturnNotesForBike(Long bikeId) {
        return returnNoteRepository.findAllByRental_Bike_IdOrderByCreatedAtDesc(bikeId)
                .stream()
                .map(note -> new ReturnNoteResponse(
                        note.getId(),
                        note.getCondition(),
                        note.getComment(),
                        note.getCreatedAt(),
                        note.getAuthor().getId()
                ))
                .toList();
    }

    @Override
    @Transactional
    public BikeResponse offerBikeForRent(BikeCreateRequest request, UUID offeredById) {
        BikeProvider offeredBy = resolveProvider(request.offeredByType(), offeredById);
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
    public List<BikeResponse> searchBikesToRent(String status, String q, UUID offeredById) {
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

    private BikeProvider resolveProvider(ProviderType type, UUID id) {
        return switch (type) {
            case STUDENT -> studentRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
            case EIFFEL_BIKE_CORP -> eiffelBikeCorpRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("EiffelBikeCorp not found: " + id));
            case EMPLOYEE -> employeeRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
            default -> throw new ValidationException("Unsupported provider type: " + type);
        };
    }

    @Override
    public List<BikeResponse> findAll() {
        List<Bike> bikes = bikeRepository.findAll();
        return bikes.stream().map(BikeMapper::toResponse).toList();
    }
}
