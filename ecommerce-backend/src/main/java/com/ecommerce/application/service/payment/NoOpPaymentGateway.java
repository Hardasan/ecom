package com.ecommerce.application.service.payment;

import com.ecommerce.persistence.entity.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Placeholder IPG until a real payment gateway is wired. Always initiates and verifies successfully.
 */
@Component
public class NoOpPaymentGateway implements PaymentGateway {

    @Override
    public PaymentInitiation initiate(Order order) {
        String reference = "noop-" + order.getId() + "-" + UUID.randomUUID();
        return new PaymentInitiation(reference, "https://payment.example/pay/" + reference);
    }

    @Override
    public PaymentVerification verify(String paymentReference) {
        return new PaymentVerification(true, paymentReference);
    }
}
