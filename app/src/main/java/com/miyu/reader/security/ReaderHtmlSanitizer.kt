package com.miyu.reader.security

object ReaderHtmlSanitizer {
    private val blockedPairedTags = Regex(
        pattern = "<\\s*(script|iframe|object|embed|applet|base|form|textarea|select|button|svg|math)\\b[\\s\\S]*?<\\s*/\\s*\\1\\s*>",
        options = setOf(RegexOption.IGNORE_CASE),
    )
    private val blockedSingleTags = Regex(
        pattern = "<\\s*(script|iframe|object|embed|applet|base|meta|link|input|textarea|select|button|svg|math)\\b[^>]*>",
        options = setOf(RegexOption.IGNORE_CASE),
    )
    private val eventAttributes = Regex(
        pattern = "\\s+on[a-zA-Z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
        options = setOf(RegexOption.IGNORE_CASE),
    )
    private val styleAttributes = Regex(
        pattern = "\\s+style\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
        options = setOf(RegexOption.IGNORE_CASE),
    )
    private val dangerousUriAttributes = Regex(
        pattern = "\\s+(href|src|xlink:href)\\s*=\\s*([\"'])\\s*(javascript:|data:text/html|data:image/svg\\+xml|file:|content:)[^\"']*\\2",
        options = setOf(RegexOption.IGNORE_CASE),
    )
    private val remoteImageSources = Regex(
        pattern = "\\s+src\\s*=\\s*([\"'])\\s*https?://[^\"']*\\1",
        options = setOf(RegexOption.IGNORE_CASE),
    )
    private val sourceSets = Regex(
        pattern = "\\s+srcset\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    fun sanitize(html: String): String {
        if (html.isBlank()) return html

        return html
            .replace(blockedPairedTags, "")
            .replace(blockedSingleTags, "")
            .replace(eventAttributes, "")
            .replace(styleAttributes, "")
            .replace(dangerousUriAttributes) { match ->
                val attr = match.groupValues.getOrNull(1).orEmpty()
                if (attr.equals("href", ignoreCase = true)) " href=\"#\"" else " src=\"\""
            }
            .replace(remoteImageSources, " src=\"\"")
            .replace(sourceSets, "")
    }
}
