package com.checkpoint.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MentionParser}.
 */
class MentionParserTest {

    @Test
    @DisplayName("should extract a single mention")
    void shouldExtractSingleMention() {
        assertThat(MentionParser.extractMentions("hello @alice!")).containsExactly("alice");
    }

    @Test
    @DisplayName("should extract multiple distinct mentions in order")
    void shouldExtractMultipleMentions() {
        assertThat(MentionParser.extractMentions("@alice and @bob_42 say hi to @charlie-x"))
                .containsExactly("alice", "bob_42", "charlie-x");
    }

    @Test
    @DisplayName("should deduplicate repeated mentions of the same pseudo")
    void shouldDeduplicateMentions() {
        assertThat(MentionParser.extractMentions("@alice @alice @alice")).containsExactly("alice");
    }

    @Test
    @DisplayName("should ignore email addresses")
    void shouldIgnoreEmailAddresses() {
        assertThat(MentionParser.extractMentions("contact me at alice@example.com")).isEmpty();
    }

    @Test
    @DisplayName("should ignore doubled @@ sequences")
    void shouldIgnoreDoubleAt() {
        assertThat(MentionParser.extractMentions("look @@double trouble")).isEmpty();
    }

    @Test
    @DisplayName("should ignore a lone @ with no pseudo")
    void shouldIgnoreLoneAt() {
        assertThat(MentionParser.extractMentions("just an @ symbol")).isEmpty();
    }

    @Test
    @DisplayName("should ignore pseudos shorter than the minimum length")
    void shouldIgnoreTooShortPseudo() {
        assertThat(MentionParser.extractMentions("@a is too short")).isEmpty();
    }

    @Test
    @DisplayName("should match a mention at the very start of the content")
    void shouldMatchMentionAtStart() {
        assertThat(MentionParser.extractMentions("@alice first")).containsExactly("alice");
    }

    @Test
    @DisplayName("should match a mention immediately after punctuation")
    void shouldMatchMentionAfterPunctuation() {
        assertThat(MentionParser.extractMentions("(@alice)")).containsExactly("alice");
    }

    @Test
    @DisplayName("should cap the captured pseudo at 30 characters")
    void shouldCapPseudoLength() {
        String thirtyOne = "a".repeat(31);
        String thirty = "a".repeat(30);
        assertThat(MentionParser.extractMentions("@" + thirtyOne)).containsExactly(thirty);
    }

    @Test
    @DisplayName("should return an empty set for null content")
    void shouldReturnEmptyForNull() {
        assertThat(MentionParser.extractMentions(null)).isEmpty();
    }

    @Test
    @DisplayName("should return an empty set when there are no mentions")
    void shouldReturnEmptyWhenNoMentions() {
        Set<String> result = MentionParser.extractMentions("just plain text");
        assertThat(result).isEmpty();
    }
}
