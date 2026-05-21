package com.checkpoint.api.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import com.checkpoint.api.client.RssFeedClient;
import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.MediaModule;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.modules.mediarss.types.MediaGroup;
import com.rometools.modules.mediarss.types.Metadata;
import com.rometools.modules.mediarss.types.Thumbnail;
import com.rometools.modules.mediarss.types.UrlReference;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;

/**
 * ROME-backed implementation of {@link RssFeedClient}.
 *
 * <p>Each fetch opens the URL with a 10s connect/read timeout, parses the body with
 * ROME's {@link SyndFeedInput}, strips all HTML from the description (we store plain
 * text — eliminates stored-XSS risk), and tries several sources to recover a usable
 * thumbnail URL. Any failure returns an empty list — callers decide how to react.</p>
 */
@Component
public class RssFeedClientImpl implements RssFeedClient {

    private static final Logger log = LoggerFactory.getLogger(RssFeedClientImpl.class);

    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT = "CheckPoint/1.0 (+https://checkpoint.local)";

    @Override
    public List<RssItem> fetch(String name, String url) {
        log.debug("Fetching RSS feed '{}' from {}", name, url);

        try {
            SyndFeed feed = readFeed(url);
            List<RssItem> items = new ArrayList<>();
            for (SyndEntry entry : feed.getEntries()) {
                RssItem item = toRssItem(entry);
                if (item != null) {
                    items.add(item);
                }
            }
            log.debug("RSS feed '{}' returned {} item(s)", name, items.size());
            return items;
        } catch (IOException | FeedException | IllegalArgumentException e) {
            log.warn("Failed to fetch/parse RSS feed '{}' ({}): {}", name, url, e.getMessage());
            return List.of();
        }
    }

    private SyndFeed readFeed(String url) throws IOException, FeedException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml;q=0.9, */*;q=0.8");
        if (connection instanceof HttpURLConnection http) {
            http.setInstanceFollowRedirects(true);
        }
        try (InputStream in = connection.getInputStream()) {
            SyndFeedInput input = new SyndFeedInput();
            return input.build(new InputSource(in));
        }
    }

    private RssItem toRssItem(SyndEntry entry) {
        String guid = entry.getUri();
        if (guid == null || guid.isBlank()) {
            guid = entry.getLink();
        }
        if (guid == null || guid.isBlank()) {
            return null;
        }

        String rawHtml = (entry.getDescription() != null) ? entry.getDescription().getValue() : null;
        String description = stripHtmlToPlainText(rawHtml);

        LocalDateTime publishedAt = null;
        if (entry.getPublishedDate() != null) {
            publishedAt = LocalDateTime.ofInstant(
                    entry.getPublishedDate().toInstant(), ZoneOffset.UTC);
        }

        String imageUrl = extractImageUrl(entry, rawHtml);

        return new RssItem(
                guid,
                entry.getTitle(),
                description,
                entry.getLink(),
                publishedAt,
                imageUrl
        );
    }

    /**
     * Strips every HTML tag (safelist=none) and returns the decoded text content.
     * Two-step pipeline because {@code Jsoup.clean} removes tags but leaves entities
     * un-decoded — {@code parse().text()} then collapses whitespace and decodes them.
     */
    private String stripHtmlToPlainText(String html) {
        if (html == null) {
            return null;
        }
        String stripped = Jsoup.clean(html, Safelist.none());
        String text = Jsoup.parse(stripped).text();
        return text.isBlank() ? null : text;
    }

    /**
     * Tries, in order: standard {@code <enclosure type="image/*">}, Media RSS
     * ({@code <media:content>}, {@code <media:thumbnail>}), then the first
     * {@code <img>} embedded in the description HTML. Returns the first http(s)
     * URL found, or {@code null} if none is available.
     */
    private String extractImageUrl(SyndEntry entry, String rawHtml) {
        String url = fromEnclosures(entry);
        if (url != null) return url;

        url = fromMediaModule(entry);
        if (url != null) return url;

        return fromDescriptionImg(rawHtml);
    }

    private String fromEnclosures(SyndEntry entry) {
        for (SyndEnclosure enclosure : entry.getEnclosures()) {
            String type = enclosure.getType();
            if (type != null && type.startsWith("image/")) {
                String accepted = acceptHttpUrl(enclosure.getUrl());
                if (accepted != null) return accepted;
            }
        }
        return null;
    }

    private String fromMediaModule(SyndEntry entry) {
        MediaEntryModule media = (MediaEntryModule) entry.getModule(MediaModule.URI);
        if (media == null) return null;

        String url = fromMediaContents(media.getMediaContents());
        if (url != null) return url;

        if (media.getMediaGroups() != null) {
            for (MediaGroup group : media.getMediaGroups()) {
                url = fromMediaContents(group.getContents());
                if (url != null) return url;
                url = fromThumbnails(group.getMetadata());
                if (url != null) return url;
            }
        }

        return fromThumbnails(media.getMetadata());
    }

    private String fromMediaContents(MediaContent[] contents) {
        if (contents == null) return null;
        for (MediaContent content : contents) {
            if (!isImageContent(content)) continue;
            if (!(content.getReference() instanceof UrlReference ref)) continue;
            String accepted = acceptHttpUrl(ref.getUrl() != null ? ref.getUrl().toString() : null);
            if (accepted != null) return accepted;
        }
        return null;
    }

    private boolean isImageContent(MediaContent content) {
        if (content == null) return false;
        String type = content.getType();
        if (type != null && type.startsWith("image/")) return true;
        String medium = content.getMedium();
        return "image".equalsIgnoreCase(medium);
    }

    private String fromThumbnails(Metadata metadata) {
        if (metadata == null || metadata.getThumbnail() == null) return null;
        for (Thumbnail thumb : metadata.getThumbnail()) {
            if (thumb == null || thumb.getUrl() == null) continue;
            String accepted = acceptHttpUrl(thumb.getUrl().toString());
            if (accepted != null) return accepted;
        }
        return null;
    }

    private String fromDescriptionImg(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) return null;
        Element img = Jsoup.parse(rawHtml).selectFirst("img");
        if (img == null) return null;
        return acceptHttpUrl(img.attr("src"));
    }

    /**
     * Allows only absolute http(s) URLs through — blocks {@code data:},
     * {@code javascript:}, and relative paths that wouldn't render anyway.
     */
    private String acceptHttpUrl(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return null;
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        return null;
    }
}
