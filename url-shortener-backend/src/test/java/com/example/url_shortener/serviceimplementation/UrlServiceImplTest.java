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
import org.springframework.dao.DataIntegrityViolationException;

import com.example.url_shortener.dtos.Urldto;
import com.example.url_shortener.dtos.UrlResponseDTO;
import com.example.url_shortener.entities.Urls;
import com.example.url_shortener.exception.ColumnAliasAlreadyExistsException;
import com.example.url_shortener.exception.UrlAlreadyExistsException;
import com.example.url_shortener.repository.UrlRepository;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

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
        sampleDto.setColumnAlias("google");

        sampleEntity = new Urls();
        sampleEntity.setId(1L);
        sampleEntity.setOriginalUrl("https://google.com");
        sampleEntity.setShortCode("google");
    }

    @Test
    @DisplayName("Should throw exception when custom alias already exists")
    void testGenerateShortCode_AliasExists() {
        // Arrange: Tell the mock repository to return a value when this alias is searched
        when(urlRepository.findByShortCode("google")).thenReturn(Optional.of(sampleEntity));

        // Act & Assert: Verify that the specific exception is thrown
        assertThrows(ColumnAliasAlreadyExistsException.class, () -> {
            urlService.generateShortCode(sampleDto);
        });

        verify(urlRepository, times(1)).findByShortCode("google");
    }

    @Test
    @DisplayName("Should throw exception when original URL already exists")
    void testGenerateShortCode_UrlExists() {
        // Arrange: Alias is free, but URL is already in DB
        when(urlRepository.findByShortCode("google")).thenReturn(Optional.empty());
        when(urlRepository.findByOriginalUrl("https://google.com")).thenReturn(Optional.of(sampleEntity));

        // Act & Assert
        assertThrows(UrlAlreadyExistsException.class, () -> {
            urlService.generateShortCode(sampleDto);
        });
    }

    @Test
    @DisplayName("Should successfully shorten URL using custom alias")
    void testGenerateShortCode_WithAlias() {
        // Arrange
        when(urlRepository.findByShortCode("google")).thenReturn(Optional.empty());
        when(urlRepository.findByOriginalUrl("https://google.com")).thenReturn(Optional.empty());
        when(urlRepository.saveAndFlush(any(Urls.class))).thenReturn(sampleEntity);
        when(modelMapper.map(any(Urldto.class), eq(Urls.class))).thenReturn(sampleEntity);
        when(modelMapper.map(any(Urls.class), eq(UrlResponseDTO.class))).thenReturn(new UrlResponseDTO());

        // Act
        urlService.generateShortCode(sampleDto);

        // Assert: Verify the record was saved exactly once
        verify(urlRepository, times(1)).saveAndFlush(any(Urls.class));
    }

    @Test
    @DisplayName("Should report an alias conflict when another request claims the alias first")
    void testGenerateShortCode_ConcurrentAliasConflict() {
        when(urlRepository.findByShortCode("google"))
                .thenReturn(Optional.empty(), Optional.of(sampleEntity));
        when(urlRepository.findByOriginalUrl("https://google.com")).thenReturn(Optional.empty());
        when(modelMapper.map(any(Urldto.class), eq(Urls.class))).thenReturn(sampleEntity);
        when(urlRepository.saveAndFlush(any(Urls.class)))
                .thenThrow(new DataIntegrityViolationException("unique short code constraint"));

        ColumnAliasAlreadyExistsException exception = assertThrows(
                ColumnAliasAlreadyExistsException.class,
                () -> urlService.generateShortCode(sampleDto));

        assertEquals("Column Alias google already exists", exception.getMessage());
        verify(urlRepository).saveAndFlush(any(Urls.class));
        verify(urlRepository, times(2)).findByShortCode("google");
    }

    @Test
    @DisplayName("Should propagate unrelated database integrity failures")
    void testGenerateShortCode_PropagatesUnrelatedDatabaseFailure() {
        when(urlRepository.findByShortCode("google")).thenReturn(Optional.empty());
        when(urlRepository.findByOriginalUrl("https://google.com")).thenReturn(Optional.empty());
        when(modelMapper.map(any(Urldto.class), eq(Urls.class))).thenReturn(sampleEntity);
        when(urlRepository.saveAndFlush(any(Urls.class)))
                .thenThrow(new DataIntegrityViolationException("unrelated database constraint"));

        assertThrows(DataIntegrityViolationException.class,
                () -> urlService.generateShortCode(sampleDto));

        verify(urlRepository, times(2)).findByShortCode("google");
    }
}
