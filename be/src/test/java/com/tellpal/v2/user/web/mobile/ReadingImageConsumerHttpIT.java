package com.tellpal.v2.user.web.mobile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.tellpal.v2.user.application.ReadingImageConsumerService;
import com.tellpal.v2.content.api.ReadingImageConsumerApi;

@org.springframework.boot.test.context.SpringBootTest(webEnvironment=org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"tellpal.reading-consumer.deadline-millis=1000","tellpal.api-docs.enabled=true"})
class ReadingImageConsumerHttpIT extends com.tellpal.v2.support.PostgresIntegrationTestBase {
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @org.springframework.test.context.bean.override.mockito.MockitoBean ReadingImageConsumerService service;
    @org.springframework.beans.factory.annotation.Autowired ReadingImageComparisonResponseWriter writer;
    String query="?accountScope=account&contentId=1&language=en&liveGeneration=1&revision=a&manifestDigest=b&jobId=12345678-1234-1234-1234-123456789abc&role=READING_DETAIL&pageNumber=0&renditionKey=c";
    @Test void slowSocketHoldsSharedPermitAndDeadlineReleasesPayloadBeforeAnotherTransfer() throws Exception {
        var entered=new java.util.concurrent.CountDownLatch(1);var calls=new java.util.concurrent.atomic.AtomicInteger();
        when(service.read(any(),any(),any(),any())).thenAnswer(c->{entered.countDown();return new ReadingImageConsumerApi.Payload(calls.incrementAndGet()==1?new byte[16*1024*1024]:new byte[]{1,2,3},"image/webp");});
        try(var slow=new java.net.Socket("127.0.0.1",port)) {
            slow.setReceiveBufferSize(1024);
            slow.getOutputStream().write(("GET /api/library/reading-imag%65s/byt%65s"+query+" HTTP/1.1\r\nHost: localhost\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));slow.getOutputStream().flush();
            assertThat(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var denied=request("bytes"+query);
            assertThat(denied.statusCode()).isEqualTo(429);assertThat(denied.headers().firstValue("Cache-Control")).contains("private,no-store");
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while(writer.activeTransfers()!=0 && System.nanoTime()<deadline)Thread.sleep(10);
            assertThat(writer.activeTransfers()).isZero();assertThat(writer.heldPayloadBytes()).isZero();
            var next=request("bytes"+query);
            assertThat(next.statusCode()).isEqualTo(200);assertThat(next.headers().firstValue("Content-Type")).contains("image/webp");assertThat(next.body()).containsExactly(1,2,3);
        }
    }
    @Test void sourceAndRangeAreRejectedBeforeServiceAndOpenApiHasTypedMimeContracts() throws Exception {
        var source=request("bytes"+query+"&canonicalSource=true");assertThat(source.statusCode()).isEqualTo(400);
        assertThat(source.headers().firstValue("Cache-Control")).contains("private,no-store");verifyNoInteractions(service);
        var client=java.net.http.HttpClient.newHttpClient();
        var range=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/library/reading-images/bytes"+query)).header("Range","bytes=0-1").GET().build(),java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        assertThat(range.statusCode()).isEqualTo(400);verifyNoInteractions(service);
        var response=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/v3/api-docs/mobile")).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(200);
        for(String endpoint:java.util.List.of("manifest","status","bytes")) {
            var operation=json.path("paths").path("/api/library/reading-images/"+endpoint).path("get");
            assertThat(operation.path("responses").path("200").path("content").isEmpty()).isFalse();
            assertThat(operation.path("security").toString()).contains("mobileBearerAuth");
            for(String error:java.util.List.of("400","401","403","404","409","422","429","503"))
                assertThat(operation.path("responses").path(error).path("content").path("application/problem+json").path("schema").path("$ref").asText()).endsWith("/ProblemDetail");
        }
        for(String mime:java.util.List.of("image/png","image/jpeg","image/webp"))
            assertThat(json.path("paths").path("/api/library/reading-images/bytes").path("get").path("responses").path("200").path("content").path(mime).path("schema").path("format").asText()).isEqualTo("binary");
    }
    java.net.http.HttpResponse<byte[]> request(String path) throws Exception {
        return java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/library/reading-images/"+path)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofByteArray());
    }
}
