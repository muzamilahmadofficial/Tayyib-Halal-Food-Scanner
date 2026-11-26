package com.example.halalengine

import java.text.Normalizer

object TextNormalizer {

    private val combiningMarksRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val eNumberSpacedRegex = Regex("\\b(?:en:|fr:)?(?:e|ins)[\\s\\-]?([0-9]{3,4}[a-f]?)\\b", RegexOption.IGNORE_CASE)
    private val punctuationRegex = Regex("[^a-z0-9\\s%']")
    private val multiSpaceRegex = Regex("\\s+")

    /**
     * Lowercases text, folds French/Latin diacritics (gélatine -> gelatine, bœuf -> boeuf),
     * standardizes E-number tokens (E 471 / E-471 / en:e471 / INS 471 -> e471),
     * and cleans punctuation while preserving word boundaries.
     */
    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val expandedLigatures = input
            .lowercase()
            .replace("œ", "oe")
            .replace("æ", "ae")
            .replace("’", "'")
            .replace("‘", "'")

        val decomposed = Normalizer.normalize(expandedLigatures, Normalizer.Form.NFD)
        val noAccents = combiningMarksRegex.replace(decomposed, "")
        val standardizedENumbers = eNumberSpacedRegex.replace(noAccents) { match ->
            "e${match.groupValues[1].lowercase()}"
        }
        val cleaned = punctuationRegex.replace(standardizedENumbers, " ")
        return multiSpaceRegex.replace(cleaned, " ").trim()
    }

    /**
     * Normalizes an Open Food Facts tag like "en:e471" or "fr:halal" -> "e471" / "halal".
     */
    fun normalizeTag(tag: String): String {
        val stripped = tag.substringAfter(':').replace('-', ' ')
        return normalize(stripped)
    }

    /**
     * Extracts normalized E-numbers present in raw text or additive tags.
     */
    fun extractENumbers(text: String): Set<String> {
        val normalized = normalize(text)
        return Regex("\\be[0-9]{3,4}[a-f]?\\b")
            .findAll(normalized)
            .map { it.value.uppercase() }
            .toSet()
    }
}
