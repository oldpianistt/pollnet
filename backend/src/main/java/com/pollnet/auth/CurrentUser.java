package com.pollnet.auth;

import com.pollnet.common.error.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Convenience accessor for the JWT principal in service/controller layers.
 * Use {@link #required()} when the endpoint is mapped behind authenticated routes
 * — it should never be missing there, but we throw a 401 instead of NPE if it is.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static Optional<AuthenticatedUser> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u)) {
            return Optional.empty();
        }
        return Optional.of(u);
    }

    public static AuthenticatedUser required() {
        return get().orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "Authentication required"));
    }

    public static UUID requiredId() {
        return required().id();
    }
}
