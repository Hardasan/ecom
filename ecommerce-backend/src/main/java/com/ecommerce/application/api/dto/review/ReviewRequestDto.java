package com.ecommerce.application.api.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Body for both creating and editing a review. Rating is required; title and comment are optional
 * (a bare rating with no text is allowed).
 */
@Getter
@Setter
public class ReviewRequestDto {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 255)
    private String title;

    @Size(max = 4000)
    private String comment;
}
