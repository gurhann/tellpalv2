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

@RestController
@RequestMapping("/api/webhooks/revenuecat")
public class RevenueCatWebhookController {

    private final RevenueCatWebhookProcessingService revenueCatWebhookProcessingService;

    public RevenueCatWebhookController(RevenueCatWebhookProcessingService revenueCatWebhookProcessingService) {
        this.revenueCatWebhookProcessingService = revenueCatWebhookProcessingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    RevenueCatWebhookResponse receiveWebhook(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody Map<String, Object> payload) {
        return RevenueCatWebhookResponses.toResponse(revenueCatWebhookProcessingService.process(
                new ProcessRevenueCatWebhookCommand(authorizationHeader, payload)));
    }
}
