package com.checkpoint.api.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.checkpoint.api.client.impl.IgdbApiClientImpl;
import com.checkpoint.api.dto.igdb.IgdbGameDto;
import com.checkpoint.api.dto.igdb.IgdbTimeToBeatDto;

/**
 * Unit tests for {@link IgdbApiClientImpl} pagination and batching, backed by a
 * {@link MockRestServiceServer} bound to the {@code RestClient} (no real network).
 */
class IgdbApiClientImplTest {

    private MockRestServiceServer server;
    private IgdbApiClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new IgdbApiClientImpl(builder.build());
    }

    @Test
    @DisplayName("fetchTopRatedGames pages through results with increasing offset and sorts by rating count")
    void fetchTopRatedGames_paginates() {
        // Pages are 200 rows (GAMES_PAGE_SIZE). limit 500 → 200 (offset 0) +
        // 200 (offset 200) + 100 (offset 400, partial → stops).
        server.expect(ExpectedCount.once(), requestTo("/games"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("sort total_rating_count desc")))
                .andExpect(content().string(containsString("offset 0")))
                .andRespond(withSuccess(gamesJson(1, 200), MediaType.APPLICATION_JSON));

        server.expect(ExpectedCount.once(), requestTo("/games"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("offset 200")))
                .andRespond(withSuccess(gamesJson(201, 200), MediaType.APPLICATION_JSON));

        server.expect(ExpectedCount.once(), requestTo("/games"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("offset 400")))
                .andRespond(withSuccess(gamesJson(401, 100), MediaType.APPLICATION_JSON));

        List<IgdbGameDto> result = client.fetchTopRatedGames(500, 50);

        assertThat(result).hasSize(500);
        server.verify();
    }

    @Test
    @DisplayName("fetchTopRatedGames truncates an overshooting final page to the requested limit")
    void fetchTopRatedGames_truncatesToLimit() {
        // limit 250 → page 1 (200, offset 0) + page 2 requests 50 but IGDB returns
        // a full 200; the aggregate is trimmed back to exactly 250.
        server.expect(ExpectedCount.once(), requestTo("/games"))
                .andExpect(content().string(containsString("offset 0")))
                .andRespond(withSuccess(gamesJson(1, 200), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo("/games"))
                .andExpect(content().string(containsString("offset 200")))
                .andRespond(withSuccess(gamesJson(201, 200), MediaType.APPLICATION_JSON));

        List<IgdbGameDto> result = client.fetchTopRatedGames(250, 50);

        assertThat(result).hasSize(250);
        server.verify();
    }

    @Test
    @DisplayName("fetchTopRatedGames stops early when a short page is returned")
    void fetchTopRatedGames_stopsOnShortPage() {
        // Requesting 1000 but IGDB only has 10 qualifying games → single request.
        server.expect(ExpectedCount.once(), requestTo("/games"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(gamesJson(1, 10), MediaType.APPLICATION_JSON));

        List<IgdbGameDto> result = client.fetchTopRatedGames(1000, 50);

        assertThat(result).hasSize(10);
        server.verify();
    }

    @Test
    @DisplayName("fetchTimeToBeatForGames chunks ids into batches of 500 and merges the results")
    void fetchTimeToBeatForGames_batches() {
        // 1100 ids → 3 batches (500 + 500 + 100)
        List<Long> ids = new java.util.ArrayList<>();
        for (long i = 1; i <= 1100; i++) {
            ids.add(i);
        }

        server.expect(ExpectedCount.once(), requestTo("/game_time_to_beat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("where game_id = (")))
                .andRespond(withSuccess(ttbJson(1, 500), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo("/game_time_to_beat"))
                .andRespond(withSuccess(ttbJson(501, 500), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo("/game_time_to_beat"))
                .andRespond(withSuccess(ttbJson(1001, 100), MediaType.APPLICATION_JSON));

        Map<Long, IgdbTimeToBeatDto> result = client.fetchTimeToBeatForGames(ids);

        assertThat(result).hasSize(1100);
        assertThat(result.get(42L).normally()).isEqualTo(100L);
        server.verify();
    }

    private static String gamesJson(long startId, int count) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(',');
            long id = startId + i;
            sb.append("{\"id\":").append(id).append(",\"name\":\"Game ").append(id).append("\"}");
        }
        return sb.append("]").toString();
    }

    private static String ttbJson(long startId, int count) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(',');
            long id = startId + i;
            sb.append("{\"game_id\":").append(id)
                    .append(",\"normally\":100,\"hastily\":50,\"completely\":200}");
        }
        return sb.append("]").toString();
    }
}
