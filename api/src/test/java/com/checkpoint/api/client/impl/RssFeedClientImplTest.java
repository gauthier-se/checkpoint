package com.checkpoint.api.client.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.checkpoint.api.client.RssFeedClient.RssItem;

class RssFeedClientImplTest {

    private final RssFeedClientImpl client = new RssFeedClientImpl();

    @Test
    void extractsImageFromEnclosure(@TempDir Path tempDir) throws IOException {
        String xml = rssWithItem("""
                <item>
                  <guid>guid-enclosure</guid>
                  <title>Enclosure</title>
                  <description>Body</description>
                  <link>https://example.com/e</link>
                  <enclosure url="https://cdn.example.com/img.jpg" type="image/jpeg" length="1234" />
                </item>
                """);

        List<RssItem> items = client.fetch("test", writeFeed(tempDir, xml));

        assertThat(items).singleElement()
                .extracting(RssItem::imageUrl).isEqualTo("https://cdn.example.com/img.jpg");
    }

    @Test
    void extractsImageFromMediaContent(@TempDir Path tempDir) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
                  <channel>
                    <title>F</title><link>https://f</link><description>d</description>
                    <item>
                      <guid>guid-mrss-content</guid>
                      <title>MRSS content</title>
                      <description>Body</description>
                      <link>https://example.com/m</link>
                      <media:content url="https://cdn.example.com/mrss.jpg" medium="image" />
                    </item>
                  </channel>
                </rss>
                """;

        List<RssItem> items = client.fetch("test", writeFeed(tempDir, xml));

        assertThat(items).singleElement()
                .extracting(RssItem::imageUrl).isEqualTo("https://cdn.example.com/mrss.jpg");
    }

    @Test
    void extractsImageFromMediaThumbnail(@TempDir Path tempDir) throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
                  <channel>
                    <title>F</title><link>https://f</link><description>d</description>
                    <item>
                      <guid>guid-mrss-thumb</guid>
                      <title>MRSS thumb</title>
                      <description>Body</description>
                      <link>https://example.com/t</link>
                      <media:thumbnail url="https://cdn.example.com/thumb.jpg" />
                    </item>
                  </channel>
                </rss>
                """;

        List<RssItem> items = client.fetch("test", writeFeed(tempDir, xml));

        assertThat(items).singleElement()
                .extracting(RssItem::imageUrl).isEqualTo("https://cdn.example.com/thumb.jpg");
    }

    @Test
    void fallsBackToImgInsideDescription(@TempDir Path tempDir) throws IOException {
        String xml = rssWithItem("""
                <item>
                  <guid>guid-img-in-html</guid>
                  <title>HTML img</title>
                  <description>&lt;p&gt;Intro &lt;img src="https://cdn.example.com/inline.jpg" /&gt; more.&lt;/p&gt;</description>
                  <link>https://example.com/h</link>
                </item>
                """);

        List<RssItem> items = client.fetch("test", writeFeed(tempDir, xml));

        assertThat(items).singleElement()
                .extracting(RssItem::imageUrl).isEqualTo("https://cdn.example.com/inline.jpg");
    }

    @Test
    void returnsNullImageWhenNoSourceAvailable(@TempDir Path tempDir) throws IOException {
        String xml = rssWithItem("""
                <item>
                  <guid>guid-no-image</guid>
                  <title>No image</title>
                  <description>Plain body</description>
                  <link>https://example.com/n</link>
                </item>
                """);

        List<RssItem> items = client.fetch("test", writeFeed(tempDir, xml));

        assertThat(items).singleElement().extracting(RssItem::imageUrl).isNull();
    }

    @Test
    void rejectsNonHttpImageUrls(@TempDir Path tempDir) throws IOException {
        String xml = rssWithItem("""
                <item>
                  <guid>guid-bad-url</guid>
                  <title>Bad URL</title>
                  <description>&lt;img src="javascript:alert(1)" /&gt;</description>
                  <link>https://example.com/b</link>
                  <enclosure url="data:image/png;base64,AAAA" type="image/png" length="4" />
                </item>
                """);

        List<RssItem> items = client.fetch("test", writeFeed(tempDir, xml));

        assertThat(items).singleElement().extracting(RssItem::imageUrl).isNull();
    }

    @Test
    void stripsAllHtmlTagsFromDescription(@TempDir Path tempDir) throws IOException {
        String xml = rssWithItem("""
                <item>
                  <guid>guid-strip</guid>
                  <title>Strip</title>
                  <description>&lt;p&gt;text &lt;a href="x"&gt;link&lt;/a&gt;&lt;/p&gt;</description>
                  <link>https://example.com/s</link>
                </item>
                """);

        List<RssItem> items = client.fetch("test", writeFeed(tempDir, xml));

        assertThat(items).singleElement()
                .extracting(RssItem::description).isEqualTo("text link");
    }

    @Test
    void stripsScriptContentFromDescription(@TempDir Path tempDir) throws IOException {
        String xml = rssWithItem("""
                <item>
                  <guid>guid-script</guid>
                  <title>Script</title>
                  <description>&lt;script&gt;alert(1)&lt;/script&gt;hello</description>
                  <link>https://example.com/x</link>
                </item>
                """);

        List<RssItem> items = client.fetch("test", writeFeed(tempDir, xml));

        assertThat(items).singleElement()
                .extracting(RssItem::description).isEqualTo("hello");
    }

    @Test
    void returnsEmptyListOnMalformedFeed(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("bad.xml");
        Files.writeString(file, "not actually xml");

        List<RssItem> items = client.fetch("bad", file.toUri().toString());

        assertThat(items).isEmpty();
    }

    private static String rssWithItem(String itemXml) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <title>F</title><link>https://f</link><description>d</description>
                    %s
                  </channel>
                </rss>
                """.formatted(itemXml);
    }

    private static String writeFeed(Path tempDir, String xml) throws IOException {
        Path file = tempDir.resolve("feed-" + System.nanoTime() + ".xml");
        Files.writeString(file, xml);
        return file.toUri().toString();
    }
}
