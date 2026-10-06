package com.example.url_shortener.serviceimplementation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.url_shortener.dtos.Urldto;
import com.example.url_shortener.dtos.UrlResponseDTO;
import com.example.url_shortener.entities.Urls;
import com.example.url_shortener.repository.UrlRepository;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplAutoGenTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private org.modelmapper.ModelMapper modelMapper;

    @InjectMocks
    private UrlServiceImpl urlService;

    private Urldto sampleDto;
    private Urls sampleEntity;

    @BeforeEach
    void setUp() {
        sampleDto = new Urldto();
        sampleDto.setUrl("https://google.com");
        sampleDto.setColumnAlias(null); // No custom alias for auto-generation

        sampleEntity = new Urls();
        sampleEntity.setId(100L);
        sampleEntity.setOriginalUrl("https://google.com");
    }

    @Test
    @DisplayName("Should successfully auto-generate short code (Double-Save path)")
    void testGenerateShortCode_AutoGenerate() {
        // Arrange
        when(urlRepository.findByShortCode(null)).thenReturn(Optional.empty());
        when(urlRepository.findByOriginalUrl("https://google.com")).thenReturn(Optional.empty());
        when(modelMapper.map(any(Urldto.class), eq(Urls.class))).thenReturn(sampleEntity);

        // The "Double Save" behavior:
        // 1st save returns the entity with an ID
        when(urlRepository.save(any(Urls.class)))
            .thenReturn(sampleEntity) // First save
            .thenReturn(sampleEntity); // Second save

        when(modelMapper.map(any(Urls.class), eq(UrlResponseDTO.class))).thenReturn(new UrlResponseDTO());

        // Act
        urlService.generateShortCode(sampleDto);

        // Assert
        // Verify that save was called TWICE (The inefficiency we identified)
        verify(urlRepository, times(2)).save(any(Urls.class));

        // Verify that the final short code was generated from the ID (Base62.encode(100))
        // 100 in Base62: 100 / 62 = 1 remainder 38. 38th char is 'c'. Result: '1c'
        assertEquals("1c", sampleEntity.getShortCode(), "Short code should be Base62 encoded ID");
    }
}
