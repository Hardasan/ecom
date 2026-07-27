package com.ecommerce.application.service.payment;

import com.ecommerce.persistence.entity.Order;

public interface PaymentGateway {

    PaymentInitiation initiate(Order order);

    PaymentVerification verify(String paymentReference);
}
