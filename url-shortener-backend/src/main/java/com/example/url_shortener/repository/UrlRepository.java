package com.example.url_shortener.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.url_shortener.entities.Urls;

@Repository
public interface UrlRepository extends JpaRepository<Urls, Long> {

	Optional<Urls> findByShortCode(String shortCode);

	Optional<Urls> findByOriginalUrl(String url);

}
