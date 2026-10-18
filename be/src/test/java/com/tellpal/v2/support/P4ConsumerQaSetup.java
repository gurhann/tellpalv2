package com.tellpal.v2.support;

import java.io.PrintStream;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.tellpal.v2.TellPalApplication;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClient;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;
import com.tellpal.v2.user.application.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** Üretim akışını değiştirmeden mevcut LIVE'ı sunan, tek sonlu QA kompozisyonu. */
public final class P4ConsumerQaSetup {
    private static final ObjectMapper JSON=new ObjectMapper();
    static final Path PRIVATE_ROOT=Path.of("/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb");
    static final String RECOVERY_APPROVAL="android/local-waiting-approval-02.private.json";
    static final Map<String,String> RECOVERY_HASHES=Map.ofEntries(
            Map.entry("backend/failure.json","87ba6bbfd5751234b9f07b8255f5fe0ddd7a34b971244638c0a62a11c0fcf952"),
            Map.entry("backend/ledger.log","b3fd94a0c26845e73ac46a9849ff5760a3d81c3ad9a324d5cbc3f3c54162ebbb"),
            Map.entry("receipts/local-backend-failure-cleanup-01.private.json","8032f024e22439d7f1c6663d0e1644476b6eccc22069c257b68c1c5bc72951da"),
            Map.entry("receipts/local-db-role-readonly-01.private.json","1a4069601265be6cb82bb4188a3d8bffb69bdea5d2c1f955bf55acfd579b2289"),
            Map.entry("receipts/root-local-backend-refusal-handoff-01.private.json","9f65f6905ebfabee1abf526395d12abf9141a4e94225650a61def22e5107b7d5"),
            Map.entry("android/local-waiting-reservation-01.private.json","ed86d98fe5c4dfa386d851e8a1e182d6fadfc6e1d576b902a1988198c78537c8"),
            Map.entry("android/connection.private.json","3ecd2bc551933c72b52884f0e77175c7ac8b622216ac77525b2240808f0cc551"),
            Map.entry("recovery-protected-before.private.json","bae525c0fbcc08a1688bf9a41eca4fcd5fa098be2dcdb1a15cd040b74714cdc7"),
            Map.entry("receipts/local-runtime-recovery-human-approval-01.private.json","1f052a5805b63eb85e26b866030a3398e449bd01d7bb9a200bee2f90833cbd4e"));
    private P4ConsumerQaSetup(){}
    public static void main(String[] args){System.exit(run(args,System.out));}
    static int run(String[] args,PrintStream out) {
        if(args.length==0 || Arrays.equals(args,new String[]{"--dry-run"})) {
            out.println("DRY_RUN_ZERO_FILE_CREDENTIAL_DB_NETWORK_IO_CONSUMER_QA");return 0;
        }
        if(args.length!=2 || !args[0].equals("--execute")){out.println("REFUSED_ARGUMENTS");return 2;}
        Path directory=null;boolean startupClaimOwned=false;
        try {
            directory=Path.of(args[1]).toAbsolutePath().normalize();privateDirectory(directory);
            var input=JSON.readTree(privateBytes(directory.resolve("inputs.private.json"),65536));
            String localUser=localDbUsername(input);
            recoveryGate(directory);
            startupClaimOwned=true;
            var approval=JSON.readTree(privateBytes(directory.resolve("approval.json"),4096));
            P4ConsumerQaLedger.validateApproval(approval);
            Path repo=Path.of(input.path("repoRoot").asText()).toRealPath();
            byte[] proposal=Files.readAllBytes(repo.resolve("be/docs/evidence/p4-consumer-qa/bounded-run-proposal-2026-10-18.json"));
            Path prior=repo.resolve("be/var/"+P4ConsumerQaLedger.RUN);
            byte[] terminal=privateBytes(prior.resolve("mobile-window-180/ledger.log"),65536);
            byte[] expected=privateBytes(prior.resolve("mobile-window-180/ready.json"),65536);
            var oldInputs=JSON.readTree(privateBytes(prior.resolve("inputs.json"),65536));
            byte[] oldPlan=Files.readAllBytes(Path.of(oldInputs.path("plan").asText()));
            require(ReadingIdentity.sha(oldPlan).equals(P4LocalComparisonLedger.PLAN_SHA),"OLD_PLAN_CHANGED");
            var plan=JSON.readTree(oldPlan);
            require(oldInputs.path("cloneUrl").asText().endsWith("/tellpal_p4a2_cb5fe81a7ada4cb5adce6ddde1381d09"),"CLONE_CHANGED");
            P3LocalCmsSetup.validateUrl(oldInputs.path("cloneUrl").asText());
            try(var ledger=new P4ConsumerQaLedger(directory.resolve("ledger.log"),proposal,terminal,expected,approval,
                    Clock.systemUTC(),System::nanoTime,false);
                var context=context(directory,prior,oldInputs,plan,ledger,localUser)) {
                var jdbc=context.getBean(JdbcTemplate.class);validateDatabase(jdbc);
                require(Runtime.getRuntime().maxMemory()==plan.path("runtimePreflight").path("parentMaxHeapBytes").longValue(),"SERVING_HEAP_CHANGED");
                context.getBean(com.tellpal.v2.content.application.publication.ReadingImageComparisonProperties.class).limits();
                P4LocalComparisonSetup.isolation(context,94);
                var admission=context.getBean(ReadingImageComparisonApi.class).manifest(94,"tr");
                var previous=JSON.readTree(expected);validateAdmission(previous,admission);
                var ready=new TreeMap<String,Object>();ready.put("proposalSha256",P4ConsumerQaLedger.PROPOSAL_SHA);
                ready.put("runId",P4ConsumerQaLedger.RUN);ready.put("contentId",94);ready.put("identity",admission.identity());ready.put("descriptor",admission.descriptor());
                ready.put("leaseMillis",admission.leaseMillis());ready.put("pid",ProcessHandle.current().pid());
                ready.put("loopbackPort",((WebServerApplicationContext)context).getWebServer().getPort());
                ready.put("servingJavaRuntime",System.getProperty("java.runtime.version"));
                ready.put("servingJavaBinarySha256",ReadingIdentity.sha(Files.readAllBytes(Path.of(System.getProperty("java.home"),"bin","java"))));
                long gateStartedAt=System.currentTimeMillis(),gateStartedNanos=System.nanoTime();
                ready.put("localGateStartedAtEpochMillis",gateStartedAt);
                ready.put("localGateDeadlineEpochMillis",Math.addExact(gateStartedAt,300000));
                P4LocalComparisonSetup.privateJson(directory.resolve("local-ready.json"),ready);
                // Bu kapı medya penceresini açmaz; önceki beş dakikalık beklemeyi uzatmaz.
                while(!Files.exists(directory.resolve("open.request"))) {
                    require(localGateOpen(gateStartedAt,gateStartedNanos,System.currentTimeMillis(),System.nanoTime())
                            && !Files.exists(directory.resolve("close.request")),"LOCAL_GATE_EXPIRED");Thread.sleep(100);
                }
                require(localGateOpen(gateStartedAt,gateStartedNanos,System.currentTimeMillis(),System.nanoTime())
                        && !Files.exists(directory.resolve("close.request")),"LOCAL_GATE_EXPIRED");
                credentialGate(directory,ledger);
                validateDatabase(jdbc);validateAdmission(previous,context.getBean(ReadingImageComparisonApi.class).manifest(94,"tr"));
                require(localGateOpen(gateStartedAt,gateStartedNanos,System.currentTimeMillis(),System.nanoTime())
                        && !Files.exists(directory.resolve("close.request")),"LOCAL_GATE_EXPIRED");
                ledger.openWindow(directory.resolve("window.json"));
                ready.put("windowStartedAtEpochMillis",ledger.started());ready.put("windowDeadlineEpochMillis",ledger.deadline());
                P4LocalComparisonSetup.privateJson(directory.resolve("ready.json"),ready);
                while(ledger.remainingMillis()>0 && !Files.exists(directory.resolve("close.request")))Thread.sleep(100);
            }
            P4LocalComparisonSetup.privateJson(directory.resolve("backend-closed.json"),Map.of("closed",true,"pid",ProcessHandle.current().pid(),"proposalSha256",P4ConsumerQaLedger.PROPOSAL_SHA,"runId",P4ConsumerQaLedger.RUN));
            return 0;
        } catch(Exception failure){
            String code=failure.getMessage()!=null && failure.getMessage().matches("[A-Z0-9_]{1,100}") ? failure.getMessage() : "CONSUMER_QA_FAILED";
            try {
                if(startupClaimOwned) {
                    privateDirectory(directory);
                    P4LocalComparisonSetup.privateJson(directory.resolve("failure.json"),Map.of("proposalSha256",P4ConsumerQaLedger.PROPOSAL_SHA,"status","FAILED_NO_RETRY","code",code));
                }
            } catch(Exception ignored) { }
            out.println("REFUSED_CONSUMER_QA_NO_RETRY_PRIVATE_EVIDENCE");return 2;
        }
    }

    static String localDbUsername(JsonNode input) {
        require(input.path("localDbUsername").isTextual() && input.path("localDbUsername").textValue().equals("gurhankucuk"),
                "EXPLICIT_LOCAL_DB_USER_REQUIRED");
        return input.path("localDbUsername").textValue();
    }

    static JsonNode recoveryApproval() {
        return JSON.valueToTree(Map.ofEntries(Map.entry("proposalSha256",P4ConsumerQaLedger.PROPOSAL_SHA),
                Map.entry("decision","A"),Map.entry("runId",P4ConsumerQaLedger.RUN),Map.entry("qaUid",P4ConsumerQaLedger.UID),
                Map.entry("evidenceHashes",RECOVERY_HASHES),Map.entry("buildName","build-0002"),
                Map.entry("apkSha256","102521e8c77a30784e3f2af185f5e810d539edfb69f76eea8aa4a541a9685166"),
                Map.entry("localDbUsername","gurhankucuk"),Map.entry("additionalQaStarts",1),Map.entry("qaStartsTotalCeiling",3),
                Map.entry("additionalBackendStarts",1),Map.entry("backendStartsTotalCeiling",2),Map.entry("measurementCeilingsChanged",false)));
    }

    static JsonNode startupReservation(String approvalSha,String kind) {
        return JSON.valueToTree(Map.of("proposalSha256",P4ConsumerQaLedger.PROPOSAL_SHA,"runId",P4ConsumerQaLedger.RUN,
                "recoveryApprovalSha256",approvalSha,"kind",kind,"state","RESERVED_NO_AUTOMATIC_RETRY","maxStarts",1,
                "backendDirectory","backend-02","connectionFile","android/connection-03.private.json"));
    }

    static void validateRecoveryLedger(byte[] value) {
        String expected="OPEN_PENDING "+P4ConsumerQaLedger.PROPOSAL_SHA+" "+P4ConsumerQaLedger.TERMINAL_SHA+" 11068\n"
                +"PRIOR_DEBT 14 74711347 13 68374490 47 243641782 8 20 2 69 91 25 31572086\nCLOSED 0 0 0 0 0 0\n";
        require(Arrays.equals(value,expected.getBytes(java.nio.charset.StandardCharsets.UTF_8)),"RECOVERY_PRIOR_SPEND_OR_WINDOW");
    }

    static void recoveryGate(Path directory) throws Exception {
        Path root=directory.getParent();
        for(var entry:RECOVERY_HASHES.entrySet())require(hashFile(root.resolve(entry.getKey())).equals(entry.getValue()),"RECOVERY_EVIDENCE_CHANGED");
        var sealed=JSON.readTree(privateBytes(root.resolve("recovery-protected-before.private.json"),65536));
        var fields=sealed.path("files").fields();
        while(fields.hasNext()) {
            var entry=fields.next();Path relative=Path.of(entry.getKey());
            require(!relative.isAbsolute() && relative.normalize().equals(relative) && !relative.startsWith(".."),"RECOVERY_SEAL_PATH_REFUSED");
            require(hashFile(root.resolve(relative)).equals(entry.getValue().asText()),"RECOVERY_PROTECTED_FILE_CHANGED");
        }
        validateRecoveryLedger(privateBytes(root.resolve("backend/ledger.log"),65536));
        for(String name:List.of("backend-02/failure.json","backend-02/backend-closed.json","backend-02/close.request","android/arm-failure-03.private.json"))
            require(!Files.exists(root.resolve(name)),"RECOVERY_ALREADY_FAILED_OR_CLOSED");
        for(String folder:List.of("backend","backend-02"))for(String name:List.of("login.reservation.private.json","qa-sign-in.ready.json","open.request","window.json","mobile.attempt.json"))
            require(!Files.exists(root.resolve(folder).resolve(name)),"RECOVERY_AFTER_AUTH_FORBIDDEN");
        byte[] approval=privateBytes(root.resolve(RECOVERY_APPROVAL),8192);
        byte[] reservation=privateBytes(directory.resolve("startup.reservation.private.json"),8192);
        String approvalSha=ReadingIdentity.sha(approval);
        validateRecoveryAuthorization(JSON.readTree(approval),JSON.readTree(reservation),approvalSha);
        for(String name:List.of("ledger.log","failure.json","local-ready.json","backend-closed.json","close.request"))
            require(!Files.exists(directory.resolve(name)),"BACKEND_RECOVERY_ALREADY_USED");
        claimStartup(directory,approvalSha,reservation);
    }

    static void claimStartup(Path directory,String approvalSha,byte[] reservation) throws Exception {
        // JVM yeniden çağrılsa da ayrılan backend payı geri açılamaz.
        P4LocalComparisonSetup.privateJson(directory.resolve("startup.claim.private.json"),Map.of("recoveryApprovalSha256",approvalSha,
                "startupReservationSha256",ReadingIdentity.sha(reservation),"pid",ProcessHandle.current().pid()));
    }

    static void validateRecoveryAuthorization(JsonNode approval,JsonNode reservation,String approvalSha) {
        require(approval.equals(recoveryApproval()),"LOCAL_RUNTIME_RECOVERY_APPROVAL_REQUIRED");
        require(reservation.equals(startupReservation(approvalSha,"BACKEND_START")),"RECOVERY_RESERVATION_CHANGED");
    }

    private static String hashFile(Path path) throws Exception {
        require(path.toRealPath().equals(path.toAbsolutePath().normalize()) && Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS),"RECOVERY_FILE_REQUIRED");
        return ReadingIdentity.sha(Files.readAllBytes(path));
    }

    static boolean localGateOpen(long wallStart,long monoStart,long wallNow,long monoNow) {
        long elapsed=monoNow-monoStart;
        return wallNow>=wallStart && wallNow-wallStart<300000 && elapsed>=0 && elapsed<300000000000L;
    }

    static void credentialGate(Path directory,P4ConsumerQaLedger ledger) throws Exception {
        var receipt=JSON.readTree(privateBytes(directory.resolve("qa-sign-in.ready.json"),16384));
        require(receipt.path("runId").asText().equals(P4ConsumerQaLedger.RUN)
                && receipt.path("qaUid").asText().equals(P4ConsumerQaLedger.UID)
                && P4ConsumerQaLedger.exact(receipt.path("expiresAtEpochMillis"),receipt.path("expiresAtEpochMillis").longValue())
                && receipt.path("expiresAtEpochMillis").longValue()>System.currentTimeMillis()+180000,"CREDENTIAL_SCOPE_OR_EXPIRY");
        Path reservation=directory.resolve("login.reservation.private.json");
        if(Files.exists(reservation)) {
            var login=JSON.readTree(privateBytes(reservation,8192));
            require(login.path("proposalSha256").asText().equals(P4ConsumerQaLedger.PROPOSAL_SHA)
                    && login.path("priorLedgerSha256").asText().equals(P4ConsumerQaLedger.TERMINAL_SHA)
                    && P4ConsumerQaLedger.exact(login.path("qaSignInInvocationsTotal"),3)
                    && P4ConsumerQaLedger.exact(login.path("firebaseVerifierReservationsTotal"),21),"LOGIN_RESERVATION_CHANGED");
            require(P4ConsumerQaLedger.exact(receipt.path("qaSignInInvocationsTotal"),3)
                    && P4ConsumerQaLedger.exact(receipt.path("firebaseVerifierReservationsTotal"),21)
                    && receipt.path("proposalSha256").asText().equals(P4ConsumerQaLedger.PROPOSAL_SHA),"LOGIN_RECEIPT_CHANGED");
            ledger.importLoginReservation();
        } else {
            require(P4ConsumerQaLedger.exact(receipt.path("qaSignInInvocationsTotal"),2)
                    && receipt.path("planSha256").asText().equals(P4LocalComparisonLedger.PLAN_SHA),"UNRESERVED_LOGIN_FORBIDDEN");
        }
    }

    private static JsonNode wire(Object value) {
        try{return JSON.readTree(JSON.writeValueAsBytes(value));}catch(Exception failure){throw new IllegalStateException("WIRE_FAILED",failure);}
    }

    static void validateAdmission(JsonNode expected,ReadingImageComparisonApi.Admission admission) {
        require(wire(admission.identity()).equals(expected.path("identity"))
                && wire(admission.descriptor()).equals(expected.path("descriptor")),"RETAINED_LIVE_CHANGED");
    }

    static void validateDatabase(JdbcTemplate jdbc) {
        require("gurhankucuk".equals(jdbc.queryForObject("select current_user",String.class)),"LOCAL_DB_USER_CHANGED");
        require("tellpal_p4a2_cb5fe81a7ada4cb5adce6ddde1381d09".equals(jdbc.queryForObject("select current_database()",String.class))
                && jdbc.queryForObject("select max(version::integer) from flyway_schema_history where success",Integer.class)==37,"V37_CLONE_REQUIRED");
        var row=jdbc.queryForMap("select operations,reserved_bytes,max_operations,max_bytes from reading_comparison_budgets where run_id=?",P4ConsumerQaLedger.RUN);
        require(((Number)row.get("operations")).longValue()==25 && ((Number)row.get("reserved_bytes")).longValue()==31572086
                && ((Number)row.get("max_operations")).longValue()==64 && ((Number)row.get("max_bytes")).longValue()==67108864,"V37_PRIOR_DEBT_CHANGED");
    }

    static ConfigurableApplicationContext context(Path directory,Path prior,JsonNode inputs,JsonNode plan,P4ConsumerQaLedger ledger,String localUser) {
        require(localDbUsername(JSON.valueToTree(Map.of("localDbUsername",localUser))).equals(localUser),"EXPLICIT_LOCAL_DB_USER_REQUIRED");
        var app=new SpringApplication(TellPalApplication.class);
        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>)event->{
            P4LocalComparisonSetup.validateEnvironment(event.getEnvironment(),inputs);
            require(!event.getEnvironment().getProperty("spring.flyway.enabled",Boolean.class,true),"FLYWAY_FORBIDDEN");
            require(localUser.equals(event.getEnvironment().getProperty("spring.datasource.username")),"EXPLICIT_LOCAL_DB_USER_REQUIRED");
        });
        app.addInitializers(boundedInitializer(ledger));
        var arguments=new ArrayList<>(List.of("--spring.profiles.active=local","--spring.config.additional-location=file:"+prior.resolve("application-p4.properties"),
                "--spring.datasource.url="+inputs.path("cloneUrl").asText(),datasourceUsernameArgument(localUser),"--spring.flyway.enabled=false","--spring.main.banner-mode=off",
                "--tellpal.reading-comparison.enabled=true","--tellpal.reading-comparison.content-id=94","--tellpal.reading-comparison.language=tr",
                "--tellpal.reading-comparison.run-id="+P4ConsumerQaLedger.RUN));
        plan.path("delivery").fields().forEachRemaining(entry->{if(Set.of("maxTotalOperations","maxTotalBytes","maxEncodedBytes","maxPixels","maxMemoryBytes","deadlineMillis","leaseMillis").contains(entry.getKey()))
            arguments.add("--tellpal.reading-comparison."+entry.getKey().replaceAll("([A-Z])","-$1").toLowerCase(Locale.ROOT)+"="+entry.getValue().asText());});
        var result=app.run(arguments.toArray(String[]::new));
        require(!result.containsBean(org.springframework.scheduling.config.TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
                && result.getBeansOfType(org.springframework.scheduling.config.ScheduledTaskHolder.class).isEmpty(),"SCHEDULER_FORBIDDEN");
        return result;
    }

    static String datasourceUsernameArgument(String value) {
        return "--spring.datasource.username="+localDbUsername(JSON.valueToTree(Map.of("localDbUsername",value)));
    }

    static ApplicationContextInitializer<ConfigurableApplicationContext> boundedInitializer(P4ConsumerQaLedger ledger) {
        return context->context.addBeanFactoryPostProcessor(factory->{
            var registry=(BeanDefinitionRegistry)factory;
            String name=org.springframework.scheduling.config.TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME;
            if(registry.containsBeanDefinition(name))registry.removeBeanDefinition(name);
            factory.addBeanPostProcessor(new BeanPostProcessor(){
                @Override public Object postProcessAfterInitialization(Object bean,String beanName) {
                    if(bean instanceof AssetStorageClient storage && storage.provider()==StorageProvider.FIREBASE_STORAGE)return new P4ConsumerQaStorage(storage,ledger);
                    if(bean instanceof FirebaseTokenVerifier verifier)return (FirebaseTokenVerifier)token->{ledger.verify();return verifier.verify(token);};
                    if(bean instanceof ReadingImageComparisonTestService) {
                        var proxy=new ProxyFactory(bean);proxy.setProxyTargetClass(true);
                        proxy.addAdvice((MethodInterceptor)invocation->{ledger.request(invocation.getMethod().getName(),invocation.getArguments());return invocation.proceed();});
                        return proxy.getProxy();
                    }
                    return bean;
                }
            });
        });
    }
    static byte[] privateBytes(Path path,int maximum) throws Exception {
        require(path.toRealPath().equals(path.toAbsolutePath().normalize()) && Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)
                && Files.size(path)<=maximum && Files.getPosixFilePermissions(path).equals(PosixFilePermissions.fromString("rw-------")),"PRIVATE_FILE_REQUIRED");
        return Files.readAllBytes(path);
    }
    static void privateDirectory(Path path) throws Exception {
        require(path.toRealPath().equals(path) && path.equals(PRIVATE_ROOT.resolve("backend-02"))
                && Files.getPosixFilePermissions(path).equals(PosixFilePermissions.fromString("rwx------")),"PRIVATE_DIRECTORY_REQUIRED");
    }
    private static void require(boolean value,String code){P4ConsumerQaLedger.require(value,code);}
}
