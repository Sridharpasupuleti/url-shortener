package com.example.url_shortener.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.url_shortener.dtos.Urldto;
import com.example.url_shortener.dtos.UrlResponseDTO;
import com.example.url_shortener.exception.ColumnAliasAlreadyExistsException;
import com.example.url_shortener.repository.UrlRepository;
import com.example.url_shortener.service.UrlService;
import com.example.url_shortener.service.UrlShorteningRateLimiter;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

class HomeControllerTest {

    private UrlService urlService;
    private UrlShorteningRateLimiter rateLimiter;
    private HttpServer targetServer;
    private ExecutorService targetServerExecutor;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws IOException {
        urlService = org.mockito.Mockito.mock(UrlService.class);
        rateLimiter = org.mockito.Mockito.mock(UrlShorteningRateLimiter.class);
        when(rateLimiter.isAllowed(any())).thenReturn(true);
        targetServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        targetServer.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        targetServerExecutor = Executors.newSingleThreadExecutor();
        targetServer.setExecutor(targetServerExecutor);
        targetServer.start();

        HomeController controller = new HomeController();
        ReflectionTestUtils.setField(controller, "urlService", urlService);
        ReflectionTestUtils.setField(controller, "rateLimiter", rateLimiter);
        ReflectionTestUtils.setField(controller, "publicBaseUrl", "https://short.example.com/");
        ReflectionTestUtils.setField(controller, "urlRepository",
                org.mockito.Mockito.mock(UrlRepository.class));
        ReflectionTestUtils.setField(controller, "restClient", RestClient.create());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown() {
        if (targetServer != null) {
            targetServer.stop(0);
        }
        if (targetServerExecutor != null) {
            targetServerExecutor.shutdownNow();
        }
    }

    @Test
    void homeReturnsHealthMessage() throws Exception {
        mockMvc.perform(get("/urlshortener/home"))
                .andExpect(status().isOk())
                .andExpect(content().string("Works !!"));
    }

    @Test
    void redirectSendsOriginalUrlAndIncrementsClickCount() throws Exception {
        when(urlService.getOriginalUrl("abc")).thenReturn("https://example.org/article");

        mockMvc.perform(get("/urlshortener/abc"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.org/article"));

        verify(urlService).incrementClickCount("abc");
    }

    @Test
    void invalidUrlIsRejectedWithoutCallingService() throws Exception {
        mockMvc.perform(post("/urlshortener/posturl")
                        .contentType("application/json")
                        .content("""
                                {"url":"ftp://example.org/file","columnAlias":"docs"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details").value("Url InValid"));

        verify(urlService, never()).generateShortCode(any(Urldto.class));
    }

    @Test
    void unreachableUrlIsRejectedWithoutCallingService() throws Exception {
        mockMvc.perform(post("/urlshortener/posturl")
                        .contentType("application/json")
                        .content("""
                                {"url":"http://127.0.0.1:1/unreachable","columnAlias":"docs"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details").value("Please provide a valid URL"));

        verify(urlService, never()).generateShortCode(any(Urldto.class));
    }

    @Test
    void duplicateAliasReturnsBadRequest() throws Exception {
        when(urlService.generateShortCode(any(Urldto.class)))
                .thenThrow(new ColumnAliasAlreadyExistsException("Alias already exists"));

        mockMvc.perform(post("/urlshortener/posturl")
                        .contentType("application/json")
                        .content("""
                                {"url":"http://127.0.0.1:%d/target","columnAlias":"docs"}
                                """.formatted(targetServer.getAddress().getPort())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details")
                        .value(containsString("Column Alias Already Exists")));
    }

    @Test
    void validUrlReturnsShortenedUrl() throws Exception {
        when(urlService.generateShortCode(any(Urldto.class)))
                .thenReturn(new UrlResponseDTO("https://example.org/page", "abc"));

        mockMvc.perform(post("/urlshortener/posturl")
                        .contentType("application/json")
                        .content("""
                                {"url":"http://127.0.0.1:%d/target","columnAlias":"docs"}
                                """.formatted(targetServer.getAddress().getPort())))
                .andExpect(status().isOk())
                .andExpect(content().string("https://short.example.com/urlshortener/abc"));
    }

    @Test
    void rateLimitedRequestReturnsTooManyRequestsJson() throws Exception {
        when(rateLimiter.isAllowed(any())).thenReturn(false);

        mockMvc.perform(post("/urlshortener/posturl")
                        .contentType("application/json")
                        .content("""
                                {"url":"https://example.org/page","columnAlias":"docs"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message")
                        .value("Rate limit exceeded: maximum 10 URL-shortening requests per minute per IP."))
                .andExpect(jsonPath("$.details").value("Too Many Requests"));

        verify(urlService, never()).generateShortCode(any(Urldto.class));
    }

    @Test
    void nonPostEndpointsDoNotUseRateLimiter() throws Exception {
        when(urlService.getOriginalUrl("abc")).thenReturn("https://example.org/article");

        mockMvc.perform(get("/urlshortener/home"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/urlshortener/abc"))
                .andExpect(status().isFound());

        verify(rateLimiter, never()).isAllowed(any());
    }
}
