package com.ecommerce.application.integration.checkout;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.persistence.entity.enumeration.Province;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressITest extends AbstractCheckoutITest {

    @Test
    void first_address_is_created_as_default() throws Exception {
        createAddress(userToken, addressRequest(Province.TEHRAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.province").value("TEHRAN"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void creating_address_without_auth_returns_401() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/addresses")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest(Province.TEHRAN))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalid_address_missing_required_fields_returns_400() throws Exception {
        AddressRequestDto req = addressRequest(Province.TEHRAN);
        req.setPostalCode(null);

        createAddress(userToken, req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void second_default_address_demotes_the_previous_default() throws Exception {
        createAddressAndGetId(userToken, Province.TEHRAN);

        AddressRequestDto second = addressRequest(Province.FARS);
        second.setIsDefault(true);
        createAddress(userToken, second).andExpect(status().isOk());

        // list is ordered default-first; exactly one default remains.
        listAddresses(userToken).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[0].province").value("FARS"))
                .andExpect(jsonPath("$[1].isDefault").value(false));
    }

    @Test
    void update_changes_fields() throws Exception {
        long id = createAddressAndGetId(userToken, Province.TEHRAN);

        AddressRequestDto update = addressRequest(Province.ISFAHAN);
        update.setCity("Isfahan");
        updateAddress(userToken, id, update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.province").value("ISFAHAN"))
                .andExpect(jsonPath("$.city").value("Isfahan"));
    }

    @Test
    void update_unknown_address_returns_404() throws Exception {
        updateAddress(userToken, 999999L, addressRequest(Province.TEHRAN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void delete_default_promotes_another_address() throws Exception {
        long first = createAddressAndGetId(userToken, Province.TEHRAN);
        createAddressAndGetId(userToken, Province.FARS); // first stays default

        deleteAddress(userToken, first).andExpect(status().isOk());

        listAddresses(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[0].province").value("FARS"));
    }

    @Test
    void delete_unknown_address_returns_404() throws Exception {
        deleteAddress(userToken, 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void user_cannot_see_or_touch_another_users_address() throws Exception {
        long id = createAddressAndGetId(userToken, Province.TEHRAN);

        String otherToken = registerAndLogin(newMobile());
        deleteAddress(otherToken, id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ADDRESS_NOT_FOUND"));
        listAddresses(otherToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
