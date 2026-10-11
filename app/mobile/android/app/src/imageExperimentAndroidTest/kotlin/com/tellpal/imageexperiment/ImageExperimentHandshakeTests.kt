package com.tellpal.imageexperiment

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.KeyFactory
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import android.util.Base64

/** Synthetic proof checks run before the launcher can report a pairing key. */
object ImageExperimentHandshakeTests {
    fun run() {
        sharedVector()
        onceOnlyGate()
        explicitLauncherOutcome()
        boundedJsonStructure()
        val key = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val payload = ImageExperimentBootstrap.signedPayload("scope", "batch", "session",
            "0".repeat(64), "1".repeat(64), key.public.encoded)
        require(payload.toString(Charsets.US_ASCII).split('\n').size == 8)
        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(key.private); update(payload)
        }.sign()
        require(Signature.getInstance("SHA256withECDSA").apply {
            initVerify(key.public); update(payload)
        }.verify(signature))
        val replaced = payload.copyOf().apply { this[0] = 'x'.code.toByte() }
        require(!Signature.getInstance("SHA256withECDSA").apply {
            initVerify(key.public); update(replaced)
        }.verify(signature))
        require(runCatching {
            ImageExperimentBootstrap.signedPayload("scope\nother", "batch", "session",
                "0".repeat(64), "1".repeat(64), key.public.encoded)
        }.isFailure)
    }

    private fun onceOnlyGate() {
        val gate = ImageExperimentAdmissionGate()
        val winners = java.util.concurrent.atomic.AtomicInteger(0)
        val threads = List(16) { Thread { if (gate.take()) winners.incrementAndGet() } }
        threads.forEach { it.start() }
        threads.forEach { it.join(1000); require(!it.isAlive) }
        require(winners.get() == 1 && !gate.take())
        require(gate.revoke() && !gate.revoke() && gate.isRevoked() && !gate.take())
        val early = ImageExperimentAdmissionGate()
        require(early.revoke() && !early.take())
    }

    private fun explicitLauncherOutcome() {
        fun prepared() = ImageExperimentLauncherOutcome().apply {
            markSessionHandedOff()
            markLedgerStored()
        }
        val positive = prepared()
        require(!positive.isSettled())
        positive.complete("settled", false)
        require(positive.isSettled())
        positive.complete("refused", true)
        require(positive.isSettled())
        for (result in listOf(null, "refused", "other")) {
            val missingPositive = prepared()
            missingPositive.complete(result, false)
            require(!missingPositive.isSettled())
            missingPositive.complete("settled", false)
            require(!missingPositive.isSettled())
        }
        val noHandoff = ImageExperimentLauncherOutcome().apply { markLedgerStored() }
        noHandoff.complete("settled", false)
        require(!noHandoff.isSettled())
        val noLedger = ImageExperimentLauncherOutcome().apply { markSessionHandedOff() }
        noLedger.complete("settled", false)
        require(!noLedger.isSettled())
        val earlyRevoked = prepared()
        earlyRevoked.complete("settled", true)
        require(!earlyRevoked.isSettled())
    }

    private fun boundedJsonStructure() {
        val atLimit = "{\"value\":" + "[".repeat(31) + "0" + "]".repeat(31) + "}"
        ImageExperimentJson.checkStructure(atLimit)
        val escaped = """{"value":"quoted \" braces { } [ ] and slash \\","nested":[{}]}"""
        ImageExperimentJson.checkStructure(escaped)
        val deep = "{\"value\":" + "[".repeat(4096) + "0" + "]".repeat(4096) + "}"
        require(deep.toByteArray(Charsets.UTF_8).size < 16_384)
        val refused = listOf(deep, "{\"value\":" + "[".repeat(32) + "0" + "]".repeat(32) + "}",
            "{]", "{\"unterminated}")
        for (raw in refused) {
            val failure = runCatching { ImageExperimentJson.checkStructure(raw) }.exceptionOrNull()
            require(failure is IllegalArgumentException && failure.message == "NATIVE_JSON_REFUSED")
        }
        require(runCatching { ImageExperimentJson.checkStructure(escaped, 4) }.isFailure)
    }

    fun runLedgerValidation() {
        val deep = "{\"value\":" + "[".repeat(4096) + "0" + "]".repeat(4096) + "}"
        require(runCatching { ImageExperimentJson.parseObject(deep) }.exceptionOrNull()?.message == "NATIVE_JSON_REFUSED")
        val escaped = """{"value":"quoted \" braces { } [ ] and slash \\"}"""
        require(ImageExperimentJson.parseObject(escaped).getString("value").contains("{ } [ ]"))
        val valid = """{"schemaVersion":1,"records":[{"event":"status_request","observerOverheadMicroseconds":2,"dpr":2.0,"fit":"contain","firstRasterizedTargetFrame":null}],"droppedCount":0}"""
        require(ImageExperimentLedger.validate(valid).isNotEmpty())
        require(runCatching { ImageExperimentLedger.validate(valid.replace("status_request", "detached_secret")) }.isFailure)
        require(runCatching { ImageExperimentLedger.validate("""{"schemaVersion":1,"records":[{"idToken":"detached_secret"}],"droppedCount":0}""") }.isFailure)
        val duplicate = """{"schemaVersion":1,"records":[{"event":"detached_secret","event":"status"}],"droppedCount":0}"""
        require(!ImageExperimentLedger.validate(duplicate).toString(Charsets.UTF_8).contains("detached_secret"))
    }

    private fun sharedVector() {
        val der = Base64.decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEaxfR8uEsQkf4vOblY6RA8ncDfYEt6zOg9KE5RdiYwpZP40Li/hp/m47n60p8D54WK84zV2sxXs7LtkBoN79R9Q==", Base64.NO_WRAP)
        val signature = Base64.decode("MEQCIAQ13SrX9he8nb5rlo7aWhB9DEcacbzNKMS6wr8xog4sAiAlRPElM4ZFIt2C6DDdv3fOhR2FFPx+kJ7ETRADBHnDow==", Base64.NO_WRAP)
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der))
        require(ImageExperimentBootstrap.sha256(der) == "5cd252fb0ce8932436faf8ccd1040981b89ee4ad6b9fe9e2a2b7e71aacb27cd3")
        fun payload(batch: String = "vector_batch", challenge: String = "2".repeat(64)) =
            ImageExperimentBootstrap.signedPayload("image-optimization-native-fixture-r1", batch, "vector_session",
                "1".repeat(64), challenge, der)
        fun verifies(value: ByteArray, proof: ByteArray = signature) = runCatching {
            Signature.getInstance("SHA256withECDSA").apply { initVerify(key); update(value) }.verify(proof)
        }.getOrDefault(false)
        require(verifies(payload()))
        require(!verifies(payload(challenge = "2".repeat(63) + "3")))
        require(!verifies(payload(batch = "changed_batch")))
        require(!verifies(payload(), byteArrayOf(0x30, 0x01)))
        val replacement = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        require(!Signature.getInstance("SHA256withECDSA").apply { initVerify(replacement.public); update(payload()) }.verify(signature))
    }
}
