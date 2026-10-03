package com.hayduck.notemate.inference

import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ProvisionedLocalModelTest {
    @Test
    fun onlyBoundedPrivateModelWithExpectedDigestIsAccepted() = runTest {
        val directory = Files.createTempDirectory("synthetic-model-test").toFile()
        try {
            val privateRoot = directory.resolve("no-backup").also { it.mkdir() }
            val file = privateRoot.resolve("synthetic.litertlm")
            file.writeText("synthetic model bytes")
            val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                .joinToString("") { "%02x".format(it) }
            assertEquals(
                file.canonicalFile, ProvisionedLocalModel(privateRoot, file, digest).verify(64),
            )
            assertNull(ProvisionedLocalModel(privateRoot, file, digest).verify(1))
            assertNull(ProvisionedLocalModel(privateRoot, file, "0".repeat(64)).verify(64))
            val external = directory.resolve("external.litertlm").also {
                it.writeBytes(file.readBytes())
            }
            assertNull(ProvisionedLocalModel(privateRoot, external, digest).verify(64))
            val linked = privateRoot.resolve("linked.litertlm")
            Files.createSymbolicLink(linked.toPath(), external.toPath())
            assertNull(ProvisionedLocalModel(privateRoot, linked, digest).verify(64))
        } finally {
            directory.deleteRecursively()
        }
    }
}
