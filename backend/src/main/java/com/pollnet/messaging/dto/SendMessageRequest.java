package com.pollnet.messaging.dto;

import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @Size(max = 4000) String body,
        @Size(max = 255)  String attachmentUrl
) {}
