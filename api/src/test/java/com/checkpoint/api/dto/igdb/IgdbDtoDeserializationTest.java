package com.checkpoint.api.dto.igdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for IGDB DTO deserialization.
 * Verifies that sample JSON responses are correctly mapped to Java DTOs.
 */
class IgdbDtoDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should deserialize IGDB game response JSON to IgdbGameDto")
    void shouldDeserializeGameResponse() throws IOException {
        // Given
        InputStream jsonStream = getClass().getClassLoader()
                .getResourceAsStream("igdb/sample-game-response.json");
        assertThat(jsonStream).isNotNull();

        // When
        List<IgdbGameDto> games = objectMapper.readValue(
                jsonStream,
                new TypeReference<List<IgdbGameDto>>() {}
        );

        // Then
        assertThat(games).hasSize(1);

        IgdbGameDto game = games.get(0);

        // Basic game info
        assertThat(game.id()).isEqualTo(1942L);
        assertThat(game.name()).isEqualTo("The Witcher 3: Wild Hunt");
        assertThat(game.slug()).isEqualTo("the-witcher-3-wild-hunt");
        assertThat(game.summary()).contains("RPG and target narrative experience");
        assertThat(game.storyline()).contains("concludes the story of the witcher Geralt");
        assertThat(game.url()).isEqualTo("https://www.igdb.com/games/the-witcher-3-wild-hunt");

        // Release date (Unix timestamp: May 19, 2015)
        assertThat(game.firstReleaseDate()).isEqualTo(1431993600L);

        // Ratings
        assertThat(game.rating()).isEqualTo(92.5);
        assertThat(game.ratingCount()).isEqualTo(1250);
        assertThat(game.aggregatedRating()).isEqualTo(93.8);
        assertThat(game.aggregatedRatingCount()).isEqualTo(85);
        assertThat(game.totalRating()).isEqualTo(93.15);
        assertThat(game.totalRatingCount()).isEqualTo(1335);
    }

    @Test
    @DisplayName("Should deserialize cover with image URL helpers")
    void shouldDeserializeCover() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // When
        IgdbCoverDto cover = game.cover();

        // Then
        assertThat(cover).isNotNull();
        assertThat(cover.id()).isEqualTo(89386L);
        assertThat(cover.imageId()).isEqualTo("co1wyy");
        assertThat(cover.url()).isEqualTo("//images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg");
        assertThat(cover.width()).isEqualTo(264);
        assertThat(cover.height()).isEqualTo(374);
        assertThat(cover.alphaChannel()).isFalse();
        assertThat(cover.animated()).isFalse();

        // Image URL helpers
        assertThat(cover.getImageUrl("cover_big"))
                .isEqualTo("https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.jpg");
        assertThat(cover.getCoverBigUrl())
                .isEqualTo("https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.jpg");
        assertThat(cover.get720pUrl())
                .isEqualTo("https://images.igdb.com/igdb/image/upload/t_720p/co1wyy.jpg");
    }

    @Test
    @DisplayName("Should deserialize genres list")
    void shouldDeserializeGenres() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // When
        List<IgdbGenreDto> genres = game.genres();

        // Then
        assertThat(genres).hasSize(2);

        assertThat(genres.get(0).id()).isEqualTo(12L);
        assertThat(genres.get(0).name()).isEqualTo("Role-playing (RPG)");
        assertThat(genres.get(0).slug()).isEqualTo("role-playing-rpg");

        assertThat(genres.get(1).id()).isEqualTo(31L);
        assertThat(genres.get(1).name()).isEqualTo("Adventure");
    }

    @Test
    @DisplayName("Should deserialize platforms with logos")
    void shouldDeserializePlatforms() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // When
        List<IgdbPlatformDto> platforms = game.platforms();

        // Then
        assertThat(platforms).hasSize(3);

        // PC
        IgdbPlatformDto pc = platforms.get(0);
        assertThat(pc.id()).isEqualTo(6L);
        assertThat(pc.name()).isEqualTo("PC (Microsoft Windows)");
        assertThat(pc.abbreviation()).isEqualTo("PC");
        assertThat(pc.alternativeName()).isEqualTo("mswin");
        assertThat(pc.platformLogo()).isNotNull();
        assertThat(pc.platformLogo().imageId()).isEqualTo("plfl");

        // PS4
        IgdbPlatformDto ps4 = platforms.get(1);
        assertThat(ps4.id()).isEqualTo(48L);
        assertThat(ps4.name()).isEqualTo("PlayStation 4");
        assertThat(ps4.abbreviation()).isEqualTo("PS4");
        assertThat(ps4.generation()).isEqualTo(8);

        // Xbox One
        IgdbPlatformDto xboxOne = platforms.get(2);
        assertThat(xboxOne.id()).isEqualTo(49L);
        assertThat(xboxOne.name()).isEqualTo("Xbox One");
        assertThat(xboxOne.abbreviation()).isEqualTo("XONE");
    }

    @Test
    @DisplayName("Should deserialize involved companies with nested company and logo")
    void shouldDeserializeInvolvedCompanies() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // When
        List<IgdbInvolvedCompanyDto> involvedCompanies = game.involvedCompanies();

        // Then
        assertThat(involvedCompanies).hasSize(1);

        IgdbInvolvedCompanyDto involvement = involvedCompanies.get(0);
        assertThat(involvement.id()).isEqualTo(37623L);
        assertThat(involvement.developer()).isTrue();
        assertThat(involvement.publisher()).isTrue();
        assertThat(involvement.porting()).isFalse();
        assertThat(involvement.supporting()).isFalse();

        IgdbCompanyDto company = involvement.company();
        assertThat(company).isNotNull();
        assertThat(company.id()).isEqualTo(908L);
        assertThat(company.name()).isEqualTo("CD Projekt RED");
        assertThat(company.slug()).isEqualTo("cd-projekt-red");
        assertThat(company.country()).isEqualTo(616); // Poland

        IgdbCompanyLogoDto logo = company.logo();
        assertThat(logo).isNotNull();
        assertThat(logo.id()).isEqualTo(876L);
        assertThat(logo.imageId()).isEqualTo("cl2h");
    }

    @Test
    @DisplayName("Should deserialize screenshots")
    void shouldDeserializeScreenshots() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // When
        List<IgdbScreenshotDto> screenshots = game.screenshots();

        // Then
        assertThat(screenshots).hasSize(2);

        IgdbScreenshotDto screenshot = screenshots.get(0);
        assertThat(screenshot.id()).isEqualTo(4589L);
        assertThat(screenshot.imageId()).isEqualTo("dfgkfivjrhcksyymh9vw");
        assertThat(screenshot.width()).isEqualTo(1920);
        assertThat(screenshot.height()).isEqualTo(1080);

        // Image URL helper
        assertThat(screenshot.get720pUrl())
                .isEqualTo("https://images.igdb.com/igdb/image/upload/t_720p/dfgkfivjrhcksyymh9vw.jpg");
    }

    @Test
    @DisplayName("Should deserialize game modes")
    void shouldDeserializeGameModes() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // When
        List<IgdbGameModeDto> gameModes = game.gameModes();

        // Then
        assertThat(gameModes).hasSize(1);
        assertThat(gameModes.get(0).id()).isEqualTo(1L);
        assertThat(gameModes.get(0).name()).isEqualTo("Single player");
        assertThat(gameModes.get(0).slug()).isEqualTo("single-player");
    }

    @Test
    @DisplayName("Should deserialize themes")
    void shouldDeserializeThemes() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // When
        List<IgdbThemeDto> themes = game.themes();

        // Then
        assertThat(themes).hasSize(3);
        assertThat(themes).extracting(IgdbThemeDto::name)
                .containsExactly("Action", "Fantasy", "Open world");
    }

    @Test
    @DisplayName("Should deserialize player perspectives")
    void shouldDeserializePlayerPerspectives() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // When
        List<IgdbPlayerPerspectiveDto> perspectives = game.playerPerspectives();

        // Then
        assertThat(perspectives).hasSize(1);
        assertThat(perspectives.get(0).id()).isEqualTo(2L);
        assertThat(perspectives.get(0).name()).isEqualTo("Third person");
    }

    @Test
    @DisplayName("Should deserialize similar games and DLCs as ID lists")
    void shouldDeserializeSimilarGamesAndDlcs() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // Then
        assertThat(game.similarGames()).containsExactly(1020L, 1030L, 1074L, 1164L, 2155L);
        assertThat(game.dlcs()).containsExactly(16873L, 26758L);
        assertThat(game.expansions()).containsExactly(16873L, 26758L);
    }

    @Test
    @DisplayName("Should handle null fields gracefully")
    void shouldHandleNullFields() throws IOException {
        // Given
        IgdbGameDto game = loadFirstGame();

        // Then - these fields are null in the sample
        assertThat(game.parentGame()).isNull();
        assertThat(game.versionTitle()).isNull();
    }

    /**
     * Helper method to load the first game from sample JSON.
     */
    private IgdbGameDto loadFirstGame() throws IOException {
        InputStream jsonStream = getClass().getClassLoader()
                .getResourceAsStream("igdb/sample-game-response.json");
        List<IgdbGameDto> games = objectMapper.readValue(
                jsonStream,
                new TypeReference<List<IgdbGameDto>>() {}
        );
        return games.get(0);
    }
}
