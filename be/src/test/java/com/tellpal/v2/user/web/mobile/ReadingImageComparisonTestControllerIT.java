package com.tellpal.v2.user.web.mobile;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.tellpal.v2.user.application.*;
import com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;

@org.springframework.boot.test.context.SpringBootTest(webEnvironment=org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"tellpal.reading-comparison.deadline-millis=1000","tellpal.api-docs.enabled=true"})
class ReadingImageComparisonTestControllerIT extends com.tellpal.v2.support.PostgresIntegrationTestBase {
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @org.springframework.test.context.bean.override.mockito.MockitoBean ReadingImageComparisonTestService actualService;
    @org.springframework.beans.factory.annotation.Autowired ReadingImageComparisonResponseWriter writer;
    @Test void slowActualHttpBodyKeepsGateUntilDisconnect() throws Exception {
        var entered=new java.util.concurrent.CountDownLatch(1);
        doAnswer(call->{entered.countDown();return new byte[16*1024*1024];}).when(actualService).read(any(),any(),any());
        String path="/api/test/reading-image-comparison/bytes?contentId=1&language=tr&liveGeneration=1&revision=a&manifestDigest=b&jobId=12345678-1234-1234-1234-123456789abc&role=READING_COVER&pageNumber=0&renditionKey=c&canonicalSource=true";
        try(var slow=new java.net.Socket("127.0.0.1",port)) {
            slow.setReceiveBufferSize(1024);
            slow.getOutputStream().write(("GET "+path+" HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            slow.getOutputStream().flush();
            org.assertj.core.api.Assertions.assertThat(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var client=java.net.http.HttpClient.newHttpClient();
            var response=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+path)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
            org.assertj.core.api.Assertions.assertThat(response.statusCode()).isEqualTo(429);
            org.assertj.core.api.Assertions.assertThat(response.headers().firstValue("Cache-Control")).contains("private,no-store");
        }
    }

    @Test void encodedHandlerUsesSameGateAndSlowOpenSocketTimesOutWithPayloadCleanup() throws Exception {
        var entered=new java.util.concurrent.CountDownLatch(1);var calls=new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(call->{entered.countDown();return calls.incrementAndGet()==1?new byte[16*1024*1024]:new byte[]{1,2,3};}).when(actualService).read(any(),any(),any());
        String query="?contentId=1&language=tr&liveGeneration=1&revision=a&manifestDigest=b&jobId=12345678-1234-1234-1234-123456789abc&role=READING_COVER&pageNumber=0&renditionKey=c&canonicalSource=true";
        try(var slow=new java.net.Socket("127.0.0.1",port)) {
            slow.setReceiveBufferSize(1024);slow.getOutputStream().write(("GET /api/test/reading-image-compar%69son/byt%65s"+query+" HTTP/1.1\r\nHost: localhost\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));slow.getOutputStream().flush();
            org.assertj.core.api.Assertions.assertThat(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var client=java.net.http.HttpClient.newHttpClient();
            var denied=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/test/reading-image-comparison/byt%65s"+query)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
            org.assertj.core.api.Assertions.assertThat(denied.statusCode()).isEqualTo(429);org.assertj.core.api.Assertions.assertThat(denied.headers().firstValue("Cache-Control")).contains("private,no-store");
            var status=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/test/reading-image-compar%69son/status?contentId=1&language=tr&liveGeneration=1&revision=a&manifestDigest=b&jobId=12345678-1234-1234-1234-123456789abc")).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
            org.assertj.core.api.Assertions.assertThat(status.statusCode()).isEqualTo(200);org.assertj.core.api.Assertions.assertThat(status.headers().firstValue("Cache-Control")).contains("private,no-store");
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while(writer.activeTransfers()!=0 && System.nanoTime()<deadline) Thread.sleep(10);
            org.assertj.core.api.Assertions.assertThat(writer.activeTransfers()).isZero();org.assertj.core.api.Assertions.assertThat(writer.heldPayloadBytes()).isZero();
            var second=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/test/reading-image-comparison/bytes"+query)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            org.assertj.core.api.Assertions.assertThat(second.statusCode()).isEqualTo(200);org.assertj.core.api.Assertions.assertThat(second.body()).containsExactly(1,2,3);
        }
    }
    @Test void generatedMobileOpenApiContainsTypedComparisonContract() throws Exception {
        var client=java.net.http.HttpClient.newHttpClient();var response=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/v3/api-docs/mobile")).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
        org.assertj.core.api.Assertions.assertThat(response.statusCode()).isEqualTo(200);
        for(String endpoint:java.util.List.of("manifest","status","bytes")) {
            var responses=json.path("paths").path("/api/test/reading-image-comparison/"+endpoint).path("get").path("responses");
            org.assertj.core.api.Assertions.assertThat(responses.path("200").path("content").isEmpty()).isFalse();
            for(String error:java.util.List.of("400","401","403","404","409","422","429","503"))
                org.assertj.core.api.Assertions.assertThat(responses.path(error).path("content").path("application/problem+json").path("schema").path("$ref").asText()).endsWith("/ProblemDetail");
        }
        var binary=json.path("paths").path("/api/test/reading-image-comparison/bytes").path("get").path("responses").path("200").path("content").path("image/png").path("schema");
        org.assertj.core.api.Assertions.assertThat(binary.path("format").asText()).isEqualTo("binary");
    }
    @Test void actualHttpPreservesDetailAndOrderedPageSelectionsWithOneIdentityAndNoStore() throws Exception {
        var identity=new ReadingImageComparisonApi.Identity(94,"tr",4,"revision","manifest",java.util.UUID.randomUUID());
        var client=java.net.http.HttpClient.newHttpClient();
        for(int page=0;page<=2;page++) {
            var role=page==0?com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role.READING_DETAIL
                    :com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role.READING_PAGE;
            var selection=new com.tellpal.v2.asset.api.ReadingImageDeliveryApi.Selection(role,page,"key-"+page,false);
            byte[] payload={(byte)(page+1),7,9};
            when(actualService.read("Bearer ephemeral",identity,selection)).thenReturn(payload);
            String query="?contentId=94&language=tr&liveGeneration=4&revision=revision&manifestDigest=manifest&jobId="+identity.jobId()
                    +"&role="+role+"&pageNumber="+page+"&renditionKey=key-"+page+"&canonicalSource=false";
            var response=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/test/reading-image-comparison/bytes"+query))
                    .header("Authorization","Bearer ephemeral").GET().build(),java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            org.assertj.core.api.Assertions.assertThat(response.statusCode()).isEqualTo(200);
            org.assertj.core.api.Assertions.assertThat(response.headers().firstValue("Cache-Control")).contains("private,no-store");
            org.assertj.core.api.Assertions.assertThat(response.headers().firstValue("Content-Type")).contains("image/png");
            org.assertj.core.api.Assertions.assertThat(response.body()).containsExactly(payload);
            verify(actualService).read("Bearer ephemeral",identity,selection);
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while(writer.activeTransfers()!=0 && System.nanoTime()<deadline) Thread.sleep(10);
            org.assertj.core.api.Assertions.assertThat(writer.activeTransfers()).isZero();
            org.assertj.core.api.Assertions.assertThat(writer.heldPayloadBytes()).isZero();
        }
        verifyNoMoreInteractions(actualService);
    }
    @Test void unsupportedContainerRefusesBeforeServiceIo() throws Exception {
        var service=mock(ReadingImageComparisonTestService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new ReadingImageComparisonTestController(service,new ReadingImageComparisonResponseWriter(1000))).addFilters(new ReadingImageComparisonCacheFilter()).build();
        mvc.perform(get("/api/test/reading-image-comparison/bytes").param("contentId","1").param("language","tr").param("liveGeneration","1").param("revision","a").param("manifestDigest","b").param("jobId","12345678-1234-1234-1234-123456789abc").param("role","READING_COVER").param("pageNumber","0").param("renditionKey","c").param("canonicalSource","true"))
            .andExpect(status().isForbidden()).andExpect(header().string("Cache-Control","private,no-store"));verifyNoInteractions(service);
    }
    @Test void realBearerIsRequiredAndEveryAuthErrorIsNoStore() throws Exception {
        var tokens=mock(FirebaseTokenVerifier.class);var api=mock(ReadingImageComparisonApi.class);
        var service=new ReadingImageComparisonTestService(tokens,new FirebaseAuthenticationProperties(false,"test",null,true),api);
        var mvc=MockMvcBuilders.standaloneSetup(new ReadingImageComparisonTestController(service,new ReadingImageComparisonResponseWriter(10000))).addFilters(new ReadingImageComparisonCacheFilter()).build();
        mvc.perform(get("/api/test/reading-image-comparison/manifest").param("contentId","1").param("language","tr"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control","private,no-store"));
        when(tokens.verify("wrong")).thenReturn(new VerifiedFirebaseToken("another-user"));
        mvc.perform(get("/api/test/reading-image-comparison/manifest").header("Authorization","Bearer wrong").param("contentId","1").param("language","tr"))
                .andExpect(status().isForbidden()).andExpect(header().string("Cache-Control","private,no-store"));
        mvc.perform(get("/api/test/reading-image-comparison/manifest").param("contentId","bad"))
                .andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control","private,no-store"));
        verify(api,times(2)).requireConfigured(1,"tr");verifyNoMoreInteractions(api);
    }
    @Test void disabledRunRejectsBeforeAnyFirebaseCall() {
        var tokens=mock(FirebaseTokenVerifier.class);var api=mock(ReadingImageComparisonApi.class);
        doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(api).requireConfigured(1,"tr");
        var service=new ReadingImageComparisonTestService(tokens,new FirebaseAuthenticationProperties(false,"test",null,true),api);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->service.manifest("Bearer unused",1,"tr"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(tokens);
    }
    @Test void stubAndRevocationDisabledConfigurationsFailClosed() {
        var tokens=mock(FirebaseTokenVerifier.class);var api=mock(ReadingImageComparisonApi.class);
        for(var auth:java.util.List.of(new FirebaseAuthenticationProperties(true,"test",null,true),new FirebaseAuthenticationProperties(false,"test",null,false))) {
            var service=new ReadingImageComparisonTestService(tokens,auth,api);
            org.assertj.core.api.Assertions.assertThatThrownBy(()->service.manifest("Bearer ignored",1,"tr"))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }
        verifyNoInteractions(tokens);verify(api,times(2)).requireConfigured(1,"tr");verifyNoMoreInteractions(api);
    }
}
