package com.checkpoint.api.dto.profile;

/**
 * DTO representing a single bucket of a rating distribution histogram: how many
 * ratings were given for a specific score.
 *
 * <p>The score is on the raw 1&ndash;10 half-star scale (display value =
 * {@code score / 2}). Distributions are sparse: only scores that have at least
 * one rating are present, so clients must fill the missing scores with a count
 * of zero when rendering all ten possible bars.
 *
 * @param score the raw score (1&ndash;10)
 * @param count the number of ratings with this score
 */
public record RatingDistributionEntryDto(int score, long count) {}
