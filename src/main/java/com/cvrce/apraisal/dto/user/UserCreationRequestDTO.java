package com.cvrce.apraisal.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreationRequestDTO {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Employee ID is required")
    @Size(min = 1, max = 50, message = "Employee ID must be between 1 and 50 characters")
    private String employeeId;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotEmpty(message = "At least one role must be assigned")
    private Set<@NotBlank(message = "Role name cannot be blank") String> roleNames;
}
