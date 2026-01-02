package fr.eiffelbikecorp.bikeapi.service.impl;

import fr.eiffelbikecorp.bikeapi.domain.entity.Bike;
import fr.eiffelbikecorp.bikeapi.domain.entity.SaleNote;
import fr.eiffelbikecorp.bikeapi.domain.entity.SaleOffer;
import fr.eiffelbikecorp.bikeapi.domain.enums.SaleOfferStatus;
import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleNoteRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.CreateSaleOfferRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleNoteResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferDetailsResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SaleOfferResponse;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.mapper.SaleNoteMapper;
import fr.eiffelbikecorp.bikeapi.mapper.SaleOfferMapper;
import fr.eiffelbikecorp.bikeapi.persistence.BikeRepository;
import fr.eiffelbikecorp.bikeapi.persistence.RentalRepository;
import fr.eiffelbikecorp.bikeapi.persistence.SaleNoteRepository;
import fr.eiffelbikecorp.bikeapi.persistence.SaleOfferRepository;
import fr.eiffelbikecorp.bikeapi.service.SaleOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleOfferServiceImpl implements SaleOfferService {

    private final BikeRepository bikeRepository;
    private final RentalRepository rentalRepository;
    private final SaleOfferRepository saleOfferRepository;
    private final SaleNoteRepository saleNoteRepository;

    @Override
    @Transactional
    public SaleOfferResponse createSaleOffer(CreateSaleOfferRequest request) {
        Bike bike = bikeRepository.findById(request.bikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found: " + request.bikeId()));
        long rentalCount = rentalRepository.countByBike_Id(bike.getId());
        if (rentalCount <= 0) {
            throw new BusinessRuleException("Bike must have been rented at least once to be sold.");
        }
        if (saleOfferRepository.findByBike_Id(bike.getId()).isPresent()) {
            throw new BusinessRuleException("This bike already has a sale offer.");
        }
        if (!bike.getOfferedBy().getId().equals(request.sellerId())) {
            throw new BusinessRuleException("You can only list your own bikes for sale.");
        }
        SaleOffer offer = SaleOffer.builder()
                .bike(bike)
                .sellerId(request.sellerId())
                .askingPriceEur(request.askingPriceEur())
                .status(SaleOfferStatus.LISTED)
                .listedAt(LocalDateTime.now())
                .build();
        SaleOffer saved = saleOfferRepository.save(offer);
        return SaleOfferMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SaleNoteResponse addSaleNote(CreateSaleNoteRequest request) {
        SaleOffer offer = saleOfferRepository.findById(request.saleOfferId())
                .orElseThrow(() -> new NotFoundException("SaleOffer not found: " + request.saleOfferId()));
        if (offer.getStatus() != SaleOfferStatus.LISTED) {
            throw new BusinessRuleException("Cannot add notes to a sale offer that is not LISTED.");
        }
        SaleNote note = SaleNote.builder()
                .saleOffer(offer)
                .title(request.title())
                .content(request.content())
                .createdAt(LocalDateTime.now())
                .createdBy(request.createdBy())
                .build();
        SaleNote saved = saleNoteRepository.save(note);
        return SaleNoteMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleOfferResponse> searchSaleOffers(String q) {
        if (q == null || q.isBlank()) {
            return saleOfferRepository.findByStatusOrderByListedAtDesc(SaleOfferStatus.LISTED)
                    .stream().map(SaleOfferMapper::toResponse).toList();
        }
        return saleOfferRepository
                .findByStatusAndBike_DescriptionContainingIgnoreCaseOrderByListedAtDesc(SaleOfferStatus.LISTED, q)
                .stream().map(SaleOfferMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SaleOfferDetailsResponse getSaleOfferDetailsByBike(Long bikeId) {
        SaleOffer offer = saleOfferRepository.findByBike_Id(bikeId)
                .orElseThrow(() -> new NotFoundException("SaleOffer not found for bike: " + bikeId));
        var notes = saleNoteRepository.findBySaleOffer_IdOrderByCreatedAtDesc(offer.getId())
                .stream().map(SaleNoteMapper::toResponse).toList();
        return new SaleOfferDetailsResponse(SaleOfferMapper.toResponse(offer), notes);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleOfferDetailsResponse getSaleOfferDetails(Long saleOfferId) {
        SaleOffer offer = saleOfferRepository.findById(saleOfferId)
                .orElseThrow(() -> new NotFoundException("SaleOffer not found: " + saleOfferId));
        var notes = saleNoteRepository.findBySaleOffer_IdOrderByCreatedAtDesc(offer.getId())
                .stream().map(SaleNoteMapper::toResponse).toList();
        return new SaleOfferDetailsResponse(SaleOfferMapper.toResponse(offer), notes);
    }
}
