package com.ben.common_lib.payload.response;

import com.ben.common_lib.enums.UserRole;

/** Minimal response used by the gateway to preserve the numeric user-id contract. */
public record ResolvedUserResponse(Long id, UserRole role) {
}
