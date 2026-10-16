package com.example.url_shortener.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.url_shortener.entities.Urls;

@Repository
public interface UrlRepository extends JpaRepository<Urls, Long> {

	Optional<Urls> findByShortCode(String shortCode);

	Optional<Urls> findByOriginalUrl(String url);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("update Urls url set url.clickCount = url.clickCount + 1 where url.shortCode = :shortCode")
	int incrementClickCountByShortCode(@Param("shortCode") String shortCode);

}
