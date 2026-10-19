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

/** Aynı V37 borcuyla yalnız P4b5 ölçümünü açan test kompozisyonu. */
public final class P4b5ConsumerQaSetup {
    private static final ObjectMapper JSON=new ObjectMapper();
    static final Path PRIVATE_ROOT=Path.of("/Volumes/MAC_EK/tellpal-qa-private/p4b5-consumer-measurement-20261019-d80884d");
    static final Path HISTORICAL_TERMINAL=Path.of("/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb/backend-02/ledger.log");
    private P4b5ConsumerQaSetup(){}
    public static void main(String[] args){System.exit(run(args,System.out));}
    static int run(String[] args,PrintStream out) {
        if(args.length==0 || Arrays.equals(args,new String[]{"--dry-run"})) {
            out.println("DRY_RUN_ZERO_FILE_CREDENTIAL_DB_NETWORK_IO_P4B5");return 0;
        }
        if(args.length!=2 || !args[0].equals("--execute")){out.println("REFUSED_ARGUMENTS");return 2;}
        Path directory=null;boolean claimed=false;OwnedContext owner=null;P4b5ConsumerQaLedger finalLedger=null;
        try {
            directory=Path.of(args[1]).toAbsolutePath().normalize();privateDirectory(directory);
            require(!closedAttempt(PRIVATE_ROOT),"ATTEMPT_CLOSED_NO_RETRY");
            var approval=JSON.readTree(privateBytes(directory.resolve("approval.json"),4096));
            P4b5ConsumerQaLedger.validateApproval(approval);
            var reservation=JSON.readTree(privateBytes(directory.resolve("startup.reservation.private.json"),8192));
            require(reservation.equals(JSON.valueToTree(Map.of("proposalSha256",P4b5ConsumerQaLedger.PROPOSAL_SHA,
                    "measurementId",P4b5ConsumerQaLedger.MEASUREMENT,"budgetRunId",P4b5ConsumerQaLedger.RUN,
                    "kind","BACKEND_START","state","RESERVED_NO_AUTOMATIC_RETRY","maxStarts",1))),"STARTUP_RESERVATION_CHANGED");
            var input=JSON.readTree(privateBytes(directory.resolve("inputs.private.json"),4194304));
            P4LocalComparisonSetup.privateJson(directory.resolve("startup.claim.private.json"),Map.of("proposalSha256",P4b5ConsumerQaLedger.PROPOSAL_SHA,
                    "measurementId",P4b5ConsumerQaLedger.MEASUREMENT,"pid",ProcessHandle.current().pid(),
                    "runBindingSha256",input.path("runBindingSha256").asText()));claimed=true;
            String localUser=localDbUsername(input);
            Path repo=Path.of(input.path("repoRoot").asText()).toRealPath();
            require(repo.equals(PRIVATE_ROOT.resolve("source")),"PRIVATE_SOURCE_ROOT_REQUIRED");
            var bindingLinks=validateReviewedRuntime(PRIVATE_ROOT,repo,input,runtimeEntries(),
                    Path.of(System.getProperty("java.home"),"bin","java"),Runtime.getRuntime().maxMemory());
            byte[] proposal=Files.readAllBytes(repo.resolve("be/docs/evidence/p4b5-consumer-measurement/bounded-run-proposal-2026-10-19.json"));
            byte[] terminal=privateBytes(HISTORICAL_TERMINAL,65536);
            byte[] expected=Files.readAllBytes(repo.resolve("be/src/test/resources/p4/consumer-retained-ready.json"));
            Path prior=Path.of(input.path("historicalStateRoot").asText()).toRealPath();
            require(prior.getFileName().toString().equals(P4b5ConsumerQaLedger.RUN),"HISTORICAL_STATE_SCOPE");
            var protectedBefore=JSON.readTree(privateBytes(PRIVATE_ROOT.resolve("receipts/protected-before.private.json"),4194304));
            for(String file:List.of("inputs.json","application-p4.properties"))require(ReadingIdentity.sha(privateBytes(prior.resolve(file),65536))
                    .equals(protectedBefore.path("oldRunFiles").path("be/var/"+P4b5ConsumerQaLedger.RUN+"/"+file).asText()),"HISTORICAL_STATE_CHANGED");
            var oldInputs=JSON.readTree(privateBytes(prior.resolve("inputs.json"),65536));
            byte[] oldPlan=Files.readAllBytes(Path.of(oldInputs.path("plan").asText()));
            require(ReadingIdentity.sha(oldPlan).equals(P4LocalComparisonLedger.PLAN_SHA),"OLD_PLAN_CHANGED");
            var plan=JSON.readTree(oldPlan);
            require(oldInputs.path("cloneUrl").asText().endsWith("/tellpal_p4a2_cb5fe81a7ada4cb5adce6ddde1381d09"),"DATABASE_CHANGED");
            P3LocalCmsSetup.validateUrl(oldInputs.path("cloneUrl").asText());
            // SDK ve Spring başlamadan aynı yerel DB salt okunur doğrulanır.
            try(var connection=java.sql.DriverManager.getConnection(oldInputs.path("cloneUrl").asText(),localUser,"")) {
                connection.setReadOnly(true);
                validateDatabase(new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true)));
            }
            var ledger=new P4b5ConsumerQaLedger(directory.resolve("ledger.log"),proposal,terminal,expected,approval,Clock.systemUTC(),System::nanoTime,false);
            finalLedger=ledger;
            try(ledger;var serving=new OwnedContext(context(directory,prior,oldInputs,plan,ledger,localUser))) {
                owner=serving;var context=serving.value;
                var jdbc=context.getBean(JdbcTemplate.class);validateDatabase(jdbc);
                context.getBean(com.tellpal.v2.content.application.publication.ReadingImageComparisonProperties.class).limits();
                P4LocalComparisonSetup.isolation(context,94);
                var previous=JSON.readTree(expected);
                var admission=context.getBean(ReadingImageComparisonApi.class).manifest(94,"tr");validateAdmission(previous,admission);
                var ready=new TreeMap<String,Object>();ready.put("proposalSha256",P4b5ConsumerQaLedger.PROPOSAL_SHA);
                ready.put("measurementId",P4b5ConsumerQaLedger.MEASUREMENT);ready.put("budgetRunId",P4b5ConsumerQaLedger.RUN);
                bindingLinks.fields().forEachRemaining(entry->ready.put(entry.getKey(),entry.getValue().asText()));
                ready.put("contentId",94);ready.put("identity",admission.identity());ready.put("descriptor",admission.descriptor());
                ready.put("leaseMillis",admission.leaseMillis());ready.put("pid",ProcessHandle.current().pid());
                ready.put("loopbackPort",((WebServerApplicationContext)context).getWebServer().getPort());
                ready.put("servingJavaRuntime",System.getProperty("java.runtime.version"));
                ready.put("servingJavaBinarySha256",ReadingIdentity.sha(Files.readAllBytes(Path.of(System.getProperty("java.home"),"bin","java"))));
                long wall=System.currentTimeMillis(),mono=System.nanoTime();
                var localGate=new LocalGate(wall,mono);
                ready.put("localGateStartedAtEpochMillis",wall);ready.put("localGateDeadlineEpochMillis",Math.addExact(wall,300000));
                P4LocalComparisonSetup.privateJson(directory.resolve("local-ready.json"),ready);
                while(!Files.exists(directory.resolve("open.request"))) {
                    require(localGate.open(System.currentTimeMillis(),System.nanoTime()) && !Files.exists(directory.resolve("close.request")),"LOCAL_GATE_EXPIRED");Thread.sleep(100);
                }
                require(localGate.open(System.currentTimeMillis(),System.nanoTime()) && !Files.exists(directory.resolve("close.request")),"LOCAL_GATE_EXPIRED");
                final Path windowDirectory=directory;
                openCredentialWindow(windowDirectory,ledger,ready,()->{
                    validateDatabase(jdbc);
                    validateAdmission(previous,context.getBean(ReadingImageComparisonApi.class).manifest(94,"tr"));
                },()->localGate.open(System.currentTimeMillis(),System.nanoTime()) && !Files.exists(windowDirectory.resolve("close.request")));
                while(ledger.remainingMillis()>0 && !Files.exists(directory.resolve("close.request")))Thread.sleep(100);

            }
            // Context ve ledger kapandıktan sonra nihai DB sayıları yeni salt okunur bağlantıyla alınır.
            try(var connection=java.sql.DriverManager.getConnection(oldInputs.path("cloneUrl").asText(),localUser,"")) {
                connection.setReadOnly(true);
                var jdbc=new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true));
                var outcome=successfulOutcome(directory,jdbc,ledger,owner.closed,bindingLinks);
                P4LocalComparisonSetup.privateJson(directory.resolve("backend-closed.json"),outcome);
            }
            return 0;
        } catch(Exception failure) {
            String code=failure.getMessage()!=null && failure.getMessage().matches("[A-Z0-9_]{1,100}") ? failure.getMessage() : "P4B5_FAILED";
            try {if(claimed)P4LocalComparisonSetup.privateJson(directory.resolve("failure.json"),Map.of("proposalSha256",P4b5ConsumerQaLedger.PROPOSAL_SHA,
                    "measurementId",P4b5ConsumerQaLedger.MEASUREMENT,"status","FAILED_NO_RETRY","code",code));
                if(owner!=null)P4LocalComparisonSetup.privateJson(directory.resolve("backend-closed.json"),Map.of("closed",owner.closed,"drainVerified",owner.closed && finalLedger!=null && finalLedger.isClosed(),"pid",ProcessHandle.current().pid(),
                    "proposalSha256",P4b5ConsumerQaLedger.PROPOSAL_SHA,"measurementId",P4b5ConsumerQaLedger.MEASUREMENT,"budgetRunId",P4b5ConsumerQaLedger.RUN,"outcome","FAILED_NO_RETRY"));
            }catch(Exception ignored){}
            out.println("REFUSED_P4B5_NO_RETRY_PRIVATE_EVIDENCE");return 2;
        }
    }

    static void validateBindings(Path repo,JsonNode input) throws Exception {
        var bindings=input.path("sourceBindings");require(bindings.isObject() && bindings.size()>0,"SOURCE_BINDINGS_REQUIRED");
        var fields=bindings.fields();while(fields.hasNext()) {
            var entry=fields.next();Path relative=Path.of(entry.getKey());
            require(!relative.isAbsolute() && relative.normalize().equals(relative) && !relative.startsWith(".."),"SOURCE_PATH_REFUSED");
            require(ReadingIdentity.sha(Files.readAllBytes(repo.resolve(relative))).equals(entry.getValue().asText()),"SOURCE_DRIFT");
        }
        var classpath=input.path("classpathBindings");require(classpath.isObject() && classpath.size()>0,"CLASSPATH_BINDINGS_REQUIRED");
        var paths=classpath.fields();while(paths.hasNext()) {
            var entry=paths.next();Path path=Path.of(entry.getKey());
            require(path.isAbsolute() && path.toRealPath().equals(path) && Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)
                    && ReadingIdentity.sha(Files.readAllBytes(path)).equals(entry.getValue().asText()),"CLASSPATH_DRIFT");
        }
    }

    static Path numberedReceipt(JsonNode input,String field,String prefix) {
        String name=input.path(field).asText();
        require(name.matches(prefix+"-[0-9]{4}\\.private\\.json"),"NUMBERED_RECEIPT_REQUIRED");
        return PRIVATE_ROOT.resolve("receipts").resolve(name);
    }

    static boolean closedAttempt(Path root) {
        return Files.exists(root.resolve("terminal.private.json")) || Files.exists(root.resolve("cleanup.claim.private.json"))
                || Files.exists(root.resolve("cleanup.private.json")) || Files.exists(root.resolve("backend/close.request"))
                || Files.exists(root.resolve("backend/backend-closed.json")) || Files.exists(root.resolve("backend/failure.json"));
    }

    static void scope(JsonNode value) {
        require(value.path("proposalSha256").asText().equals(P4b5ConsumerQaLedger.PROPOSAL_SHA)
                && value.path("measurementId").asText().equals(P4b5ConsumerQaLedger.MEASUREMENT)
                && value.path("budgetRunId").asText().equals(P4b5ConsumerQaLedger.RUN),"REVIEWED_SCOPE_CHANGED");
    }

    static Path receipt(Path root,String name,String prefix) {
        require(name.matches(prefix+"-[0-9]{4}\\.private\\.json"),"NUMBERED_RECEIPT_REQUIRED");
        return root.resolve("receipts").resolve(name);
    }

    static JsonNode boundReceipt(Path root,JsonNode link,String field,String prefix) throws Exception {
        byte[] bytes=privateBytes(receipt(root,link.path(field+"File").asText(),prefix),4194304);
        require(ReadingIdentity.sha(bytes).equals(link.path(field+"Sha256").asText()),"REVIEWED_RECEIPT_CHANGED");
        var result=JSON.readTree(bytes);scope(result);return result;
    }

    static List<Path> runtimeEntries() {
        String raw=System.getProperty("java.class.path");
        var values=raw.split(java.util.regex.Pattern.quote(System.getProperty("path.separator")),-1);
        require(Arrays.stream(values).noneMatch(String::isBlank),"CLASSPATH_EMPTY_ENTRY");
        return Arrays.stream(values).map(value->Path.of(value).toAbsolutePath().normalize()).toList();
    }

    static JsonNode validateReviewedRuntime(Path root,Path repo,JsonNode input,List<Path> actualEntries,Path javaBinary,long heap) throws Exception {
        require(!closedAttempt(root),"ATTEMPT_CLOSED_NO_RETRY");
        require(heap==1073741824L,"SERVING_HEAP_CHANGED");
        require(repo.equals(root.resolve("source")),"PRIVATE_SOURCE_ROOT_REQUIRED");
        Path runPath=receipt(root,input.path("runBindingFile").asText(),"run-binding");
        byte[] runBytes=privateBytes(runPath,4194304);
        String runSha=ReadingIdentity.sha(runBytes);
        require(runSha.equals(input.path("runBindingSha256").asText()),"REVIEWED_RUN_CHANGED");
        var run=JSON.readTree(runBytes);scope(run);
        var source=boundReceipt(root,run,"sourceBinding","source-binding");
        var classpath=boundReceipt(root,run,"classpathBinding","classpath-binding");
        var selected=boundReceipt(root,run,"selectedBuild","selected-build");
        require(input.path("sourceBindingFile").equals(run.path("sourceBindingFile"))
                && input.path("sourceBindingSha256").equals(run.path("sourceBindingSha256"))
                && source.path("files").equals(input.path("sourceBindings")),"APPROVED_SOURCE_INPUT_CHANGED");
        require(classpath.path("entries").equals(input.path("classpathEntries"))
                && classpath.path("files").equals(input.path("classpathBindings")),"APPROVED_CLASSPATH_INPUT_CHANGED");
        require(ReadingIdentity.sha(Files.readAllBytes(javaBinary)).equals(classpath.path("javaBinarySha256").asText()),"APPROVED_JAVA_BINARY_CHANGED");
        validateBindings(repo,input);
        validateRuntimeClasspath(actualEntries,classpath);
        String buildName=selected.path("buildName").asText();require(buildName.matches("build-[0-9]{4}"),"BUILD_NAME_REQUIRED");
        Path buildRoot=root.resolve("android/builds").resolve(buildName);
        var build=JSON.readTree(privateBytes(buildRoot.resolve("build.private.json"),65536));scope(build);
        require(build.path("sourceBindingSha256").equals(run.path("sourceBindingSha256"))
                && build.path("entry").asText().equals("test_driver/reading_p4b5_qa_entry.dart")
                && build.path("package").asText().equals("com.tellpal.tellpal_mobile_poc.readingconsumerqa")
                && build.path("mode").asText().equals("debug")
                && selected.path("apkSha256").equals(build.path("apkSha256"))
                && ReadingIdentity.sha(Files.readAllBytes(buildRoot.resolve("consumer.apk"))).equals(build.path("apkSha256").asText()),"APPROVED_APK_CHANGED");
        var claim=JSON.readTree(privateBytes(root.resolve("backend-start.claim.private.json"),8192));scope(claim);
        require(claim.path("runBindingSha256").asText().equals(runSha)
                && claim.path("state").asText().equals("RESERVED_NO_AUTOMATIC_RETRY"),"APPROVED_BACKEND_CLAIM_CHANGED");
        String generation=runPath.getFileName().toString().substring("run-binding-".length(),"run-binding-".length()+4);
        var reviewers=new HashSet<String>();
        for(int number=1;number<=3;number++) {
            var review=JSON.readTree(privateBytes(root.resolve("reviews/pre-run-"+generation+"-"+String.format(Locale.ROOT,"%02d",number)+".private.json"),65536));scope(review);
            require(review.path("sourceBindingSha256").equals(run.path("sourceBindingSha256"))
                    && review.path("runBindingSha256").asText().equals(runSha)
                    && review.path("status").asText().equals("PASS") && P4b5ConsumerQaLedger.exact(review.path("unresolvedFindings"),0)
                    && review.path("reviewerId").isTextual() && !review.path("reviewerId").textValue().isBlank(),"THREE_PRE_RUN_REVIEWS_REQUIRED");
            reviewers.add(review.path("reviewerId").textValue());
        }
        require(reviewers.size()==3,"INDEPENDENT_REVIEWERS_REQUIRED");
        var links=JSON.createObjectNode();links.put("runBindingFile",runPath.getFileName().toString());links.put("runBindingSha256",runSha);
        for(String field:List.of("sourceBindingFile","sourceBindingSha256","classpathBindingFile","classpathBindingSha256","selectedBuildFile","selectedBuildSha256"))links.set(field,run.path(field));
        links.set("apkSha256",selected.path("apkSha256"));links.set("servingJavaBinarySha256",classpath.path("javaBinarySha256"));
        return links;
    }

    static void validateRuntimeClasspath(List<Path> entries,JsonNode approved) throws Exception {
        require(JSON.valueToTree(entries.stream().map(Path::toString).toList()).equals(approved.path("entries")),"CLASSPATH_ENTRIES_CHANGED");
        var actual=new TreeMap<String,String>();
        for(Path entry:entries) {
            require(entry.toRealPath().equals(entry),"CLASSPATH_PATH_REFUSED");
            if(Files.isDirectory(entry)) {
                try(var paths=Files.walk(entry)) {
                    for(Path file:paths.filter(Files::isRegularFile).toList()) {
                        require(file.toRealPath().equals(file),"CLASSPATH_PATH_REFUSED");
                        actual.put(file.toString(),ReadingIdentity.sha(Files.readAllBytes(file)));
                    }
                }
            } else actual.put(entry.toString(),ReadingIdentity.sha(Files.readAllBytes(entry)));
        }
        require(JSON.valueToTree(actual).equals(approved.path("files")),"CLASSPATH_CLOSURE_CHANGED");
    }

    static final class OwnedContext implements AutoCloseable {
        final ConfigurableApplicationContext value;boolean closed;
        OwnedContext(ConfigurableApplicationContext value){this.value=value;}
        @Override public void close(){value.close();closed=true;}
    }

    static Map<String,Object> successfulOutcome(Path directory,JdbcTemplate jdbc,P4b5ConsumerQaLedger ledger,boolean contextClosed,JsonNode links) throws Exception {
        require(contextClosed && ledger.isClosed() && !ledger.isFailed() && !Files.exists(directory.resolve("failure.json")),"BACKEND_NOT_SUCCESSFULLY_DRAINED");
        require(ledger.completeMeasurement(),"BACKEND_MEASUREMENT_INCOMPLETE");
        var budget=validateFinalDatabase(jdbc,ledger.deliveryOperations(),ledger.reservedBytes());
        byte[] terminal=Files.readAllBytes(directory.resolve("ledger.log"));
        String last=new String(terminal,java.nio.charset.StandardCharsets.UTF_8).strip().lines().reduce((a,b)->b).orElse("");
        require(last.equals(ledger.terminalLine()),"TERMINAL_LEDGER_CHANGED");
        var result=new TreeMap<String,Object>();result.put("proposalSha256",P4b5ConsumerQaLedger.PROPOSAL_SHA);result.put("measurementId",P4b5ConsumerQaLedger.MEASUREMENT);result.put("budgetRunId",P4b5ConsumerQaLedger.RUN);
        result.put("closed",true);result.put("drainVerified",true);result.put("outcome","SUCCESS");result.put("pid",ProcessHandle.current().pid());
        result.put("bindings",links);result.put("finalBudget",budget);result.put("terminalLedgerSha256",ReadingIdentity.sha(terminal));result.put("terminalLedgerLine",last);
        result.put("deliveryOperations",ledger.deliveryOperations());result.put("reservedBytes",ledger.reservedBytes());return result;
    }

    @FunctionalInterface interface WindowPrerequisites {void verify() throws Exception;}

    static void openCredentialWindow(Path directory,P4b5ConsumerQaLedger ledger,Map<String,Object> ready,
            WindowPrerequisites prerequisites,java.util.function.BooleanSupplier localGateOpen) throws Exception {
        credentialGate(directory,ledger);
        prerequisites.verify();
        require(localGateOpen.getAsBoolean(),"LOCAL_GATE_EXPIRED");
        ledger.openWindow(directory.resolve("window.json"));
        ready.put("windowStartedAtEpochMillis",ledger.started());ready.put("windowDeadlineEpochMillis",ledger.deadline());
        P4LocalComparisonSetup.privateJson(directory.resolve("ready.json"),ready);
    }

    static void credentialGate(Path directory,P4b5ConsumerQaLedger ledger) throws Exception {
        var receipt=JSON.readTree(privateBytes(directory.resolve("qa-sign-in.ready.json"),16384));
        var reservation=JSON.readTree(privateBytes(directory.resolve("login.reservation.private.json"),8192));
        for(var value:List.of(receipt,reservation))require(value.path("proposalSha256").asText().equals(P4b5ConsumerQaLedger.PROPOSAL_SHA)
                && value.path("measurementId").asText().equals(P4b5ConsumerQaLedger.MEASUREMENT)
                && value.path("budgetRunId").asText().equals(P4b5ConsumerQaLedger.RUN)
                && P4b5ConsumerQaLedger.exact(value.path("qaSignInInvocationsTotal"),4)
                && P4b5ConsumerQaLedger.exact(value.path("firebaseVerifierReservationsTotal"),32),"LOGIN_SCOPE_OR_RESERVATION");
        require(receipt.path("qaUid").asText().equals(P4b5ConsumerQaLedger.UID)
                && P4b5ConsumerQaLedger.exact(receipt.path("expiresAtEpochMillis"),receipt.path("expiresAtEpochMillis").longValue())
                && receipt.path("expiresAtEpochMillis").longValue()>System.currentTimeMillis()+180000,"CREDENTIAL_SCOPE_OR_EXPIRY");
        for(String key:List.of("accountLookupReserved","tokenExchangeReserved","loginVerifierReserved"))
            require(P4b5ConsumerQaLedger.exact(reservation.path(key),1),"LOGIN_RESERVATION_CHANGED");
        Path tokenPath=directory.toRealPath().resolve("id-token.private");
        require(receipt.path("tokenFile").isTextual() && receipt.path("tokenFile").textValue().equals(tokenPath.toString()),"TOKEN_FILE_SCOPE");
        String token=new String(privateBytes(tokenPath,16384),java.nio.charset.StandardCharsets.UTF_8).strip();
        require(token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"),"TOKEN_FORMAT");
        String[] tokenParts=token.split("\\.");
        var header=JSON.readTree(java.util.Base64.getUrlDecoder().decode(tokenParts[0]));
        require(header.isObject() && header.path("alg").asText().equals("RS256")
                && (!header.has("typ") || header.path("typ").asText().equals("JWT"))
                && java.util.Base64.getUrlDecoder().decode(tokenParts[2]).length>0,"TOKEN_FORMAT");
        var claims=JSON.readTree(java.util.Base64.getUrlDecoder().decode(tokenParts[1]));
        long now=System.currentTimeMillis();
        require(claims.path("aud").asText().equals("tellpal-ee0dd")
                && claims.path("iss").asText().equals("https://securetoken.google.com/tellpal-ee0dd")
                && claims.path("sub").asText().equals(P4b5ConsumerQaLedger.UID)
                && (!claims.has("user_id") || claims.path("user_id").asText().equals(P4b5ConsumerQaLedger.UID))
                && claims.path("exp").isIntegralNumber() && claims.path("exp").canConvertToLong()
                && claims.path("exp").longValue()>Math.floorDiv(now+180000,1000)
                && claims.path("exp").longValue()<=Long.MAX_VALUE/1000
                && receipt.path("expiresAtEpochMillis").longValue()==claims.path("exp").longValue()*1000
                && claims.path("iat").isIntegralNumber() && claims.path("iat").canConvertToLong()
                && claims.path("iat").longValue()>0 && claims.path("iat").longValue()<=Math.floorDiv(now,1000),"TOKEN_SCOPE_OR_EXPIRY");
        ledger.importLoginReservation();
    }

    static Map<String,Object> validateFinalDatabase(JdbcTemplate jdbc,int operations,long bytes) {
        require(operations>=0 && operations<=25 && bytes>=0 && bytes<=13170884,"ADDITIONAL_DELIVERY_QUOTA");
        var row=jdbc.queryForMap("select operations,reserved_bytes,max_operations,max_bytes from reading_comparison_budgets where run_id=?",P4b5ConsumerQaLedger.RUN);
        require(((Number)row.get("operations")).longValue()==39+operations && ((Number)row.get("reserved_bytes")).longValue()==44742970+bytes
                && ((Number)row.get("max_operations")).longValue()==64 && ((Number)row.get("max_bytes")).longValue()==67108864,"V37_RECONCILIATION_FAILED");
        return row;
    }
    static String localDbUsername(JsonNode input) {
        require(input.path("localDbUsername").isTextual() && input.path("localDbUsername").textValue().equals("gurhankucuk"),
                "EXPLICIT_LOCAL_DB_USER_REQUIRED");
        return input.path("localDbUsername").textValue();
    }

    static final class LocalGate {
        private final long wallStart,monoStart;
        private long lastWall,lastMono;
        private boolean failed;
        LocalGate(long wall,long mono){wallStart=lastWall=wall;monoStart=lastMono=mono;}
        boolean open(long wall,long mono) {
            if(failed || wall<lastWall || mono<lastMono || !localGateOpen(wallStart,monoStart,wall,mono)){failed=true;return false;}
            lastWall=wall;lastMono=mono;return true;
        }
    }

    static boolean localGateOpen(long wallStart,long monoStart,long wallNow,long monoNow) {
        long elapsed=monoNow-monoStart;
        return wallNow>=wallStart && wallNow-wallStart<300000 && elapsed>=0 && elapsed<300000000000L;
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
        var row=jdbc.queryForMap("select operations,reserved_bytes,max_operations,max_bytes from reading_comparison_budgets where run_id=?",P4b5ConsumerQaLedger.RUN);
        require(((Number)row.get("operations")).longValue()==39 && ((Number)row.get("reserved_bytes")).longValue()==44742970
                && ((Number)row.get("max_operations")).longValue()==64 && ((Number)row.get("max_bytes")).longValue()==67108864,"V37_PRIOR_DEBT_CHANGED");
    }

    static ConfigurableApplicationContext context(Path directory,Path prior,JsonNode inputs,JsonNode plan,P4b5ConsumerQaLedger ledger,String localUser) {
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
                "--tellpal.reading-comparison.run-id="+P4b5ConsumerQaLedger.RUN));
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

    static ApplicationContextInitializer<ConfigurableApplicationContext> boundedInitializer(P4b5ConsumerQaLedger ledger) {
        return context->context.addBeanFactoryPostProcessor(factory->{
            var registry=(BeanDefinitionRegistry)factory;
            String name=org.springframework.scheduling.config.TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME;
            if(registry.containsBeanDefinition(name))registry.removeBeanDefinition(name);
            factory.addBeanPostProcessor(new BeanPostProcessor(){
                @Override public Object postProcessAfterInitialization(Object bean,String beanName) {
                    if(bean instanceof AssetStorageClient storage && storage.provider()==StorageProvider.FIREBASE_STORAGE)return new P4b5ConsumerQaStorage(storage,ledger);
                    if(bean instanceof FirebaseTokenVerifier verifier)return (FirebaseTokenVerifier)token->{try {ledger.verify();return verifier.verify(token);}catch(RuntimeException failure){ledger.fail("AUTH_FAILED");throw failure;}};
                    if(bean instanceof ReadingImageComparisonTestService) {
                        var proxy=new ProxyFactory(bean);proxy.setProxyTargetClass(true);
                        proxy.addAdvice((MethodInterceptor)invocation->{try {ledger.enterRequest(invocation.getMethod().getName(),invocation.getArguments());try{return invocation.proceed();}finally{ledger.exitRequest();}} catch(Throwable failure){ledger.fail("HTTP_FAILED");throw failure;}});
                        return proxy.getProxy();
                    }
                    return bean;
                }
            });
        });
    }
    static byte[] privateBytes(Path path,int maximum) throws Exception {return P4ConsumerQaSetup.privateBytes(path,maximum);}
    static void privateDirectory(Path path) throws Exception {
        require(path.toRealPath().equals(path) && path.equals(PRIVATE_ROOT.resolve("backend"))
                && Files.getPosixFilePermissions(path).equals(PosixFilePermissions.fromString("rwx------")),"PRIVATE_DIRECTORY_REQUIRED");
    }
    private static void require(boolean value,String code){P4b5ConsumerQaLedger.require(value,code);}
}
