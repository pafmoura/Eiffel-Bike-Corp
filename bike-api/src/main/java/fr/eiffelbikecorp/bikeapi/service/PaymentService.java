package fr.eiffelbikecorp.bikeapi.service;

import fr.eiffelbikecorp.bikeapi.dto.PayPurchaseRequest;
import fr.eiffelbikecorp.bikeapi.dto.PayRentalRequest;
import fr.eiffelbikecorp.bikeapi.dto.RentalPaymentResponse;
import fr.eiffelbikecorp.bikeapi.dto.SalePaymentResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    RentalPaymentResponse payRental(PayRentalRequest request);

    SalePaymentResponse payPurchase(UUID customerId, PayPurchaseRequest request);

    List<RentalPaymentResponse> listPayments(Long rentalId);
}
