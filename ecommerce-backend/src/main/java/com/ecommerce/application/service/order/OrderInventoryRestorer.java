package com.ecommerce.application.service.order;

import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.OrderItem;
import com.ecommerce.persistence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OrderInventoryRestorer {

    private final ProductRepository productRepository;

    void restore(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.incrementInventory(item.getProduct().getProductId(), item.getQuantity());
        }
    }
}
