package com.tellpal.imageexperiment.host

import com.tellpal.imageexperiment.ImageExperimentBootstrap
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.security.Principal
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

object HostedFixture {
    lateinit var certificate: ByteArray
    private fun manifest(): ByteArray = if (!HostRuntime.readerFixture) "{\"synthetic_fixture\":1}".toByteArray()
        else JSONObject(mapOf("entries" to (1..if (HostRuntime.readerBatchId.startsWith("reader_quality_")) 2 else 6)
            .map { index -> mapOf("caseId" to "reader_page_$index",
                "mime" to "image/png", "purposeAcceptanceReceipt" to HostRuntime.readerBatchId) })).toString().toByteArray()
    fun install(certificateFile: File) {
        certificate = certificateFile.readBytes()
        URL.setURLStreamHandlerFactory { protocol -> if (protocol == "https") object : URLStreamHandler() {
            override fun openConnection(url: URL): URLConnection = HostedConnection(url).also { HostRuntime.requests += it }
        } else null }
    }

    fun metadata(): JSONObject = JSONObject().apply {
        val manifest = manifest()
        put("schemaVersion", 1); put("scope", "image-optimization-native-fixture-r1")
        put("batchId", if (HostRuntime.readerFixture) HostRuntime.readerBatchId else "synthetic_batch")
        put("sessionId", "synthetic_session"); put("caseId", if (HostRuntime.readerFixture) "reader_page_1" else "synthetic_case")
        put("launchNonce", "1".repeat(64)); put("deadline", System.currentTimeMillis() + 29_000)
        put("expectedUid", "jLRypTmoBrhhX21HsDJojurcKTC3"); put("expectedAppId", HostRuntime.applicationId)
        put("firebaseProjectId", "synthetic-project"); put("backendEnvironment", "test")
        put("brokerOrigin", "https://fixture.invalid"); put("brokerCertificateDerBase64", encode(certificate))
        put("brokerCertificateSha256", ImageExperimentBootstrap.sha256(certificate))
        put("manifestSha256", "2".repeat(64)); put("publicManifestSha256", ImageExperimentBootstrap.sha256(manifest))
    }

    fun response(path: String): String {
        val metadata = metadata()
        return when {
            path == "/experiment/v1/probe" || path == "/experiment/v1/backend-probe" -> "{}"
            path.startsWith("/experiment/v1/bootstrap/paired") -> JSONObject().apply {
                val key = HostRuntime.pairedPublicKey
                put("paired", key != null)
                if (key != null) {
                    val der = java.util.Base64.getDecoder().decode(key)
                    put("publicKeySha256", ImageExperimentBootstrap.sha256(der))
                    HostRuntime.events += "pairing"
                }
            }.toString()
            path == "/experiment/v1/bootstrap/challenge" -> JSONObject().apply {
                for (field in listOf("scope", "batchId", "sessionId", "launchNonce")) put(field, metadata.get(field))
                put("challenge", "3".repeat(64)); put("expiresAt", System.currentTimeMillis() + 20_000)
            }.toString()
            path == "/experiment/v1/bootstrap/claim" -> JSONObject().apply {
                put("schemaVersion", 1); put("generation", 0)
                for (field in listOf("scope", "batchId", "sessionId", "expectedUid", "backendEnvironment", "brokerOrigin",
                    "brokerCertificateSha256", "manifestSha256", "publicManifestSha256")) put(field, metadata.get(field))
                put("issuedAt", System.currentTimeMillis() - 1_000); put("expiresAt", System.currentTimeMillis() + 60_000)
                put("idToken", "SYNTHETIC_DETACHED_TOKEN"); put("publicManifestBase64", encode(manifest()))
            }.toString()
            else -> error("Unexpected path")
        }
    }
    fun encode(bytes: ByteArray): String = java.util.Base64.getEncoder().encodeToString(bytes)
}

class HostedConnection(url: URL) : HttpsURLConnection(url) {
    val output = ByteArrayOutputStream()
    var disconnectCount = 0
    var responseReads = 0
    override fun connect() {}
    override fun disconnect() { disconnectCount++ }
    override fun usingProxy() = false
    override fun getResponseCode(): Int {
        responseReads++
        if (HostRuntime.networkFailure == url.path) throw HostRuntime.failure
        if (url.path == "/experiment/v1/backend-probe" && HostRuntime.backendProbeClockAdvance != 0L) {
            Thread.sleep(80)
            HostRuntime.elapsedOffset += HostRuntime.backendProbeClockAdvance
        }
        return 200
    }
    override fun getInputStream(): ByteArrayInputStream {
        val text = if (HostRuntime.networkFailure == "invalid-envelope" && url.path.endsWith("/claim")) "{}"
            else if (HostRuntime.networkFailure == "invalid-challenge" && url.path.endsWith("/challenge")) "{}"
            else if (HostRuntime.networkFailure == "invalid-envelope-time" && url.path.endsWith("/claim"))
                JSONObject(HostedFixture.response(url.path)).put("expiresAt", 0).toString()
            else if (HostRuntime.networkFailure == "invalid-envelope-manifest" && url.path.endsWith("/claim"))
                JSONObject(HostedFixture.response(url.path)).put("publicManifestBase64", "e30=").toString()
            else HostedFixture.response(url.path)
        return ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))
    }
    override fun getOutputStream() = output
    override fun getCipherSuite() = "TLS_FAKE_BOUNDARY"
    override fun getLocalCertificates(): Array<Certificate>? = null
    override fun getServerCertificates(): Array<Certificate> = emptyArray()
    override fun getPeerPrincipal(): Principal = Principal { "synthetic" }
    override fun getLocalPrincipal(): Principal? = null
}
