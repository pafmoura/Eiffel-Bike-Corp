package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.RentalPaymentResponse;

import java.util.List;

public interface PaymentService {

    RentalPaymentResponse payRental(PayRentalRequest request);

    List<RentalPaymentResponse> listPayments(Long rentalId);
}
