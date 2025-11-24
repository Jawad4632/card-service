package com.cartservice.client;

import com.cartservice.dto.OrderCreateRequest;
import com.cartservice.exception.BadRequestException;
import com.cartservice.exception.RemoteServiceException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OrderClient {
    private final RestTemplate restTemplate;

    @Value("${order.service.url}")
    private String orderUrl;

    public Long createOrder(OrderCreateRequest req) {
        try {
            return restTemplate.postForObject(orderUrl, req, Long.class);
        }
        catch (HttpClientErrorException.BadRequest e){
            throw new BadRequestException(e.getMessage());
        }
        catch (RestClientException ex) {
            throw new RemoteServiceException("Failed to call Order Service", ex);
        }
    }
}
