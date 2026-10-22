package com.tellpal.v2.user.web.mobile;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.user.application.ReadingImageConsumerService;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;

/** Sadece local/test normal tüketici; tüm yanıtlar özel ve saklanamaz. */
@RestController
@Profile({"local", "test"})
@RequestMapping("/api/library/reading-images")
@Tag(name="Okuma görsel tüketicisi", description="Ücretsiz hikâyenin doğrulanmış retained LIVE görselleri")
@SecurityRequirement(name="mobileBearerAuth")
@ApiResponses({@ApiResponse(responseCode="503",description="Belirsiz transport hatası",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="400", description="Geçersiz kimlik veya seçim", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="401", description="Geçersiz Firebase bearer", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="403", description="Yerel tüketici veya ücretsiz erişim kapalı", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="404", description="Kopya veya LIVE bulunamadı", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="409", description="LIVE kimliği değişti", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="422", description="Medya bütünlüğü reddedildi", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="429", description="Kaynak veya süre sınırı", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class)))})
public class ReadingImageConsumerController {
    private final ReadingImageConsumerService service;
    private final ReadingImageComparisonResponseWriter writer;
    private final long deadlineMillis;
    public ReadingImageConsumerController(ReadingImageConsumerService service,ReadingImageComparisonResponseWriter writer, @org.springframework.beans.factory.annotation.Value("${tellpal.reading-consumer.deadline-millis:0}") long deadlineMillis) {this.service=service;this.writer=writer;this.deadlineMillis=deadlineMillis;}
    @ApiResponse(responseCode="200",description="Tam LIVE admission",content=@Content(mediaType="application/json",schema=@Schema(implementation=ReadingImageConsumerService.Admission.class)))
    @GetMapping("/manifest")
    @Operation(summary="Ücretsiz hikâyenin tam LIVE descriptor'ını al")
    public ResponseEntity<ReadingImageConsumerService.Admission> manifest(@RequestHeader(name="Authorization", required=false) String bearer,
            @RequestParam long contentId, @RequestParam String language) {
        return response(service.manifest(bearer, contentId, language), MediaType.APPLICATION_JSON);
    }
    @ApiResponse(responseCode="200",description="Yeniden doğrulanmış admission",content=@Content(mediaType="application/json",schema=@Schema(implementation=ReadingImageConsumerService.Admission.class)))
    @GetMapping("/status")
    @Operation(summary="Aynı LIVE erişimini yeniden doğrula")
    public ResponseEntity<ReadingImageConsumerService.Admission> status(@RequestHeader(name="Authorization", required=false) String bearer,
            @RequestParam String accountScope, @RequestParam long contentId, @RequestParam String language, @RequestParam long liveGeneration,
            @RequestParam String revision, @RequestParam String manifestDigest, @RequestParam UUID jobId) {
        return response(service.status(bearer, accountScope, new ReadingImageComparisonApi.Identity(contentId, language, liveGeneration, revision, manifestDigest, jobId)), MediaType.APPLICATION_JSON);
    }
    @GetMapping("/bytes")
    @Operation(summary="Doğrulanmış rendition byte'ını al", description="Range ve original fallback yoktur. Body başlamadan tam doğrulama yapılır.")
    @ApiResponse(responseCode="200",description="Doğrulanmış PNG, JPEG veya WebP",content={@Content(mediaType="image/png",schema=@Schema(type="string",format="binary")),@Content(mediaType="image/jpeg",schema=@Schema(type="string",format="binary")),@Content(mediaType="image/webp",schema=@Schema(type="string",format="binary"))})
    public void bytes(@RequestHeader(name="Authorization", required=false) String bearer,
            @RequestParam String accountScope, @RequestParam long contentId, @RequestParam String language, @RequestParam long liveGeneration,
            @RequestParam String revision, @RequestParam String manifestDigest, @RequestParam UUID jobId,
            @RequestParam Role role, @RequestParam int pageNumber, @RequestParam String renditionKey,
            @RequestHeader(name="Range", required=false) String range,
            jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        if (request.getParameter("canonicalSource")!=null) throw new IllegalArgumentException("SOURCE_DELIVERY_FORBIDDEN");
        if (range!=null) throw new IllegalArgumentException("RANGE_UNSUPPORTED");
        writer.requireSupported(request);
        var lease=writer.acquire(deadlineMillis);
        try {
            var payload=service.read(bearer,accountScope,new ReadingImageComparisonApi.Identity(contentId,language,liveGeneration,revision,manifestDigest,jobId),
                    new ReadingImageDeliveryApi.Selection(role,pageNumber,renditionKey,false));
            writer.write(request,response,payload.bytes(),lease,payload.mime());
        } catch(java.io.IOException|RuntimeException failure){lease.close();throw failure;}
    }
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalidRequest(Exception ignored) {
        return ResponseEntity.badRequest().header(HttpHeaders.CACHE_CONTROL,"private,no-store")
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Geçersiz okuma görseli isteği"));
    }
    @ExceptionHandler({ResponseStatusException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> error(RuntimeException error) {
        HttpStatusCode status=error instanceof ResponseStatusException response ? response.getStatusCode()
                : error instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : error.getMessage()!=null && error.getMessage().contains("RESOURCE") ? HttpStatus.TOO_MANY_REQUESTS
                : error.getMessage()!=null && error.getMessage().contains("TRANSPORT") ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.UNPROCESSABLE_ENTITY;
        var problem=ProblemDetail.forStatusAndDetail(status, "Okuma görseli isteği reddedildi");
        return ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL, "private,no-store").body(problem);
    }
    private static <T> ResponseEntity<T> response(T body, MediaType type) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"private,no-store").contentType(type).body(body);
    }
}
