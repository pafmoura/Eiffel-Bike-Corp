package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.request.PayPurchaseRequest;
import fr.eiffelbikecorp.bikeapi.dto.request.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.response.RentalPaymentResponse;
import fr.eiffelbikecorp.bikeapi.dto.response.SalePaymentResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    RentalPaymentResponse payRental(PayRentalRequest request);

    SalePaymentResponse payPurchase(UUID customerId, PayPurchaseRequest request);

    List<RentalPaymentResponse> listPayments(Long rentalId);
}
