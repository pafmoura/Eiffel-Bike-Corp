package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.domain.*;
import fr.eiffelbikecorp.bikeapi.dto.*;
import fr.eiffelbikecorp.bikeapi.exceptions.BusinessRuleException;
import fr.eiffelbikecorp.bikeapi.exceptions.NotFoundException;
import fr.eiffelbikecorp.bikeapi.mapper.SaleNoteMapper;
import fr.eiffelbikecorp.bikeapi.mapper.SaleOfferMapper;
import fr.eiffelbikecorp.bikeapi.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private final BikeRepository bikeRepository;
    private final SaleOfferRepository saleOfferRepository;
    private final SaleNoteRepository saleNoteRepository;
    private final EiffelBikeCorpRepository eiffelBikeCorpRepository;

    @Override
    @Transactional
    public SaleOfferResponse createSaleOffer(CreateSaleOfferRequest request) {

        Bike bike = bikeRepository.findById(request.bikeId())
                .orElseThrow(() -> new NotFoundException("Bike not found: " + request.bikeId()));

        EiffelBikeCorp seller = eiffelBikeCorpRepository.findById(request.sellerCorpId())
                .orElseThrow(() -> new NotFoundException("EiffelBikeCorp not found: " + request.sellerCorpId()));

        // Rule: one sale offer per bike
        if (saleOfferRepository.findByBike_Id(bike.getId()).isPresent()) {
            throw new BusinessRuleException("This bike already has a sale offer.");
        }

        SaleOffer offer = SaleOffer.builder()
                .bike(bike)
                .seller(seller)
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
