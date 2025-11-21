package com.cartservice.client;

import com.cartservice.dto.ProductDto;
import com.cartservice.exception.RemoteServiceException;
import com.cartservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ProductClient {
    private final RestTemplate restTemplate;
    @Value("${product.service.url}")
    private String productServiceUrl;

    public ProductDto getProduct(Long productId) {
        String url = productServiceUrl + "/" + productId;
        try {
            return restTemplate.getForObject(url, ProductDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        } catch (RestClientException e) {
            throw new RemoteServiceException("Product service unavailable", e);
        }
    }
}
