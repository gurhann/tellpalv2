package com.tellpal.v2.support;

import java.io.PrintStream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.*;
import com.tellpal.v2.TellPalApplication;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.asset.infrastructure.processing.reading.ReadingImageWorker;
import com.tellpal.v2.content.application.publication.*;
import com.tellpal.v2.content.api.*;
import com.tellpal.v2.user.application.FirebaseTokenVerifier;

/** Açık tek yürütme dışında dosya/credential/DB/ağ erişimi yapmayan QA girişidir. */
public final class P4LocalComparisonSetup {
    private static final ObjectMapper JSON = new ObjectMapper();
    private P4LocalComparisonSetup() { }
    public static void main(String[] args) { System.exit(run(args,System.out)); }
    static int run(String[] args,PrintStream output) {
        if (args.length==0 || Arrays.equals(args,new String[]{"--dry-run"})) {
            output.println("DRY_RUN_ZERO_FILE_CREDENTIAL_DB_NETWORK_IO P4A2_FIXED_PLAN_NO_RETRY"); return 0;
        }
        if(args.length!=2 || !args[0].equals("--execute")) {output.println("REFUSED_ARGUMENTS");return 2;}
        try {
            Path directory=Path.of(args[1]).toAbsolutePath().normalize();
            require(directory.toRealPath().equals(directory) && Files.getPosixFilePermissions(directory).equals(PosixFilePermissions.fromString("rwx------")),"PRIVATE_DIRECTORY_REQUIRED");
            var inputs=JSON.readTree(Files.readAllBytes(directory.resolve("inputs.json")));
            var plan=validateInputs(inputs);
            var attempt=JSON.readTree(Files.readAllBytes(directory.resolve("attempt.json")));
            require(attempt.path("planSha256").asText().equals(P4LocalComparisonLedger.PLAN_SHA),"APPROVAL_REQUIRED");
            try(var ledger=new P4LocalComparisonLedger(directory.resolve("ledger.log"),Clock.systemUTC(),1)) {
                execute(directory,inputs,plan,ledger);
            }
            return 0;
        } catch(Exception failure) {
            output.println("REFUSED_OR_FAILED_P4A2_PRIVATE_EVIDENCE_NO_RETRY_NO_CLEANUP");return 2;
        }
    }

    static JsonNode validateInputs(JsonNode inputs) throws Exception {
        byte[] bytes=Files.readAllBytes(Path.of(inputs.path("plan").asText()));
        require(ReadingIdentity.sha(bytes).equals(P4LocalComparisonLedger.PLAN_SHA)
                && inputs.path("planSha256").asText().equals(P4LocalComparisonLedger.PLAN_SHA),"PLAN_HASH_CHANGED");
        var plan=JSON.readTree(bytes);
        String clone=inputs.path("cloneUrl").asText();
        P3LocalCmsSetup.validateUrl(clone);
        P3LocalCmsSetup.validateUrl(inputs.path("sourceUrl").asText());
        require(URI.create(clone.substring(5)).getPath().equals("/"+plan.path("database").path("name").asText())
                && !clone.equals(inputs.path("sourceUrl").asText()) && inputs.path("sourceIdentity").asText().endsWith(":on"),"ISOLATED_CLONE_REQUIRED");
        require(inputs.path("sources").size()==3,"THREE_SOURCES_REQUIRED");
        for(int i=0;i<3;i++) {
            Path source=Path.of(inputs.path("sources").get(i).asText());
            require(source.toRealPath().equals(source) && Files.isRegularFile(source,LinkOption.NOFOLLOW_LINKS)
                    && Files.size(source)==plan.path("sources").get(i).path("bytes").asLong(),"SOURCE_CHANGED");
            require(ReadingIdentity.sha(Files.readAllBytes(source)).equals(plan.path("sources").get(i).path("sha256").asText()),"SOURCE_CHANGED");
        }
        return plan;
    }

    static void credentialGate(Path directory,JsonNode inputs) throws Exception {
        Path path=directory.resolve("qa-sign-in.ready.json");
        require(Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)
                && Files.getPosixFilePermissions(path).equals(PosixFilePermissions.fromString("rw-------")),"PRIVATE_CREDENTIAL_RECEIPT_REQUIRED");
        byte[] bytes=Files.readAllBytes(path);
        require(ReadingIdentity.sha(bytes).equals(inputs.path("credentialReceiptSha256").asText()),"CREDENTIAL_RECEIPT_CHANGED");
        var ready=JSON.readTree(bytes);
        require(ready.path("runId").asText().equals(P4LocalComparisonLedger.RUN)
                && ready.path("qaUid").asText().equals("jLRypTmoBrhhX21HsDJojurcKTC3")
                && ready.path("planSha256").asText().equals(P4LocalComparisonLedger.PLAN_SHA)
                && ready.path("expiresAtEpochMillis").isIntegralNumber() && ready.path("expiresAtEpochMillis").canConvertToLong()
                && ready.path("expiresAtEpochMillis").longValue()>System.currentTimeMillis()+180000,"REAL_QA_CREDENTIAL_READY_REQUIRED");
    }

    static TestProfile profile(JsonNode plan) throws Exception {return JSON.treeToValue(plan.path("preparation"),TestProfile.class);}

    private static void execute(Path directory,JsonNode inputs,JsonNode plan,P4LocalComparisonLedger ledger) throws Exception {
        long contentId;
        var profile=profile(plan);
        try(var context=context(directory,inputs,plan,ledger,0,true)) {
            var jdbc=context.getBean(JdbcTemplate.class);
            require(jdbc.queryForObject("select current_database()",String.class).equals(plan.path("database").path("name").asText()),"CLONE_IDENTITY_CHANGED");
            require(jdbc.queryForObject("select max(version::integer) from flyway_schema_history where success",Integer.class)==37,"CLONE_V37_REQUIRED");
            com.tellpal.v2.asset.application.reading.ReadingPreparationCapacity.requireParentMemory(profile);
            context.getBean(ReadingImageComparisonProperties.class).limits();
            require(Runtime.getRuntime().maxMemory()==plan.path("runtimePreflight").path("parentMaxHeapBytes").asLong(),"PARENT_HEAP_CHANGED");
            for(var role:Role.values()) require(ReadingProfiles.capture(role,profile).fingerprint().equals(plan.path("runtimePreflight").path("roleFingerprints").path(role.name()).asText()),"RUNTIME_FINGERPRINT_CHANGED");
            var baseline=P3LocalCmsSetup.snapshot(jdbc);
            privateJson(directory.resolve("before.json"),baseline);
            var fixture=P4LocalComparisonFixture.seed(context,ledger);
            contentId=fixture.id;
            isolation(context,contentId);
            P3LocalCmsSetup.requirePreserved(baseline,P3LocalCmsSetup.snapshot(jdbc));
            privateJson(directory.resolve("preflight.json"),Map.of("planSha256",P4LocalComparisonLedger.PLAN_SHA,
                    "sourceIdentity",inputs.path("sourceIdentity").asText(),"cloneDatabase",plan.path("database").path("name").asText(),
                    "contentId",contentId,"publicIsolation",true,"cloudCalls",0));
            credentialGate(directory,inputs);
            ledger.record("PREFLIGHT_SEALED",Long.toString(contentId));
            ledger.phase(0);
            long[] originals=upload(context,inputs,plan,ledger);
            ledger.phase(1);
            long[] admitted=new long[3];
            for(int i=0;i<3;i++) admitted[i]=context.getBean(ReadingSourceAdmissionApi.class).admit(originals[i],profile).assetId();
            fixture.rebind(context,admitted);
            var publication=context.getBean(ReadingPublicationService.class);
            ledger.phase(2); ledger.once("RECONCILE_CAPTURE"); publication.reconcile(contentId,"tr");
            UUID job=jdbc.queryForObject("select job_id from reading_publications where content_id=? and language_code='tr'",UUID.class,contentId);
            require(job!=null,"EXACT_JOB_REQUIRED");ledger.record("SEALED_JOB",job.toString());
            ledger.phase(3); ledger.once("RUN_EXACT_JOB");context.getBean(ReadingImageWorker.class).run(job);
            var prepared=context.getBean(ReadingJobStore.class).find(job);
            require(prepared.state()==State.COMPLETED,"EXACT_JOB_FAILED_NO_RETRY");
            ledger.phase(4);ledger.once("RECONCILE_ADOPT");publication.reconcile(contentId,"tr");
            require(publication.status(contentId,"tr").state().equals("LIVE"),"AUTOMATIC_LIVE_REQUIRED");
            isolation(context,contentId);
            P3LocalCmsSetup.requirePreserved(baseline,P3LocalCmsSetup.snapshot(jdbc));
            privateJson(directory.resolve("after.json"),P3LocalCmsSetup.snapshot(jdbc));
            ledger.record("FIXTURE_LIVE",Long.toString(contentId));
        }
        ledger.phase(5);
        try(var context=context(directory,inputs,plan,ledger,contentId,false)) {
            isolation(context,contentId);
            var admitted=context.getBean(ReadingImageComparisonApi.class).manifest(contentId,"tr");
            var result=new TreeMap<String,Object>();result.put("runId",P4LocalComparisonLedger.RUN);
            result.put("planSha256",P4LocalComparisonLedger.PLAN_SHA);result.put("contentId",contentId);
            result.put("loopbackPort",((WebServerApplicationContext)context).getWebServer().getPort());
            result.put("pid",ProcessHandle.current().pid());result.put("identity",admitted.identity());result.put("descriptor",admitted.descriptor());
            privateJson(directory.resolve("ready.json"),result);
            ledger.record("MEDIA_READY",Long.toString(contentId));
            // Sonlu QA sunucusu; yeni I/O penceresi ledger tarafından ayrıca uygulanır.
            long stop=System.nanoTime()+Math.max(0,ledger.remainingMillis()-1000)*1000000L;
            while(System.nanoTime()<stop && !Files.exists(directory.resolve("close.request"))) Thread.sleep(250);
            ledger.record("CLOSED","MEDIA_OWNERSHIP_RELEASED");
        }
    }

    private static long[] upload(ConfigurableApplicationContext context,JsonNode inputs,JsonNode plan,P4LocalComparisonLedger ledger) throws Exception {
        long[] ids=new long[3];var storage=context.getBean(ReadingObjectStorage.class);
        var registry=context.getBean(AssetRegistryApi.class);
        for(int i=0;i<3;i++) {
            byte[] bytes=Files.readAllBytes(Path.of(inputs.path("sources").get(i).asText()));
            require(ReadingIdentity.sha(bytes).equals(plan.path("sources").get(i).path("sha256").asText()),"SOURCE_CHANGED");
            String path=P4LocalComparisonLedger.PREFIX+"/canonical/"+plan.path("sources").get(i).path("assetId").asLong()+".png";
            var stored=storage.create(StorageProvider.FIREBASE_STORAGE,path,"image/png",bytes,8*P4LocalComparisonLedger.MIB,30000);
            ledger.record("SOURCE_VERSION",stored.version()+":"+ReadingIdentity.sha(bytes)+":"+bytes.length);
            ids[i]=registry.register(new RegisterMediaAssetCommand(AssetStorageProvider.FIREBASE_STORAGE,path,AssetKind.ORIGINAL_IMAGE,"image/png",(long)bytes.length,ReadingIdentity.sha(bytes))).assetId();
        }
        return ids;
    }

    static ConfigurableApplicationContext context(Path directory,JsonNode inputs,JsonNode plan,P4LocalComparisonLedger ledger,long id,boolean migrate) {
        var app=new SpringApplication(TellPalApplication.class);
        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>)event->validateEnvironment(event.getEnvironment(),inputs));
        app.addInitializers(boundedInitializer(ledger));
        var arguments=new ArrayList<>(List.of("--spring.profiles.active=local","--spring.config.additional-location=file:"+directory.resolve("application-p4.properties"),
                "--spring.datasource.url="+inputs.path("cloneUrl").asText(),"--spring.flyway.enabled="+migrate,
                "--spring.main.banner-mode=off","--tellpal.reading-comparison.enabled="+(id>0),"--tellpal.reading-comparison.content-id="+id,
                "--tellpal.reading-comparison.language=tr","--tellpal.reading-comparison.run-id="+P4LocalComparisonLedger.RUN));
        plan.path("delivery").fields().forEachRemaining(entry->{
            if(entry.getKey().equals("maxTotalOperations") || entry.getKey().equals("maxTotalBytes") || entry.getKey().equals("maxEncodedBytes")
                    ||entry.getKey().equals("maxPixels")||entry.getKey().equals("maxMemoryBytes")||entry.getKey().equals("deadlineMillis")||entry.getKey().equals("leaseMillis"))
                arguments.add("--tellpal.reading-comparison."+entry.getKey().replaceAll("([A-Z])","-$1").toLowerCase(Locale.ROOT)+"="+entry.getValue().asText());
        });
        var result=app.run(arguments.toArray(String[]::new));
        require(!result.containsBean(org.springframework.scheduling.config.TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME),"SCHEDULER_NOT_REMOVED");
        require(result.getBeansOfType(org.springframework.scheduling.config.ScheduledTaskHolder.class).isEmpty(),"SCHEDULED_TASKS_PRESENT");
        return result;
    }

    static ApplicationContextInitializer<ConfigurableApplicationContext> boundedInitializer(P4LocalComparisonLedger ledger) {
        return context->context.addBeanFactoryPostProcessor(factory->{
            var registry=(BeanDefinitionRegistry)factory;
            String scheduler=org.springframework.scheduling.config.TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME;
            if(registry.containsBeanDefinition(scheduler)) registry.removeBeanDefinition(scheduler);
            factory.addBeanPostProcessor(new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean,String name) {
                    if(bean instanceof AssetStorageClient storage && storage.provider()==StorageProvider.FIREBASE_STORAGE) return new P4LocalComparisonStorage(storage,ledger);
                    if(bean instanceof FirebaseTokenVerifier verifier) return (FirebaseTokenVerifier)token->{ledger.verify();return verifier.verify(token);};
                    return bean;
                }
            });
        });
    }

    static void validateEnvironment(Environment env,JsonNode inputs) {
        require(Set.of(env.getActiveProfiles()).equals(Set.of("local")),"LOCAL_ONLY");
        require(env.getRequiredProperty("spring.datasource.url").equals(inputs.path("cloneUrl").asText()),"CLONE_ONLY");
        require(env.getRequiredProperty("server.address").equals("127.0.0.1") && env.getProperty("server.port",Integer.class)==0,"EPHEMERAL_LOOPBACK_REQUIRED");
        require(env.getRequiredProperty("tellpal.asset.storage.firebase.path-prefix").equals(P4LocalComparisonLedger.PREFIX),"PREFIX_CHANGED");
        require(!env.getProperty("tellpal.asset.processing.poller.enabled",Boolean.class,true),"POLLER_FORBIDDEN");
        require(env.getRequiredProperty("tellpal.asset.storage.firebase.project-id").equals(inputs.path("sdk").path("projectId").asText())
                && env.getRequiredProperty("tellpal.asset.storage.firebase.bucket-name").equals(inputs.path("sdk").path("bucket").asText())
                && env.getRequiredProperty("tellpal.asset.storage.firebase.credentials-path").equals(inputs.path("sdk").path("credentialsPath").asText())
                && env.getRequiredProperty("tellpal.security.firebase.project-id").equals(inputs.path("sdk").path("projectId").asText())
                && env.getRequiredProperty("tellpal.security.firebase.credentials-path").equals(inputs.path("sdk").path("credentialsPath").asText()),"SDK_AUTH_CONFIGURATION_CHANGED");
        require(!env.getProperty("tellpal.security.firebase.stub-tokens-enabled",Boolean.class,true)
                && env.getProperty("tellpal.security.firebase.check-revoked",Boolean.class,false),"REAL_REVOKED_AUTH_REQUIRED");
        require(!env.getProperty("tellpal.asset.storage.firebase.fake-client-enabled",Boolean.class,false),"REAL_STORAGE_REQUIRED");
        require(env.getProperty("tellpal.security.admin.bootstrap.username","").isBlank() && env.getProperty("tellpal.security.admin.bootstrap.password","").isBlank(),"ADMIN_BOOTSTRAP_FORBIDDEN");
    }

    static void isolation(ConfigurableApplicationContext context,long id) throws Exception {
        var api=context.getBean(PublicContentQueryApi.class);
        require(api.findContent(id,P4LocalComparisonFixture.TR,null).isEmpty() && api.findStoryPages(id,P4LocalComparisonFixture.TR).isEmpty()
                && api.listContentsByIds(List.of(id),P4LocalComparisonFixture.TR,null).isEmpty(),"PUBLIC_ISOLATION_FAILED");
        require(context.getBean(ContentPreviewQueryApi.class).list(List.of(id),P4LocalComparisonFixture.TR,false).isEmpty(),"LIBRARY_ISOLATION_FAILED");
        var jdbc=context.getBean(JdbcTemplate.class);
        require(jdbc.queryForObject("select processing_status from content_localizations where content_id=? and language_code='tr'",String.class,id).equals("PENDING"),"LEGACY_PENDING_REQUIRED");
        int port=((WebServerApplicationContext)context).getWebServer().getPort();
        try(var http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            for(String suffix:List.of("/"+id+"?lang=tr","/"+id+"/pages?lang=tr")) {
                var response=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/contents"+suffix)).timeout(Duration.ofSeconds(5)).GET().build(),HttpResponse.BodyHandlers.discarding());
                require(response.statusCode()==404,"REAL_HTTP_PUBLIC_ISOLATION_FAILED");
            }
            var response=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/contents?lang=tr")).timeout(Duration.ofSeconds(5)).GET().build(),HttpResponse.BodyHandlers.ofString());
            require(response.statusCode()==200,"REAL_HTTP_LIST_FAILED");
            for(var item:JSON.readTree(response.body())) require(item.path("contentId").asLong()!=id,"REAL_HTTP_LIST_EXPOSED_FIXTURE");
        }
    }

    static void privateJson(Path path,Object value) throws Exception {
        Files.createFile(path,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try(var channel=java.nio.channels.FileChannel.open(path,StandardOpenOption.WRITE)) {
            var data=java.nio.ByteBuffer.wrap(JSON.writeValueAsBytes(value));while(data.hasRemaining())channel.write(data);channel.force(true);
        }
    }
    static void require(boolean value,String code) {P4LocalComparisonLedger.require(value,code);}
}
