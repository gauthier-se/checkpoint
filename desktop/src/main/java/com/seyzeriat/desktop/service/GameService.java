package com.seyzeriat.desktop.service;

import java.io.IOException;
import java.util.List;
import com.seyzeriat.desktop.dto.*;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.exception.GameReferencedException;

public interface GameService {
    List<ExternalGameResult> searchExternalGames(String query, int limit) throws IOException, InterruptedException, UnauthorizedException;
    ImportedGameResult importGame(Long externalId) throws IOException, InterruptedException, UnauthorizedException;
    ImportJobStatus startTopRatedImport(int limit, int minRatingCount) throws IOException, InterruptedException, UnauthorizedException;
    ImportJobStatus startRecentImport(int limit) throws IOException, InterruptedException, UnauthorizedException;
    ImportJobStatus getImportJob(String jobId) throws IOException, InterruptedException, UnauthorizedException;
    
    PagedResponse<GameSummaryResult> getGames(int page, int size) throws IOException, InterruptedException, UnauthorizedException;
    GameDetailResult getGameDetail(String id) throws IOException, InterruptedException, UnauthorizedException;
    GameDetailResult createGame(GameFormPayload payload) throws IOException, InterruptedException, UnauthorizedException;
    GameDetailResult updateGame(String id, GameFormPayload payload) throws IOException, InterruptedException, UnauthorizedException;
    void deleteGame(String id) throws IOException, InterruptedException, UnauthorizedException, GameReferencedException;
    
    List<CatalogOption> getGenres() throws IOException, InterruptedException, UnauthorizedException;
    List<CatalogOption> getPlatforms() throws IOException, InterruptedException, UnauthorizedException;
    List<CatalogOption> getCompanies() throws IOException, InterruptedException, UnauthorizedException;
}
