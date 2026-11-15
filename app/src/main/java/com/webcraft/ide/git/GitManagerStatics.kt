package com.webcraft.ide.git

/**
 * Static (non-Android) helpers for testing GitManager logic on the JVM.
 */
object GitManagerStatics {
    fun diffMaps(before: Map<String, String>, after: Map<String, String>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        for ((k, v) in after) {
            if (k !in before) out[k] = "added"
            else if (before[k] != v) out[k] = "modified"
        }
        for (k in before.keys) if (k !in after) out[k] = "deleted"
        return out
    }

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
}
