package com.pollnet.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Size(max = 64) String displayName,
        @Size(max = 1000) String bio
) {}
