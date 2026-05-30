package com.seyzeriat.desktop.service;

import java.io.IOException;
import com.seyzeriat.desktop.dto.NewsResult;
import com.seyzeriat.desktop.dto.NewsRequestPayload;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.exception.UnauthorizedException;

public interface NewsService {
    PagedResponse<NewsResult> getNews(int page, int size, String sort) throws IOException, InterruptedException, UnauthorizedException;
    NewsResult createNews(NewsRequestPayload payload) throws IOException, InterruptedException, UnauthorizedException;
    NewsResult updateNews(String id, NewsRequestPayload payload) throws IOException, InterruptedException, UnauthorizedException;
    void deleteNews(String id) throws IOException, InterruptedException, UnauthorizedException;
    NewsResult publishNews(String id) throws IOException, InterruptedException, UnauthorizedException;
    NewsResult unpublishNews(String id) throws IOException, InterruptedException, UnauthorizedException;
}
