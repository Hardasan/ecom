package com.ecommerce.application.integration.discount;

import com.ecommerce.application.api.dto.discount.CreateDiscountRequestDto;
import com.ecommerce.persistence.entity.enumeration.Province;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The global usage limit is enforced by an atomic conditional {@code UPDATE} (see
 * {@code DiscountRepository.redeem}). These tests fire many checkouts of the same code simultaneously
 * and assert the limit is honoured exactly — never over-redeemed — under contention.
 */
class DiscountConcurrencyITest extends AbstractDiscountITest {

    @Test
    void concurrent_checkouts_redeem_a_single_use_code_exactly_once() throws Exception {
        runConcurrentRedemption("RACE1", 1, 6);
    }

    @Test
    void concurrent_checkouts_never_exceed_a_multi_use_limit() throws Exception {
        runConcurrentRedemption("RACE3", 3, 8);
    }

    private void runConcurrentRedemption(String code, int usageLimit, int contenders) throws Exception {
        CreateDiscountRequestDto dto = percentage(code, 10);
        dto.setUsageLimit(usageLimit);
        long discountId = createDiscount(dto);
        Long productId = createActiveProduct("conc-" + code, contenders + 5, 500);

        // Each contender is a distinct user with their own cart + address, prepared sequentially so the
        // only thing racing is the checkout redemption.
        List<String> tokens = new ArrayList<>();
        List<Long> addresses = new ArrayList<>();
        for (int i = 0; i < contenders; i++) {
            String token = createUserAndLogin(newMobile());
            addToCart(token, productId, DEFAULT_VARIANT_VALUE, 1);
            tokens.add(token);
            addresses.add(createAddressAndGetId(token, Province.TEHRAN));
        }

        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(contenders);
        AtomicInteger reserved = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < contenders; i++) {
            String token = tokens.get(i);
            long addressId = addresses.get(i);
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    int status = checkoutWithDiscount(token, addressId, code).andReturn().getResponse().getStatus();
                    if (status == 200) {
                        reserved.incrementAndGet();
                    } else if (status == 409) {
                        rejected.incrementAndGet();
                    } else {
                        unexpected.incrementAndGet();
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS), "workers did not start");
        start.countDown(); // release all contenders at once
        assertTrue(done.await(30, TimeUnit.SECONDS), "checkouts did not finish in time");
        pool.shutdownNow();

        assertTrue(errors.isEmpty(), () -> "unexpected errors: " + errors);
        assertEquals(0, unexpected.get(), "every checkout should be a 200 or a 409");
        assertEquals(usageLimit, reserved.get(), "exactly the limit should redeem");
        assertEquals(contenders - usageLimit, rejected.get(), "the rest should be refused");
        // The authoritative counter matches the number of winners — never over-counted.
        assertEquals(usageLimit, usageCount(discountId));
        assertEquals(usageLimit, orderCountForDiscount(discountId));
    }
}
