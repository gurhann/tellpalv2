package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Sabit gerçek makbuz yalnız yerel fixture'dır; test storage/auth çağrısı yapmaz. */
class P4ExistingComparisonLedgerTest {
    @TempDir Path directory;
    Path source() throws Exception {
        Path parent=directory.toRealPath().resolve(P4LocalComparisonLedger.RUN);Files.createDirectories(parent.resolve("mobile-continuation"));
        Path source=parent.resolve("ledger.log");
        try(var input=getClass().getResourceAsStream("/p4/existing-comparison-ledger.txt")) {Files.write(source,input.readAllBytes());}
        return source;
    }
    long fixtureOpen() {
        try(var input=getClass().getResourceAsStream("/p4/existing-comparison-ledger.txt")) {
            String first=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().findFirst().orElseThrow();
            return Long.parseLong(first.split(" ")[2]);
        } catch(Exception failure) {throw new IllegalStateException("FROZEN_FIXTURE_OPEN",failure);}
    }
    Clock clock(long elapsed) {return Clock.fixed(Instant.ofEpochMilli(fixtureOpen()+elapsed),ZoneOffset.UTC);}
    @Test void copiedLedgerKeepsOriginalDeadlineAndSpentReadAuthCountersAndCannotBeRestarted() throws Exception {
        Path source=source();byte[] original=Files.readAllBytes(source);Path child=source.resolveSibling("mobile-continuation").resolve("ledger.log");
        try(var ledger=P4LocalComparisonLedger.resumeMedia(source,child,clock(500000))) {
            assertThat(ledger.remainingMillis()).isEqualTo(400000);
            for(int i=0;i<23;i++)ledger.read(P4LocalComparisonLedger.PREFIX+"/existing.png",1);
            assertThatThrownBy(()->ledger.read(P4LocalComparisonLedger.PREFIX+"/existing.png",1)).hasMessage("READ_BUDGET");
            for(int i=0;i<60;i++)ledger.verify();
            assertThatThrownBy(ledger::verify).hasMessage("AUTH_BUDGET");
            assertThat(Files.readString(child)).contains("READ_RESERVED 42 215362418 ","READ_RESERVED 64 215362440 ","AUTH_RESERVED 5\n","AUTH_RESERVED 64\n");
        }
        assertThat(Files.readAllBytes(source)).isEqualTo(original);
        assertThat(Files.readAllBytes(child)).startsWith(original);
        assertThatThrownBy(()->P4LocalComparisonLedger.resumeMedia(source,child,clock(500000))).isInstanceOf(FileAlreadyExistsException.class);
    }
    @Test void allProductionAndPhaseMutationPathsAreClosed() throws Exception {
        Path source=source();Path child=source.resolveSibling("mobile-continuation").resolve("ledger.log");
        try(var ledger=P4LocalComparisonLedger.resumeMedia(source,child,clock(1))) {
            assertThatThrownBy(()->ledger.phase(5)).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
            assertThatThrownBy(()->ledger.create(P4LocalComparisonLedger.PREFIX+"/new",1)).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
            assertThatThrownBy(()->ledger.observe(P4LocalComparisonLedger.PREFIX+"/existing")).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
            assertThatThrownBy(()->ledger.once("RUN_EXACT_JOB")).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
            assertThatThrownBy(()->ledger.scope(94)).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
            assertThatThrownBy(()->ledger.record("FIXTURE_LIVE","94")).hasMessage("MEDIA_CONTINUATION_READ_AUTH_ONLY");
        }
    }
    @Test void expiredOrChangedOriginalIsRejectedBeforeChildCreation() throws Exception {
        assertThat(P4LocalComparisonLedger.ORIGINAL_OPEN).isEqualTo(fixtureOpen());
        Path source=source();Path child=source.resolveSibling("mobile-continuation").resolve("ledger.log");
        assertThatThrownBy(()->P4LocalComparisonLedger.resumeMedia(source,child,clock(900000))).hasMessage("RUN_WINDOW_OR_FAILED");
        assertThat(child).doesNotExist();
        Files.writeString(source,"changed",StandardOpenOption.APPEND);
        assertThatThrownBy(()->P4LocalComparisonLedger.resumeMedia(source,child,clock(1))).hasMessage("CONTINUATION_HASH");
        assertThat(child).doesNotExist();
    }
    @Test void existingEntryDefaultsToZeroIo() {
        var output=new java.io.ByteArrayOutputStream();
        assertThat(P4ExistingComparisonSetup.run(new String[0],new java.io.PrintStream(output))).isZero();
        assertThat(output.toString()).contains("ZERO_FILE_CREDENTIAL_DB_NETWORK_IO_EXISTING_ONLY");
    }
}
