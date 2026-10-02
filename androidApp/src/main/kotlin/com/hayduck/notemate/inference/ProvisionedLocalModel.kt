package com.hayduck.notemate.inference

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** A separately provisioned, immutable model under app-private no-backup storage. */
internal class ProvisionedLocalModel(
    private val noBackupDirectory: File,
    private val modelFile: File,
    expectedSha256: String,
) {
    private val expectedDigest = expectedSha256.lowercase()

    init {
        require(expectedDigest.matches(Regex("[a-f0-9]{64}"))) { "Invalid model digest." }
    }

    suspend fun verify(maxBytes: Long): File? {
        val root = noBackupDirectory.canonicalFile
        val file = modelFile.canonicalFile
        if (!file.path.startsWith(root.path + File.separator) ||
            file.extension != "litertlm" || !file.isFile || !file.canRead() ||
            file.length() !in 1..maxBytes) return null
        val digest = MessageDigest.getInstance("SHA-256")
        var readBytes = 0L
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = stream.read(buffer)
                if (count < 0) break
                readBytes += count
                if (readBytes > maxBytes) return null
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return file.takeIf { actual == expectedDigest }
    }

    override fun toString(): String = "ProvisionedLocalModel([REDACTED])"
}
