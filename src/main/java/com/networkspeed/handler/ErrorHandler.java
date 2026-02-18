package com.networkspeed.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global error handler for the application.
 */
public class ErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);

    /**
     * Handle any unhandled exception.
     */
    public void handle(RoutingContext ctx) {
        Throwable failure = ctx.failure();

        if (failure != null) {
            LOG.error("Unhandled error: {}", failure.getMessage(), failure);

            int statusCode = ctx.statusCode();
            if (statusCode < 100) {
                statusCode = 500;
            }

            JsonObject errorResponse = new JsonObject()
                    .put("error", getErrorType(statusCode))
                    .put("message", failure.getMessage())
                    .put("status", statusCode);

            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(statusCode)
                    .endAndForget(errorResponse.encode());
        } else {
            int statusCode = ctx.statusCode();
            if (statusCode < 100) {
                statusCode = 500;
            }

            JsonObject errorResponse = new JsonObject()
                    .put("error", getErrorType(statusCode))
                    .put("message", getDefaultMessage(statusCode))
                    .put("status", statusCode);

            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(statusCode)
                    .endAndForget(errorResponse.encode());
        }
    }

    /**
     * Handle 404 Not Found.
     */
    public void handleNotFound(RoutingContext ctx) {
        JsonObject errorResponse = new JsonObject()
                .put("error", "Not Found")
                .put("message", "The requested resource was not found")
                .put("path", ctx.request().path())
                .put("status", 404);

        ctx.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(404)
                .endAndForget(errorResponse.encode());
    }

    private String getErrorType(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Error";
        };
    }

    private String getDefaultMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "The request could not be understood by the server";
            case 401 -> "Authentication is required";
            case 403 -> "You don't have permission to access this resource";
            case 404 -> "The requested resource was not found";
            case 429 -> "Too many requests - please try again later";
            case 500 -> "An unexpected error occurred on the server";
            case 502 -> "The server received an invalid response from an upstream server";
            case 503 -> "The service is temporarily unavailable";
            case 504 -> "The upstream server did not respond in time";
            default -> "An error occurred";
        };
    }
}
