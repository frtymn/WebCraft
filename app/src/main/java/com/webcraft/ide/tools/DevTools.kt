package com.webcraft.ide.tools

import java.net.URLEncoder
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID
import java.util.regex.Pattern

/** Pure utility functions for the developer toolkit. No Android dependencies. */

object JsonTools {
    /** Pretty-print JSON with 2-space indentation. Throws on invalid input. */
    fun prettyPrint(input: String): String {
        val sb = StringBuilder()
        var indent = 0
        var inString = false
        var escape = false
        for (c in input) {
            if (inString) {
                sb.append(c)
                if (escape) escape = false
                else if (c == '\\') escape = true
                else if (c == '"') inString = false
                continue
            }
            when (c) {
                '"' -> { inString = true; sb.append(c) }
                '{', '[' -> { sb.append(c); indent++; sb.append('\n'); repeat(indent) { sb.append("  ") } }
                '}', ']' -> {
                    indent--
                    sb.append('\n'); repeat(indent) { sb.append("  ") }; sb.append(c)
                }
                ',' -> { sb.append(c); sb.append('\n'); repeat(indent) { sb.append("  ") } }
                ':' -> sb.append(": ")
                else -> if (!c.isWhitespace()) sb.append(c)
            }
        }
        return sb.toString().trim()
    }

    /** Minify JSON (strip whitespace outside strings). */
    fun minify(input: String): String {
        val sb = StringBuilder()
        var inString = false
        var escape = false
        for (c in input) {
            if (inString) {
                sb.append(c)
                if (escape) escape = false
                else if (c == '\\') escape = true
                else if (c == '"') inString = false
                continue
            }
            when (c) {
                '"' -> { inString = true; sb.append(c) }
                ' ', '\t', '\n', '\r' -> {}
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}

object Base64Tools {
    fun encode(text: String): String = Base64.getEncoder().encodeToString(text.toByteArray())
    fun decode(encoded: String): String = String(Base64.getDecoder().decode(encoded.trim()))
    fun encodeUrlSafe(text: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray())
    fun decodeUrlSafe(encoded: String): String =
        String(Base64.getUrlDecoder().decode(encoded.trim()))
}

object UrlTools {
    fun encode(input: String): String = URLEncoder.encode(input, Charsets.UTF_8)
    fun decode(input: String): String = URLDecoder.decode(input, Charsets.UTF_8)
}

object UuidTools {
    fun v4(): String = UUID.randomUUID().toString()
    fun v7(): String {
        // UUID v7: 48-bit unix_ms + 12-bit rand + version + 62-bit rand
        val ms = System.currentTimeMillis()
        val bytes = ByteArray(16)
        // 48 bits of timestamp
        for (i in 0 until 6) bytes[i] = ((ms ushr (8 * (5 - i))) and 0xFF).toByte()
        // 12 random bits for rand_a, version 7 in high nibble of byte 6
        val r = java.security.SecureRandom()
        bytes[6] = (0x70 or (r.nextInt(16))).toByte()
        bytes[7] = r.nextInt(256).toByte()
        // variant 10 in high bits of byte 8, rest random
        bytes[8] = (0x80 or (r.nextInt(64))).toByte()
        for (i in 9 until 16) bytes[i] = r.nextInt(256).toByte()
        // Format as UUID string
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "${hex.substring(0,8)}-${hex.substring(8,12)}-${hex.substring(12,16)}-${hex.substring(16,20)}-${hex.substring(20)}"
    }
}

object TimestampTools {
    private val ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun epochToIso(epochSeconds: Long): String =
        Instant.ofEpochSecond(epochSeconds).toString()

    fun epochMsToIso(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).toString()

    fun isoToEpoch(iso: String): Long {
        val i = Instant.parse(iso)
        return i.epochSecond
    }

    fun isoToEpochMs(iso: String): Long =
        Instant.parse(iso).toEpochMilli()

    fun nowEpoch(): Long = System.currentTimeMillis() / 1000
    fun nowEpochMs(): Long = System.currentTimeMillis()
    fun nowIso(): String = Instant.now().toString()
}

object RegexTools {
    /** Returns list of (matchText, startIdx) */
    fun findMatches(pattern: String, input: String, flags: Int = 0): List<Pair<String, Int>> {
        if (pattern.isBlank()) return emptyList()
        val re = try { Pattern.compile(pattern, flags) } catch (_: Exception) { return emptyList() }
        val m = re.matcher(input)
        val out = mutableListOf<Pair<String, Int>>()
        while (m.find()) out.add(m.group() to m.start())
        return out
    }

    fun replaceAll(pattern: String, input: String, replacement: String, flags: Int = 0): String =
        Pattern.compile(pattern, flags).matcher(input).replaceAll(replacement)

    fun isValid(pattern: String): Boolean = try {
        Pattern.compile(pattern); true
    } catch (_: Exception) { false }
}

/** Color conversion helpers for the Color Picker. */
object ColorTools {
    data class Rgb(val r: Int, val g: Int, val b: Int)

    fun hexToRgb(hex: String): Rgb? {
        val h = hex.removePrefix("#").trim()
        return when (h.length) {
            3 -> {
                val r = h[0].digitToIntOrNull(16) ?: return null
                val g = h[1].digitToIntOrNull(16) ?: return null
                val b = h[2].digitToIntOrNull(16) ?: return null
                Rgb(r * 17, g * 17, b * 17)
            }
            6 -> {
                val r = h.substring(0,2).toIntOrNull(16) ?: return null
                val g = h.substring(2,4).toIntOrNull(16) ?: return null
                val b = h.substring(4,6).toIntOrNull(16) ?: return null
                Rgb(r, g, b)
            }
            8 -> {
                // RGBA — drop alpha for RGB view
                val r = h.substring(0,2).toIntOrNull(16) ?: return null
                val g = h.substring(2,4).toIntOrNull(16) ?: return null
                val b = h.substring(4,6).toIntOrNull(16) ?: return null
                Rgb(r, g, b)
            }
            else -> null
        }
    }

    fun rgbToHex(r: Int, g: Int, b: Int): String =
        "#%02X%02X%02X".format(r.coerceIn(0,255), g.coerceIn(0,255), b.coerceIn(0,255))

    fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf)
        val l = (max + min) / 2
        if (max == min) return Triple(0f, 0f, l)
        val d = max - min
        val s = if (l > 0.5) d / (2 - max - min) else d / (max + min)
        val h = when (max) {
            rf -> (gf - bf) / d + (if (gf < bf) 6 else 0)
            gf -> (bf - rf) / d + 2
            else -> (rf - gf) / d + 4
        } * 60f
        return Triple(h, s, l)
    }

    fun rgbToCmyk(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val k = 1 - maxOf(rf, gf, bf)
        val c = (1 - rf - k) / (1 - k).coerceAtLeast(0.0001f)
        val m = (1 - gf - k) / (1 - k).coerceAtLeast(0.0001f)
        val y = (1 - bf - k) / (1 - k).coerceAtLeast(0.0001f)
        return Triple(c, m, y)
    }

    fun hslToHex(h: Float, s: Float, l: Float): String {
        val c = (1 - kotlin.math.abs(2 * l - 1)) * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
        val m = l - c / 2
        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun f(v: Float) = ((v + m) * 255).toInt().coerceIn(0, 255)
        return rgbToHex(f(r1), f(g1), f(b1))
    }

    /** Simple WCAG relative luminance for contrast ratio. */
    fun contrastRatio(hex1: String, hex2: String): Double? {
        val c1 = hexToRgb(hex1) ?: return null
        val c2 = hexToRgb(hex2) ?: return null
        fun lum(c: Rgb): Double {
            fun ch(v: Int): Double {
                val s = v / 255.0
                return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * ch(c.r) + 0.7152 * ch(c.g) + 0.0722 * ch(c.b)
        }
        val l1 = lum(c1); val l2 = lum(c2)
        val hi = maxOf(l1, l2); val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }
}
