package com.checkpoint.api.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bound configuration for the RSS news importer.
 *
 * <p>Example {@code application.properties}:
 * <pre>
 * news.rss.feeds[0].name=IGN
 * news.rss.feeds[0].url=https://feeds.feedburner.com/ign/all
 * </pre>
 *
 * <p>This is the first {@code @ConfigurationProperties} bean in the codebase — picked
 * over {@code @Value} because of the structured list-of-objects shape, which {@code @Value}
 * can't express cleanly.</p>
 */
@Component
@ConfigurationProperties(prefix = "news.rss")
public class RssFeedsProperties {

    private List<Feed> feeds = new ArrayList<>();

    public List<Feed> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<Feed> feeds) {
        this.feeds = feeds;
    }

    public static class Feed {
        private String name;
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
