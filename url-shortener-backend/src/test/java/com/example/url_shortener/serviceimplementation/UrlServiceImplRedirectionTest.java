package com.example.url_shortener.serviceimplementation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.url_shortener.entities.Urls;
import com.example.url_shortener.exception.URLNotFoundException;
import com.example.url_shortener.repository.UrlRepository;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplRedirectionTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlServiceImpl urlService;

    private Urls sampleEntity;

    @BeforeEach
    void setUp() {
        sampleEntity = new Urls();
        sampleEntity.setId(1L);
        sampleEntity.setShortCode("google");
        sampleEntity.setOriginalUrl("https://google.com");
        sampleEntity.setClickCount(10L);
    }

    @Test
    @DisplayName("Should return original URL when short code exists")
    void testGetOriginalUrl_Success() {
        // Arrange
        when(urlRepository.findByShortCode("google")).thenReturn(Optional.of(sampleEntity));

        // Act
        String result = urlService.getOriginalUrl("google");

        // Assert
        assertEquals("https://google.com", result);
        verify(urlRepository, times(1)).findByShortCode("google");
    }

    @Test
    @DisplayName("Should throw URLNotFoundException when short code does not exist")
    void testGetOriginalUrl_NotFound() {
        // Arrange
        when(urlRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(URLNotFoundException.class, () -> {
            urlService.getOriginalUrl("unknown");
        });
    }

    @Test
    @DisplayName("Should increment click count correctly")
    void testIncrementClickCount_Success() {
        when(urlRepository.incrementClickCountByShortCode("google")).thenReturn(1);
        urlService.incrementClickCount("google");

        verify(urlRepository).incrementClickCountByShortCode("google");
        verify(urlRepository, never()).findByShortCode("google");
        verify(urlRepository, never()).save(any(Urls.class));
    }

    @Test
    @DisplayName("Should throw exception when incrementing click count for non-existent code")
    void testIncrementClickCount_NotFound() {
        when(urlRepository.incrementClickCountByShortCode("unknown")).thenReturn(0);

        // Act & Assert
        assertThrows(URLNotFoundException.class, () -> {
            urlService.incrementClickCount("unknown");
        });
    }
}
