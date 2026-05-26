package com.checkpoint.api.utils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting {@code @username} mentions from user-generated content
 * such as comments and reviews.
 */
public final class MentionParser {

    /**
     * Matches an {@code @pseudo} token where the pseudo is 2-30 characters of
     * letters, digits, underscores or hyphens — the valid CheckPoint pseudo format.
     *
     * <p>The {@code (?<![\w@])} lookbehind ensures the {@code @} is not preceded by
     * a word character or another {@code @}, so email addresses ({@code user@host})
     * and doubled {@code @@} sequences are not treated as mentions.</p>
     */
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![\\w@])@([a-zA-Z0-9_-]{2,30})");

    private MentionParser() {
    }

    /**
     * Extracts the distinct pseudos mentioned in the given content.
     *
     * @param content the text to scan (may be {@code null})
     * @return an ordered set of mentioned pseudos (without the leading {@code @});
     *         empty if the content is {@code null} or contains no mentions
     */
    public static Set<String> extractMentions(String content) {
        Set<String> pseudos = new LinkedHashSet<>();
        if (content == null) {
            return pseudos;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            pseudos.add(matcher.group(1));
        }
        return pseudos;
    }
}
