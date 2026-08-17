package com.example.url_shortener.serviceimplementation;

import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.url_shortener.dtos.Urldto;
import com.example.url_shortener.entities.Urls;
import com.example.url_shortener.exception.ColumnAliasAlreadyExistsException;
import com.example.url_shortener.exception.URLNotFoundException;
import com.example.url_shortener.exception.UrlAlreadyExistsException;
import com.example.url_shortener.repository.UrlRepository;
import com.example.url_shortener.service.UrlService;

@Service
public class UrlServiceImpl implements UrlService {
	
	@Autowired
	UrlRepository urlRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	public Urls generateShortCode(Urldto urldto) {
//		String encodedurl= "";
		Optional<Urls> existedColumnAlias = urlRepository.findByShortCode(urldto.getColumnAlias());
		if (existedColumnAlias.isPresent()) {
		    throw new ColumnAliasAlreadyExistsException(
		        String.format("Column Alias %s already exists", urldto.getColumnAlias())
		    );
		}
		Optional<Urls> existingUrl = urlRepository.findByOriginalUrl(urldto.getUrl());
		if (existingUrl.isPresent()) {
		    throw new UrlAlreadyExistsException(
		        String.format("URL %s already exists", urldto.getUrl())
		    );
		}
		Urls url = modelMapper.map(urldto, Urls.class);
		if(urldto.getColumnAlias() != null && !urldto.getColumnAlias().isBlank()) {
			url.setShortCode(urldto.getColumnAlias());
			url.setOriginalUrl(urldto.getUrl());
			Urls savedurl = urlRepository.save(url);
			return savedurl;
		}

		url.setShortCode(null);
		Urls savedurl = urlRepository.save(url);
		savedurl.setShortCode(Base62.encode(savedurl.getId()));
		savedurl = urlRepository.save(savedurl);
		return savedurl;
	}
	
	

	@Override
	public Urls findByShortCode(String shortCode) {
		// TODO Auto-generated method stub
		return urlRepository.findByShortCode(shortCode).orElseThrow(() -> new URLNotFoundException("URL Not Found"));
	}
}
