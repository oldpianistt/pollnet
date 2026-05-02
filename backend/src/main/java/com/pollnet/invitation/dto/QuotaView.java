package com.pollnet.invitation.dto;

import java.time.OffsetDateTime;

public record QuotaView(int remaining, int monthlyAllowance, OffsetDateTime resetAt) {}
