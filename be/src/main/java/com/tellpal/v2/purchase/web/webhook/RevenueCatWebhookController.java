package com.tellpal.v2.purchase.web.webhook;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tellpal.v2.purchase.application.RevenueCatWebhookCommands.ProcessRevenueCatWebhookCommand;
import com.tellpal.v2.purchase.application.RevenueCatWebhookProcessingService;
import com.tellpal.v2.purchase.web.webhook.RevenueCatWebhookResponses.RevenueCatWebhookResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/webhooks/revenuecat")
@Tag(name = "RevenueCat Webhook", description = "RevenueCat webhook ingestion endpoint.")
@SecurityRequirement(name = "revenueCatHeaderAuth")
public class RevenueCatWebhookController {

    private final RevenueCatWebhookProcessingService revenueCatWebhookProcessingService;

    public RevenueCatWebhookController(RevenueCatWebhookProcessingService revenueCatWebhookProcessingService) {
        this.revenueCatWebhookProcessingService = revenueCatWebhookProcessingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Receive RevenueCat webhook", description = "Processes one RevenueCat webhook payload after validating the authorization header.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook processed"),
            @ApiResponse(responseCode = "400", description = "Webhook payload is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "RevenueCat authorization header is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Webhook could not be attributed", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "422", description = "Webhook lookup values are unsupported", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    RevenueCatWebhookResponse receiveWebhook(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody Map<String, Object> payload) {
        return RevenueCatWebhookResponses.toResponse(revenueCatWebhookProcessingService.process(
                new ProcessRevenueCatWebhookCommand(authorizationHeader, payload)));
    }
}
