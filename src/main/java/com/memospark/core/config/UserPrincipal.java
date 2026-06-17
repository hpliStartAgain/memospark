package com.memospark.core.config;

/**
 * Lightweight principal resolved from the current session.
 * Avoids repeated DB lookups in every controller method.
 */
public record UserPrincipal(Long id, String username, boolean admin) {
}
