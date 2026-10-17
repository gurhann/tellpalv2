package com.tellpal.v2.user.web.mobile;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.user.application.ReadingImageComparisonTestService;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;

/** Sadece local/test QA karşılaştırması; tüm yanıtlar özel ve saklanamaz. */
@RestController
@Profile({"local", "test"})
@RequestMapping("/api/test/reading-image-comparison")
@Tag(name="Okuma görsel karşılaştırması", description="Ücretsiz QA kopyasının aynı retained LIVE görselleri")
@SecurityRequirement(name="mobileBearerAuth")
@ApiResponses({@ApiResponse(responseCode="503",description="Belirsiz transport hatası",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="400", description="Geçersiz kimlik veya seçim", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="401", description="Geçersiz Firebase bearer", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="403", description="QA kapsamı veya ücretsiz erişim kapalı", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="404", description="Kopya veya LIVE bulunamadı", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="409", description="LIVE kimliği değişti", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="422", description="Medya bütünlüğü reddedildi", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="429", description="Kaynak veya süre sınırı", content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemDetail.class)))})
public class ReadingImageComparisonTestController {
    private final ReadingImageComparisonTestService service;
    private final ReadingImageComparisonResponseWriter writer;
    public ReadingImageComparisonTestController(ReadingImageComparisonTestService service,ReadingImageComparisonResponseWriter writer) {this.service=service;this.writer=writer;}
    @ApiResponse(responseCode="200",description="Tam LIVE admission",content=@Content(mediaType="application/json",schema=@Schema(implementation=ReadingImageComparisonApi.Admission.class)))
    @GetMapping("/manifest")
    @Operation(summary="QA kopyasının tam LIVE descriptor'ını al")
    public ResponseEntity<ReadingImageComparisonApi.Admission> manifest(@RequestHeader(name="Authorization", required=false) String bearer,
            @RequestParam long contentId, @RequestParam String language) {
        return response(service.manifest(bearer, contentId, language), MediaType.APPLICATION_JSON);
    }
    @ApiResponse(responseCode="200",description="Yeniden doğrulanmış admission",content=@Content(mediaType="application/json",schema=@Schema(implementation=ReadingImageComparisonApi.Admission.class)))
    @GetMapping("/status")
    @Operation(summary="Aynı LIVE erişimini yeniden doğrula")
    public ResponseEntity<ReadingImageComparisonApi.Admission> status(@RequestHeader(name="Authorization", required=false) String bearer,
            @RequestParam long contentId, @RequestParam String language, @RequestParam long liveGeneration,
            @RequestParam String revision, @RequestParam String manifestDigest, @RequestParam UUID jobId) {
        return response(service.status(bearer, new ReadingImageComparisonApi.Identity(contentId, language, liveGeneration, revision, manifestDigest, jobId)), MediaType.APPLICATION_JSON);
    }
    @GetMapping("/bytes")
    @Operation(summary="Sabit kaynak veya rendition byte'ını doğrulayarak al", description="Range ve original fallback yoktur. Body başlamadan tam doğrulama yapılır.")
    @ApiResponse(responseCode="200",description="Doğrulanmış PNG",content=@Content(mediaType="image/png",schema=@Schema(type="string",format="binary")))
    public void bytes(@RequestHeader(name="Authorization", required=false) String bearer,
            @RequestParam long contentId, @RequestParam String language, @RequestParam long liveGeneration,
            @RequestParam String revision, @RequestParam String manifestDigest, @RequestParam UUID jobId,
            @RequestParam Role role, @RequestParam int pageNumber, @RequestParam String renditionKey,
            @RequestParam boolean canonicalSource, @RequestHeader(name="Range", required=false) String range,
            jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        if (range!=null) throw new IllegalArgumentException("RANGE_UNSUPPORTED");
        writer.requireSupported(request);
        var lease=writer.acquire();
        try {
            var bytes=service.read(bearer,new ReadingImageComparisonApi.Identity(contentId,language,liveGeneration,revision,manifestDigest,jobId),
                    new ReadingImageDeliveryApi.Selection(role,pageNumber,renditionKey,canonicalSource));
            writer.write(request,response,bytes,lease);
        } catch(java.io.IOException|RuntimeException failure){lease.close();throw failure;}
    }
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalidRequest(Exception ignored) {
        return ResponseEntity.badRequest().header(HttpHeaders.CACHE_CONTROL,"private,no-store")
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Geçersiz karşılaştırma isteği"));
    }
    @ExceptionHandler({ResponseStatusException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> error(RuntimeException error) {
        HttpStatusCode status=error instanceof ResponseStatusException response ? response.getStatusCode()
                : error instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : error.getMessage()!=null && error.getMessage().contains("RESOURCE") ? HttpStatus.TOO_MANY_REQUESTS
                : error.getMessage()!=null && error.getMessage().contains("TRANSPORT") ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.UNPROCESSABLE_ENTITY;
        var problem=ProblemDetail.forStatusAndDetail(status, "Karşılaştırma isteği reddedildi");
        return ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL, "private,no-store").body(problem);
    }
    private static <T> ResponseEntity<T> response(T body, MediaType type) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"private,no-store").contentType(type).body(body);
    }
}
