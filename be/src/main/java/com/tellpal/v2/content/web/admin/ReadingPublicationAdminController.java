package com.tellpal.v2.content.web.admin;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.*;
import com.tellpal.v2.content.api.ReadingPublicationApi;
import com.tellpal.v2.content.application.publication.ReadingPublicationPreviewService;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import com.tellpal.v2.shared.web.admin.AdminApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Yerel/test yayın durumunu ve aynı uygun LIVE görsellerinin admin önizlemesini sunar. */
@AdminApiController
@RequestMapping("/api/admin/contents/{contentId}/reading-publication")
@Tag(name = "Admin Reading Publication", description = "Görsel hazırlığı, yeniden deneme ve doğrulanmış önizleme.")
@SecurityRequirement(name = "adminBearerAuth")
@ApiResponses({
    @ApiResponse(responseCode="400",description="Geçersiz dil veya seçim",content=@Content(schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="401",description="Admin token yok veya geçersiz",content=@Content(schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="403",description="Admin yetkisi gerekli veya özellik kapalı",content=@Content(schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="404",description="İçerik bulunamadı",content=@Content(schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="409",description="Hazır ve uygun görsel bulunamadı veya değişti",content=@Content(schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="422",description="Görsel bütünlüğü reddedildi",content=@Content(schema=@Schema(implementation=ProblemDetail.class))),
    @ApiResponse(responseCode="429",description="Kaynak veya toplam çalışma sınırı aşıldı",content=@Content(schema=@Schema(implementation=ProblemDetail.class)))})
public class ReadingPublicationAdminController {
    private final ReadingPublicationApi publication;
    private final ReadingPublicationPreviewService preview;
    public ReadingPublicationAdminController(ReadingPublicationApi publication, ReadingPublicationPreviewService preview) {
        this.publication=publication;this.preview=preview;
    }
    @GetMapping
    @Operation(summary="Okuma görsellerinin yayın durumunu al",description="Hatalar kaynak adresi taşımayan sınırlı kodlardır. Hazır durumu yalnız yerel/test görsellerini doğrular.")
    @ApiResponse(responseCode="200",description="Seçilen dilin durumu")
    public ReadingPublicationApi.Status status(@PathVariable long contentId,@RequestParam String languageCode) {
        return publication.status(contentId,languageCode);
    }
    @PostMapping("/retry")
    @Operation(summary="Başarısız görsel hazırlığını yeniden dene",description="Güncel başarısız hazırlığı tekrar ister. Yeni yayın onayı gerekmez.")
    @ApiResponse(responseCode="200",description="Yeniden deneme sonrasındaki durum")
    public ReadingPublicationApi.Status retry(@PathVariable long contentId,@RequestParam String languageCode) {
        return publication.retry(contentId,languageCode);
    }
    @GetMapping("/preview")
    @Operation(summary="Hazır görsellerin önizleme listesini al",description="Aynı uygun LIVE'ın kaynak ve çıktı ölçülerini verir. Kaynak adresi veya indirme URL'si içermez.")
    @ApiResponse(responseCode="200",description="Doğrulanmış görsel listesi")
    public ResponseEntity<ReadingPublicationPreviewService.Manifest> preview(@PathVariable long contentId,@RequestParam String languageCode) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"private,no-store").body(preview.manifest(contentId,languageCode));
    }
    @GetMapping("/preview/bytes")
    @Operation(summary="Hazır görselin doğrulanmış byte'ını al",description="Yalnız aynı LIVE kimliğinin seçili kapak veya sayfa çıktısını verir. Kaynak ve Range erişimi kapalıdır.")
    @ApiResponse(responseCode="200",description="Doğrulanmış görsel",content={
        @Content(mediaType="image/jpeg",schema=@Schema(type="string",format="binary")),
        @Content(mediaType="image/png",schema=@Schema(type="string",format="binary")),
        @Content(mediaType="image/webp",schema=@Schema(type="string",format="binary"))})
    public ResponseEntity<Resource> bytes(@PathVariable long contentId,@RequestParam String languageCode,
            @RequestParam long liveGeneration,@RequestParam String revision,@RequestParam String manifestDigest,@RequestParam UUID jobId,
            @RequestParam Role role,@RequestParam int pageNumber,@RequestParam String renditionKey,
            @RequestHeader(name="Range",required=false) String range,jakarta.servlet.http.HttpServletRequest request) {
        if(range!=null || request.getParameter("canonicalSource")!=null)throw new IllegalArgumentException("RENDITION_ONLY");
        var payload=preview.read(new ReadingPublicationPreviewService.Identity(contentId,languageCode,liveGeneration,revision,manifestDigest,jobId),
                new ReadingImageDeliveryApi.Selection(role,pageNumber,renditionKey,false));
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"private,no-store")
                .contentType(MediaType.parseMediaType(payload.mime())).contentLength(payload.bytes().length).body(new ByteArrayResource(payload.bytes()));
    }
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ProblemDetail> invalid(Exception ignored){return failure(HttpStatus.BAD_REQUEST);}
    @ExceptionHandler({ResponseStatusException.class,IllegalArgumentException.class,IllegalStateException.class})
    public ResponseEntity<ProblemDetail> error(RuntimeException error){
        var status=error instanceof ResponseStatusException response?response.getStatusCode()
                :error instanceof IllegalArgumentException?HttpStatus.BAD_REQUEST
                :error.getMessage()!=null && error.getMessage().contains("RESOURCE")?HttpStatus.TOO_MANY_REQUESTS
                :error.getMessage()!=null && error.getMessage().contains("TRANSPORT")?HttpStatus.SERVICE_UNAVAILABLE:HttpStatus.UNPROCESSABLE_ENTITY;
        return failure(status);
    }
    private static ResponseEntity<ProblemDetail> failure(HttpStatusCode status){
        return ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL,"private,no-store")
                .body(ProblemDetail.forStatusAndDetail(status,"Okuma görseli isteği tamamlanamadı"));
    }
}
