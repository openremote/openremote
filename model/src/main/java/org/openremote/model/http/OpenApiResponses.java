/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.http;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Reusable OpenAPI response annotations for the Manager REST API.
 *
 * <p>Swagger Core resolves annotations which are meta-annotated with {@link ApiResponse}. Keeping the standard
 * responses here gives every resource the same wording while still producing ordinary response objects in the
 * generated OpenAPI document.</p>
 */
public final class OpenApiResponses {

    private OpenApiResponses() {
    }

    @Documented
    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @ApiResponse(responseCode = "200", description = "The request completed successfully", useReturnTypeSchema = true)
    public @interface Ok {
    }

    @Documented
    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @ApiResponse(responseCode = "204", description = "The request completed successfully; no response body is returned")
    public @interface NoContent {
    }

    @Documented
    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @ApiResponse(responseCode = "400", description = "The request parameters or body are invalid",
        content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public @interface BadRequest {
    }

    @Documented
    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @SecurityRequirement(name = "openid")
    @ApiResponse(responseCode = "401", description = "Authentication is required or the supplied credentials are invalid",
        content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "403", description = "The authenticated user does not have permission to perform this operation",
        content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public @interface Authenticated {
    }

    @Documented
    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @ApiResponse(responseCode = "403", description = "The caller does not have permission to access the requested resource",
        content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public @interface Forbidden {
    }

    @Documented
    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @ApiResponse(responseCode = "404", description = "The requested resource does not exist",
        content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public @interface NotFound {
    }

    @Documented
    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @ApiResponse(responseCode = "409", description = "The request conflicts with the current state of the resource",
        content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public @interface Conflict {
    }

    @Documented
    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @ApiResponse(responseCode = "413", description = "The request or generated response exceeds the configured size limit",
        content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public @interface PayloadTooLarge {
    }

    @Documented
    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @ApiResponse(responseCode = "500", description = "The operation failed because of an unexpected server error",
        content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public @interface ServerError {
    }
}
