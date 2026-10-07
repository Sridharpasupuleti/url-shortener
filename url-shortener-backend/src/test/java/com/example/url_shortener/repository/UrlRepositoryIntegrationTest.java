package com.example.url_shortener.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.example.url_shortener.entities.Urls;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(UrlRepositoryIntegrationTest.CacheConfiguration.class)
class UrlRepositoryIntegrationTest {

    @TestConfiguration
    static class CacheConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired
    private UrlRepository urlRepository;

    @Test
    void findsPersistedUrlByShortCodeAndOriginalUrl() {
        Urls url = new Urls();
        url.setOriginalUrl("https://example.org/article");
        url.setShortCode("article");
        url.setClickCount(3);
        urlRepository.saveAndFlush(url);

        assertEquals(url.getId(),
                urlRepository.findByShortCode("article").orElseThrow().getId());
        assertEquals(url.getId(),
                urlRepository.findByOriginalUrl("https://example.org/article").orElseThrow().getId());
    }

    @Test
    void concurrentInsertsWithSameAliasAllowOnlyOneWinner() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(
                    () -> insertWithAlias("first", ready, start));
            Future<Boolean> second = executor.submit(
                    () -> insertWithAlias("second", ready, start));

            ready.await();
            start.countDown();

            assertTrue(first.get() ^ second.get(),
                    "Exactly one insert should win the unique short-code constraint");
            assertEquals(1, urlRepository.findAll().stream()
                    .filter(url -> "duplicate".equals(url.getShortCode()))
                    .count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsOriginalUrlExceedingDatabaseColumnLength() {
        Urls invalid = new Urls();
        invalid.setOriginalUrl("https://example.org/" + "x".repeat(2048));
        invalid.setShortCode("too-long");

        assertThrows(DataIntegrityViolationException.class,
                () -> urlRepository.saveAndFlush(invalid));
    }

    @Test
    void emptyDatabaseHasNoMatchingShortCode() {
        assertTrue(urlRepository.findByShortCode("missing").isEmpty());
    }

    private boolean insertWithAlias(String path, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        Urls url = new Urls();
        url.setOriginalUrl("https://example.org/" + path);
        url.setShortCode("duplicate");
        ready.countDown();
        start.await();
        try {
            urlRepository.saveAndFlush(url);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }
}
