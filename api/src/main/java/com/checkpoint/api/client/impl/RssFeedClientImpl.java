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
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import com.checkpoint.api.client.RssFeedClient;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;

/**
 * ROME-backed implementation of {@link RssFeedClient}.
 *
 * <p>Each fetch opens the URL with a 10s connect/read timeout, parses the body with
 * ROME's {@link SyndFeedInput}, and sanitizes HTML in the description via Jsoup.
 * Any failure returns an empty list — callers decide how to react.</p>
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

        String description = null;
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            description = Jsoup.clean(entry.getDescription().getValue(), Safelist.basic());
        }

        LocalDateTime publishedAt = null;
        if (entry.getPublishedDate() != null) {
            publishedAt = LocalDateTime.ofInstant(
                    entry.getPublishedDate().toInstant(), ZoneOffset.UTC);
        }

        String imageUrl = null;
        for (SyndEnclosure enclosure : entry.getEnclosures()) {
            String type = enclosure.getType();
            if (type != null && type.startsWith("image/")) {
                imageUrl = enclosure.getUrl();
                break;
            }
        }

        return new RssItem(
                guid,
                entry.getTitle(),
                description,
                entry.getLink(),
                publishedAt,
                imageUrl
        );
    }
}
