package com.pollnet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 64)
        String inviteToken,

        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$",
                 message = "username must be 3-32 chars, alphanumeric or underscore")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Size(max = 64)
        String displayName
) {}
