package com.ecommerce.application.api.dto.returns;

import com.ecommerce.persistence.entity.enumeration.ReturnReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** One selected line in a return request: which order line, how many units, and why. */
@Getter
@Setter
public class CreateReturnItemDto {

    @NotNull
    private Long orderItemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    private ReturnReason reason;
}
