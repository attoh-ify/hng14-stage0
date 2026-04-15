package hng14.stage0.nameclassifier.dto.payload;

import jakarta.validation.constraints.NotBlank;

public record CreatePayload(
        @NotBlank(message = "Missing or empty name")
        String name
) {
}
