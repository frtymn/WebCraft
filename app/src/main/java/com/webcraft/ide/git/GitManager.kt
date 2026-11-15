package com.webcraft.ide.git

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Git state and operations.
 *
 * NOTE ON IMPLEMENTATION:
 * Full Git plumbing (packfiles, refs, protocol v2) is out of scope for
 * this release. WebCraft v0.1.0 provides:
 *   - .gitignore-aware change detection (working tree vs snapshot)
 *   - Local snapshot "commits" stored on disk
 *   - Branch name tracking
 *   - Export/import of snapshots via ZIP
 *   - A GitHub auth flow entry point (token entry UI) that stores the
 *     token in Android EncryptedSharedPreferences-equivalent storage.
 *
 * Push/pull/clone over HTTPS requires the smart HTTP protocol which is
 * nontrivial to implement from scratch; it is documented as a limitation
 * for v0.1.0 and will land in v0.2.0 via JGit or a bundled libgit2.
 */
class GitManager(context: Context) {

    private val appContext = context.applicationContext
    private val gitRoot = File(appContext.filesDir, "git").apply { mkdirs() }
    private val securePrefs: SharedPreferences = appContext
        .getSharedPreferences("webcraft_secure", Context.MODE_PRIVATE)

    // ── Token storage (encrypted at rest) ───────────────────────────

    /**
     * Store the GitHub token encrypted with AES-256-GCM.
     * The key is derived from a random 32-byte value stored in prefs
     * (this is obfuscation-grade, not hardware-backed; see docs/security.md).
     */
    fun storeToken(token: String) {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key: SecretKey = deriveKey(salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(token.toByteArray())
        val blob = iv + encrypted
        val b64 = Base64.encodeToString(blob, Base64.NO_WRAP)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        securePrefs.edit()
            .putString("gh_token", b64)
            .putString("gh_salt", saltB64)
            .apply()
    }

    fun retrieveToken(): String? {
        val b64 = securePrefs.getString("gh_token", null) ?: return null
        val saltB64 = securePrefs.getString("gh_salt", null) ?: return null
        return try {
            val blob = Base64.decode(b64, Base64.NO_WRAP)
            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            val key: SecretKey = deriveKey(salt)
            val iv = blob.copyOfRange(0, 12)
            val ct = blob.copyOfRange(12, blob.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(ct))
        } catch (_: Exception) { null }
    }

    fun clearToken() {
        securePrefs.edit().remove("gh_token").remove("gh_salt").apply()
    }

    private fun deriveKey(salt: ByteArray): SecretKey {
        // PBKDF2-style derivation from a fixed app secret + salt
        val secret = "WebCraft-git-auth-v1".toByteArray()
        val spec = javax.crypto.spec.PBEKeySpec(
            String(secret).toCharArray(), salt, 65536, 256
        )
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // ── Working-tree state ──────────────────────────────────────────

    data class Commit(val id: String, val message: String, val timestamp: Long, val files: Map<String, String>)
    data class Branch(val name: String, val isCurrent: Boolean)

    private fun dirFor(projectRoot: String): File =
        File(File(projectRoot).parentFile, ".webcraft-git/${File(projectRoot).name}").apply { mkdirs() }

    fun hasGit(projectRoot: String): Boolean = dirFor(projectRoot).exists()

    fun init(projectRoot: String, defaultBranch: String = "main") {
        val dir = dirFor(projectRoot)
        dir.mkdirs()
        File(dir, "HEAD").writeText(defaultBranch)
        File(dir, "branch").writeText(defaultBranch)
    }

    fun currentBranch(projectRoot: String): String =
        File(dirFor(projectRoot), "branch").takeIf { it.exists() }?.readText()?.trim() ?: "main"

    fun switchBranch(projectRoot: String, name: String) {
        val dir = dirFor(projectRoot)
        // Save current branch tip
        val cur = currentBranch(projectRoot)
        if (cur != name) {
            val head = File(dir, "HEAD").readText().trim()
            File(dir, "refs/$head").parentFile?.mkdirs()
            File(dir, "refs/$head").writeText(head)
        }
        File(dir, "branch").writeText(name)
        File(dir, "HEAD").writeText(name)
    }

    fun listBranches(projectRoot: String): List<Branch> {
        val dir = dirFor(projectRoot)
        val cur = currentBranch(projectRoot)
        val branches = mutableSetOf(cur)
        File(dir, "refs").takeIf { it.exists() }?.listFiles()?.forEach { branches.add(it.name) }
        return branches.map { Branch(it, it == cur) }
    }

    /** Commit the current state of `files` with a message. */
    fun commit(projectRoot: String, message: String, files: Map<String, String>): Commit {
        val dir = dirFor(projectRoot)
        val id = System.currentTimeMillis().toString(16) +
            java.security.MessageDigest.getInstance("SHA-256")
                .digest((message + files.hashCode()).toByteArray())
                .take(4).joinToString("") { "%02x".format(it) }
        val ts = System.currentTimeMillis()
        val commitDir = File(dir, "commits/$id").apply { mkdirs() }
        File(commitDir, "meta.json").writeText(
            """{"id":"$id","message":"${message.replace("\"", "\\\"")}","timestamp":$ts}"""
        )
        val filesDir = File(commitDir, "files").apply { mkdirs() }
        files.forEach { (path, content) ->
            val f = File(filesDir, path.replace('/', '_'))
            f.parentFile?.mkdirs()
            f.writeText(content)
        }
        // Update branch tip
        File(dir, "refs/${currentBranch(projectRoot)}").parentFile?.mkdirs()
        File(dir, "refs/${currentBranch(projectRoot)}").writeText(id)
        return Commit(id, message, ts, files)
    }

    fun log(projectRoot: String): List<Commit> {
        val dir = dirFor(projectRoot)
        val commitsDir = File(dir, "commits")
        if (!commitsDir.exists()) return emptyList()
        return commitsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { c ->
                val meta = File(c, "meta.json")
                if (!meta.exists()) return@mapNotNull null
                val json = meta.readText()
                val id = Regex(""""id":"([^"]+)"""").find(json)?.groupValues?.get(1) ?: return@mapNotNull null
                val msg = Regex(""""message":"((?:[^"\\]|\\.)*)"""").find(json)?.groupValues?.get(1)
                    ?.replace("\\\"", "\"") ?: ""
                val ts = Regex(""""timestamp":(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 0
                Commit(id, msg, ts, emptyMap())
            }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    /** Diff two file maps. Returns per-path status. */
    fun diff(before: Map<String, String>, after: Map<String, String>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        for ((k, v) in after) {
            if (k !in before) out[k] = "added"
            else if (before[k] != v) out[k] = "modified"
        }
        for (k in before.keys) if (k !in after) out[k] = "deleted"
        return out
    }

    /** Return files that should be ignored per a .gitignore-style list. */
    fun filterIgnored(files: Map<String, String>, ignorePatterns: List<String>): Map<String, String> {
        if (ignorePatterns.isEmpty()) return files
        val patterns = ignorePatterns.filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.trim().removeSuffix("/") }
        return files.filter { (path, _) ->
            val name = path.substringAfterLast('/')
            patterns.none { p ->
                path == p || path.startsWith("$p/") ||
                name == p || name.endsWith(if (p.startsWith("*")) p.substring(1) else p) && p.startsWith("*")
            }
        }
    }

    fun setRemote(projectRoot: String, remoteUrl: String) {
        val dir = dirFor(projectRoot)
        File(dir, "remote").writeText(remoteUrl)
    }

    fun getRemote(projectRoot: String): String? =
        File(dirFor(projectRoot), "remote").takeIf { it.exists() }?.readText()?.trim()
}
