package com.ecommerce.application.api.dto.returns;

import com.ecommerce.persistence.entity.enumeration.ReturnStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin return-queue filter. A null {@code status} lists every request; otherwise only that status
 * (e.g. {@code ?status=REQUESTED} for the pending-review queue). Bound via {@code @ModelAttribute}
 * per the project convention (search/filter never as loose request params).
 */
@Getter
@Setter
public class ReturnSearchRequestDto {

    private ReturnStatus status;
}
