package com.tellpal.imageexperiment

import android.os.SystemClock
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URI
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** Shared admission state prevents replay or admission after teardown. */
class ImageExperimentAdmissionGate {
    private val state = java.util.concurrent.atomic.AtomicInteger(0)
    fun take(): Boolean = state.compareAndSet(0, 1)
    fun revoke(): Boolean = state.getAndSet(2) != 2
    fun isRevoked(): Boolean = state.get() == 2
}

/** Bounds nesting before Android's recursive JSON parser sees any untrusted string. */
object ImageExperimentJson {
    fun checkStructure(raw: String, maximumBytes: Int = 65_536) {
        fun refuse(): Nothing = throw IllegalArgumentException("NATIVE_JSON_REFUSED")
        if (raw.toByteArray(Charsets.UTF_8).size !in 1..maximumBytes) refuse()
        val stack = CharArray(32)
        var depth = 0
        var quoted = false
        var escaped = false
        for (character in raw) {
            if (quoted) {
                if (escaped) escaped = false
                else when (character) {
                    '\\' -> escaped = true
                    '"' -> quoted = false
                }
            } else when (character) {
                '"' -> quoted = true
                '{', '[' -> {
                    if (depth == stack.size) refuse()
                    stack[depth++] = character
                }
                '}', ']' -> {
                    if (depth == 0 || stack[--depth] != if (character == '}') '{' else '[') refuse()
                }
            }
        }
        if (quoted || escaped || depth != 0) refuse()
    }

    fun parseObject(raw: String, maximumBytes: Int = 65_536): JSONObject {
        checkStructure(raw, maximumBytes)
        return JSONObject(raw)
    }
}

/** A closed process is successful only after the app explicitly reports its verified fixture result. */
class ImageExperimentLauncherOutcome {
    private var handedOff = false
    private var ledgerStored = false
    private var completed = false
    private var settled = false

    @Synchronized fun markSessionHandedOff() { if (!completed) handedOff = true }
    @Synchronized fun markLedgerStored() { if (!completed) ledgerStored = true }
    @Synchronized fun complete(requestedResult: String?, alreadyRevoked: Boolean) {
        if (completed) return
        settled = requestedResult == "settled" && handedOff && ledgerStored && !alreadyRevoked
        completed = true
    }
    @Synchronized fun isSettled(): Boolean = completed && settled
}

/** Finite in-memory bootstrap; every request validates the exact TLS leaf before HTTP. */
class ImageExperimentBootstrap(rawMetadata: String?, applicationId: String) {
    val launcherOutcome = ImageExperimentLauncherOutcome()
    private val metadata = parseMetadata(rawMetadata, applicationId)
    private val publicMetadata = metadata.keys().asSequence().associateWith { metadata.get(it) }
    @Volatile private var key: KeyPair? = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
    val publicKeySpkiBase64: String = Base64.encodeToString(key!!.public.encoded, Base64.NO_WRAP)
    private val gate = ImageExperimentAdmissionGate()
    private val reachabilityQualified = java.util.concurrent.atomic.AtomicBoolean(false)
    val isReachabilityQualified: Boolean get() = reachabilityQualified.get()
    val isRevoked: Boolean get() = gate.isRevoked()
    private val completion = AtomicReference<((Map<String, Any>?) -> Unit)?>(null)
    private val connection = AtomicReference<HttpsURLConnection?>(null)
    private val executor = Executors.newSingleThreadExecutor()
    private val startedAt = SystemClock.elapsedRealtime()
    private val certificate = CertificateFactory.getInstance("X.509").generateCertificate(
        ByteArrayInputStream(Base64.decode(metadata.getString("brokerCertificateDerBase64"), Base64.NO_WRAP))
    ) as X509Certificate
    private val socketFactory = pinnedContext(certificate, metadata.getString("brokerCertificateSha256")).socketFactory

    fun takeSession(callback: (Map<String, Any>?) -> Unit) {
        if (!gate.take()) { callback(null); return }
        completion.set(callback)
        if (gate.isRevoked()) { completion.getAndSet(null)?.invoke(null); return }
        try {
            executor.execute {
                var result: Map<String, Any>? = null
                try {
                    request("GET", "/experiment/v1/probe")
                    request("GET", "/experiment/v1/backend-probe")
                    reachabilityQualified.set(true)
                    awaitPairing()
                    val challenge = ImageExperimentJson.parseObject(request("POST", "/experiment/v1/bootstrap/challenge", publicProof()))
                    for (field in listOf("scope", "batchId", "sessionId", "launchNonce")) {
                        require(challenge.getString(field) == metadata.getString(field))
                    }
                    val value = challenge.getString("challenge")
                    val challengeExpiry = challenge.get("expiresAt")
                    require((challengeExpiry is Long || challengeExpiry is Int) && HEX.matches(value) &&
                        challenge.getLong("expiresAt") - System.currentTimeMillis() in 1..30_000)
                    val proof = publicProof().put("challenge", value)
                    val retained = key ?: error("REFUSED")
                    val signer = Signature.getInstance("SHA256withECDSA").apply {
                        initSign(retained.private)
                        update(signedPayload(metadata.getString("scope"), metadata.getString("batchId"),
                            metadata.getString("sessionId"), metadata.getString("launchNonce"), value,
                            retained.public.encoded))
                    }
                    val signature = signer.sign()
                    require(signature.size <= 128)
                    proof.put("signatureBase64", Base64.encodeToString(signature, Base64.NO_WRAP))
                    val envelope = request("POST", "/experiment/v1/bootstrap/claim", proof)
                    validateEnvelope(envelope)
                    checkAlive()
                    result = mapOf("envelope" to envelope, "metadata" to publicMetadata)
                } catch (_: Exception) {
                    revoke()
                } finally {
                    key = null
                    connection.getAndSet(null)?.disconnect()
                }
                completion.getAndSet(null)?.invoke(if (gate.isRevoked()) null else result)
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            completion.getAndSet(null)?.invoke(null)
        }
    }

    fun revoke() {
        if (gate.revoke()) {
            key = null
            connection.getAndSet(null)?.disconnect()
            completion.getAndSet(null)?.invoke(null)
            executor.shutdownNow()
        }
    }

    private fun publicProof() = JSONObject().apply {
        for (field in listOf("scope", "batchId", "sessionId", "launchNonce")) put(field, metadata.getString(field))
        put("publicKeySpkiBase64", publicKeySpkiBase64)
    }

    private fun awaitPairing() {
        val digest = sha256(Base64.decode(publicKeySpkiBase64, Base64.NO_WRAP))
        while (true) {
            checkAlive()
            val reply = ImageExperimentJson.parseObject(request("GET", "/experiment/v1/bootstrap/paired?sessionId=" +
                metadata.getString("sessionId") + "&launchNonce=" + metadata.getString("launchNonce")))
            if (reply.optBoolean("paired", false)) {
                require(reply.getString("publicKeySha256") == digest)
                return
            }
            Thread.sleep(100)
        }
    }

    private fun checkAlive() {
        require(!gate.isRevoked() && SystemClock.elapsedRealtime() - startedAt < 30_000 &&
            System.currentTimeMillis() < metadata.getLong("deadline"))
    }

    private fun request(method: String, path: String, body: JSONObject? = null): String {
        checkAlive()
        val opened = (URI(metadata.getString("brokerOrigin") + path).toURL().openConnection() as HttpsURLConnection)
        connection.set(opened)
        try {
            opened.sslSocketFactory = socketFactory
            // The platform's normal hostname verifier is intentionally retained.
            opened.instanceFollowRedirects = false
            opened.useCaches = false
            val remaining = minOf(10_000L, 30_000L - (SystemClock.elapsedRealtime() - startedAt),
                metadata.getLong("deadline") - System.currentTimeMillis()).toInt()
            require(remaining > 0)
            opened.connectTimeout = remaining
            opened.readTimeout = remaining
            opened.requestMethod = method
            opened.setRequestProperty("X-Experiment-Session", metadata.getString("sessionId"))
            opened.setRequestProperty("X-Experiment-Launch-Nonce", metadata.getString("launchNonce"))
            if (body != null) {
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                require(bytes.size <= 4096)
                opened.doOutput = true
                opened.setRequestProperty("Content-Type", "application/json")
                opened.setFixedLengthStreamingMode(bytes.size)
                opened.outputStream.use { it.write(bytes) }
            }
            require(opened.responseCode == 200)
            val bytes = opened.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    checkAlive()
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 65_536)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } finally {
            connection.compareAndSet(opened, null)
            opened.disconnect()
        }
    }

    private fun validateEnvelope(raw: String) {
        val envelope = ImageExperimentJson.parseObject(raw)
        require(envelope.getInt("schemaVersion") == 1 && envelope.getLong("generation") >= 0)
        for (field in listOf("scope", "batchId", "sessionId", "expectedUid", "backendEnvironment", "brokerOrigin",
            "brokerCertificateSha256", "manifestSha256", "publicManifestSha256")) {
            require(envelope.getString(field) == metadata.getString(field))
        }
        val now = System.currentTimeMillis()
        val issued = envelope.getLong("issuedAt")
        val expires = envelope.getLong("expiresAt")
        require(issued <= now && expires > now && expires - issued in 1..180_000 &&
            envelope.getString("idToken").toByteArray(Charsets.UTF_8).size in 1..8192)
        val manifest = Base64.decode(envelope.getString("publicManifestBase64"), Base64.NO_WRAP)
        require(sha256(manifest) == metadata.getString("publicManifestSha256"))
    }

    companion object {
        private val ID = Regex("[A-Za-z0-9_-]{1,64}")
        private val HEX = Regex("[a-f0-9]{64}")
        private val FIELDS = setOf("schemaVersion", "scope", "batchId", "sessionId", "caseId", "launchNonce", "deadline",
            "expectedUid", "expectedAppId", "firebaseProjectId", "backendEnvironment", "brokerOrigin",
            "brokerCertificateDerBase64", "brokerCertificateSha256", "manifestSha256", "publicManifestSha256")

        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

        fun signedPayload(scope: String, batch: String, session: String, nonce: String,
            challenge: String, publicKey: ByteArray): ByteArray {
            require(ID.matches(scope) && ID.matches(batch) && ID.matches(session) &&
                HEX.matches(nonce) && HEX.matches(challenge) && publicKey.size in 1..256)
            return "tellpal-native-fixture-v1\n$scope\n$batch\n$session\n$nonce\n$challenge\n${sha256(publicKey)}\n"
                .toByteArray(Charsets.US_ASCII)
        }

        private fun parseMetadata(raw: String?, applicationId: String): JSONObject {
            require(raw != null && raw.toByteArray(Charsets.UTF_8).size in 1..16_384)
            val data = ImageExperimentJson.parseObject(raw, 16_384)
            require(data.keys().asSequence().toSet() == FIELDS && data.get("schemaVersion") == 1)
            require(data.getString("scope") == "image-optimization-native-fixture-r1")
            for (field in listOf("scope", "batchId", "sessionId", "caseId", "expectedUid")) require(ID.matches(data.getString(field)))
            for (field in listOf("launchNonce", "brokerCertificateSha256", "manifestSha256", "publicManifestSha256")) {
                require(HEX.matches(data.getString(field)))
            }
            require(data.getString("expectedUid") == "jLRypTmoBrhhX21HsDJojurcKTC3")
            require(data.getString("expectedAppId") == applicationId && applicationId in setOf(
                "com.tellpal.tellpal_mobile_poc.imageexperiment.profile",
                "com.tellpal.tellpal_mobile_poc.imageexperiment.release"))
            require(data.getString("backendEnvironment") in setOf("local", "test"))
            require(Regex("[a-z][a-z0-9-]{4,62}").matches(data.getString("firebaseProjectId")))
            val origin = URI(data.getString("brokerOrigin"))
            require(origin.scheme == "https" && origin.host != null && origin.rawUserInfo == null &&
                origin.rawPath.isNullOrEmpty() && origin.rawQuery == null && origin.rawFragment == null &&
                (origin.port == -1 || origin.port in 1..65535))
            val deadline = data.get("deadline")
            require(deadline is Long || deadline is Int)
            require(data.getLong("deadline") - System.currentTimeMillis() in 1..30_000)
            val cert = Base64.decode(data.getString("brokerCertificateDerBase64"), Base64.NO_WRAP)
            require(cert.size in 1..8192 && sha256(cert) == data.getString("brokerCertificateSha256"))
            return data
        }

        private fun pinnedContext(certificate: X509Certificate, pin: String): SSLContext {
            certificate.checkValidity()
            val anchors = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setCertificateEntry("experiment-leaf", certificate)
            }
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(anchors) }
            val evaluator = factory.trustManagers.filterIsInstance<X509TrustManager>().single()
            val pinned = object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = evaluator.acceptedIssuers
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                    throw java.security.cert.CertificateException("REFUSED")
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    require(chain.isNotEmpty() && sha256(chain[0].encoded) == pin)
                    chain[0].checkValidity()
                    evaluator.checkServerTrusted(chain, authType)
                }
            }
            return SSLContext.getInstance("TLS").apply { init(null, arrayOf(pinned), null) }
        }
    }
}
