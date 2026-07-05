package com.ecommerce.persistence.entity.embeddable;

import com.ecommerce.persistence.entity.enumeration.Province;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

/**
 * Shipping address captured at checkout so the order is unaffected by later edits or deletion of the
 * source {@code UserAddress}.
 */
@Embeddable
@Getter
@Setter
public class AddressSnapshot {

    @Column(name = "recipient_first_name", nullable = false, length = 255)
    private String recipientFirstName;

    @Column(name = "recipient_last_name", nullable = false, length = 255)
    private String recipientLastName;

    @Column(name = "recipient_mobile", nullable = false, length = 20)
    private String recipientMobile;

    @Column(name = "recipient_national_id", length = 20)
    private String recipientNationalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "province", nullable = false, length = 64)
    private Province province;

    @Column(name = "city", nullable = false, length = 255)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "address_line", nullable = false, columnDefinition = "TEXT")
    private String addressLine;

    @Column(name = "plaque", length = 32)
    private String plaque;

    @Column(name = "unit", length = 32)
    private String unit;
}
