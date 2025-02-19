package br.com.conectabyte.profissu.dtos;

import br.com.conectabyte.profissu.enums.ContactTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ContactRequestDto(
    @NotBlank ContactTypeEnum type,
    @NotBlank @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$") String value,
    boolean standard) {
}
