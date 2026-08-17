package com.example.url_shortener;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import com.example.url_shortener.dtos.Urldto;
import com.example.url_shortener.entities.Urls;

@SpringBootApplication
public class UrlShortenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerApplication.class, args);
		
	}
	@Bean
	public ModelMapper modelMapper() {

	    ModelMapper mapper = new ModelMapper();

	    mapper.typeMap(Urldto.class, Urls.class)
	          .addMappings(m -> {
	              m.map(Urldto::getUrl, Urls::setOriginalUrl);
	              m.map(Urldto::getColumnAlias, Urls::setShortCode);
	          });

	    return mapper;
	}
	@Bean
	public RestClient restClient() {
		return RestClient.create();
	}
}
