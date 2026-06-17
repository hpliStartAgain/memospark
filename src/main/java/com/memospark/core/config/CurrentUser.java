package com.memospark.core.config;

import java.lang.annotation.*;

/**
 * Injects the current authenticated user's {@link UserPrincipal} directly
 * into controller method parameters, eliminating the per-request DB lookup
 * pattern of {@code userService.getUserId(userDetails.getUsername())}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
