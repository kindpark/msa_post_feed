package com.example.login.feed.repository;
import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(name="feed.follow-reader",
        havingValue = "http")
public class HttpFollowReader implements FollowReader {
    private final RestClient client;

    // 초기화용 생성자
    public HttpFollowReader(
            @Value("${user-service.url}")
            String baseUrl
    ){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(Duration.ofSeconds(3));
                factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
    @Override
    public List<Long> followingIds(Long userId) {
        try{

            var req = client.get().uri("/internal/users/{userId}/following-ids", userId);

            List<Long> ids = req.retrieve().body(new ParameterizedTypeReference<List<Long>>() {
            });

            return ids == null ? List.of() : ids;
        }catch(RestClientException e){
            throw new BusinessException(ErrorCode.UPSTREAM_UNAVAILABLE);
        }
    }
}
