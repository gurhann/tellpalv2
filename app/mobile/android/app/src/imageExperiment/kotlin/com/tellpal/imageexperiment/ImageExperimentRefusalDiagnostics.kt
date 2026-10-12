package com.tellpal.imageexperiment

import java.util.concurrent.atomic.AtomicReference

/** Fixed experiment-only categories; no failure or launcher input is retained. */
enum class ImageExperimentRefusalStage(val code: String) {
    signingIdentity("NATIVE_SIGNING_IDENTITY"),
    selfTest("NATIVE_SELF_TEST"),
    selfTestVector("NATIVE_SELF_TEST_VECTOR"),
    selfTestGate("NATIVE_SELF_TEST_GATE"),
    selfTestOutcome("NATIVE_SELF_TEST_OUTCOME"),
    selfTestJson("NATIVE_SELF_TEST_JSON"),
    selfTestProof("NATIVE_SELF_TEST_PROOF"),
    selfTestLedger("NATIVE_SELF_TEST_LEDGER"),
    launcherMetadata("NATIVE_LAUNCHER_METADATA"),
    activityStart("NATIVE_ACTIVITY_START"),
    activityBootstrap("NATIVE_ACTIVITY_BOOTSTRAP"),
    reachability("NATIVE_REACHABILITY"),
    pairingReport("NATIVE_PAIRING_REPORT"),
    settlement("NATIVE_SETTLEMENT"),
    metadata("NATIVE_METADATA"),
    keyGeneration("NATIVE_KEY_GENERATION"),
    certificate("NATIVE_CERTIFICATE"),
    trustContext("NATIVE_TRUST_CONTEXT"),
    dispatch("NATIVE_DISPATCH"),
    probe("NATIVE_PROBE"),
    backendProbe("NATIVE_BACKEND_PROBE"),
    pairing("NATIVE_PAIRING"),
    challenge("NATIVE_CHALLENGE"),
    proof("NATIVE_PROOF"),
    claim("NATIVE_CLAIM"),
    envelopeFields("NATIVE_ENVELOPE_FIELDS"),
    envelopeTimeToken("NATIVE_ENVELOPE_TIME_TOKEN"),
    envelopeManifest("NATIVE_ENVELOPE_MANIFEST"),
    handoff("NATIVE_HANDOFF"),
    dartNativeHandoff("DART_NATIVE_HANDOFF"),
    dartNativeResponse("DART_NATIVE_RESPONSE"),
    dartConfiguration("DART_CONFIGURATION"),
    dartMode("DART_MODE"),
    dartComposition("DART_COMPOSITION"),
    compositionRegistration("DART_COMPOSITION_REGISTRATION"),
    compositionCache("DART_COMPOSITION_CACHE"),
    compositionStatus("DART_COMPOSITION_STATUS"),
    statusAdmission("DART_STATUS_ADMISSION"),
    statusClient("DART_STATUS_CLIENT"),
    statusValidation("DART_STATUS_VALIDATION"),
    clientPreconditions("DART_CLIENT_PRECONDITIONS"),
    clientConstruction("DART_CLIENT_CONSTRUCTION"),
    trustContextDart("DART_TRUST_CONTEXT"),
    certificateImport("DART_CERTIFICATE_IMPORT"),
    certificateImportDerFailPemPass("DART_CERT_IMPORT_DER_FAIL_PEM_PASS"),
    certificateImportDerPassPemFail("DART_CERT_IMPORT_DER_PASS_PEM_FAIL"),
    certificateImportBothFail("DART_CERT_IMPORT_BOTH_FAIL"),
    certificateImportBothPass("DART_CERT_IMPORT_BOTH_PASS"),
    certificateImportUnavailable("DART_CERT_IMPORT_UNAVAILABLE"),
    requestPreconditions("DART_REQUEST_PRECONDITIONS"),
    tlsConnect("DART_TLS_CONNECT"),
    tlsHandshake("DART_TLS_HANDSHAKE"),
    tlsPinValidity("DART_TLS_PIN_VALIDITY"),
    outgoingRequest("DART_OUTGOING_REQUEST"),
    responseValidation("DART_RESPONSE_VALIDATION"),
    responseBody("DART_RESPONSE_BODY"),
    configurationEnvelope("DART_CONFIG_ENVELOPE"),
    configurationFields("DART_CONFIG_FIELDS"),
    configurationIdentity("DART_CONFIG_IDENTITY"),
    configurationIds("DART_CONFIG_IDS"),
    configurationLauncher("DART_CONFIG_LAUNCHER"),
    configurationTime("DART_CONFIG_TIME"),
    configurationEnvironment("DART_CONFIG_ENVIRONMENT"),
    configurationOrigin("DART_CONFIG_ORIGIN"),
    configurationApp("DART_CONFIG_APP"),
    configurationHashes("DART_CONFIG_HASHES"),
    configurationCertificate("DART_CONFIG_CERTIFICATE"),
    configurationToken("DART_CONFIG_TOKEN"),
    configurationClaims("DART_CONFIG_CLAIMS"),
    configurationManifestHash("DART_CONFIG_MANIFEST_HASH"),
    configurationManifest("DART_CONFIG_MANIFEST"),
    configurationBindings("DART_CONFIG_BINDINGS"),
    configurationCapacity("DART_CONFIG_CAPACITY"),
    configurationFixture("DART_CONFIG_FIXTURE"),
    configurationGeometry("DART_CONFIG_GEOMETRY"),
    configurationSelection("DART_CONFIG_SELECTION")
}

/** One bounded stage is shared by the owned instrumentation, Activity and bootstrap. */
class ImageExperimentRefusalDiagnostics {
    private val first = AtomicReference<ImageExperimentRefusalStage?>(null)
    val firstStage: ImageExperimentRefusalStage? get() = first.get()

    fun record(stage: ImageExperimentRefusalStage): Boolean = first.compareAndSet(null, stage)

    fun reportDart(value: Any?): Boolean {
        if (value !is String || value.length !in 1..64 || value.any { it == '\r' || it == '\n' || it == '\u0000' }) return false
        val stage = ImageExperimentRefusalStage.entries.firstOrNull { it.code.startsWith("DART_") && it.code == value }
            ?: return false
        record(stage)
        return true
    }
}

/** Passive observers default to silence and preserve the exact original exception. */
object ImageExperimentRefusalObservation {
    fun report(stage: ImageExperimentRefusalStage, observer: ((ImageExperimentRefusalStage) -> Unit)?) {
        try { observer?.invoke(stage) } catch (_: Throwable) { }
    }

    fun <T> run(stage: ImageExperimentRefusalStage, observer: ((ImageExperimentRefusalStage) -> Unit)? = null,
        operation: () -> T): T = try {
        operation()
    } catch (failure: Exception) {
        report(stage, observer)
        throw failure
    }
}
