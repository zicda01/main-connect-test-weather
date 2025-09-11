package com.example.main_connect_test_weather.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import com.example.main_connect_test_weather.client.WeatherDto.WeatherResponse;

@Component
public class WeatherClient {
    private final WebClient webClient;
    private final String apiKey;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public WeatherClient(
            WebClient.Builder webClientBuilder,
            @Value("${weather.api.key}") String apiKey,
            @Value("${weather.api.url}") String baseUrl
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)  // <--- baseUrl 반영
                .build();
        this.apiKey = apiKey;
    }

    private String getFormattedTime(LocalDateTime now) {
        LocalDateTime baseDateTime = now.minusMinutes(10);

        // 계산된 시각을 "HHmm" 형식으로 변환하여 반환
        return baseDateTime.format(DateTimeFormatter.ofPattern("HH00"));
    }

    private String getFormattedDate(LocalDateTime now, String baseTime) {
        // base_time이 "2300"이고, 현재 시간이 02시 10분 이전인 경우에만 날짜를 하루 뺍니다.
        int nowHour = now.getHour();
        int nowMinute = now.getMinute();
        int currentTime = nowHour * 100 + nowMinute;

        if ("2300".equals(baseTime) && currentTime < 10) {
            return now.minusDays(1).format(DATE_FORMATTER);
        }
        return now.format(DATE_FORMATTER);
    }

    public Mono<WeatherResponse> getCurrentWeather(String nx, String ny) {
        LocalDateTime now = LocalDateTime.now();
        String baseTime = getFormattedTime(now);
        String baseDate = getFormattedDate(now, baseTime);

        // API 키를 직접 URL 인코딩
        String encodedApiKey;
        try {
            encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return Mono.error(new RuntimeException("API Key encoding failed", e));
        }

        // 초단기 실활 API 호출
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/VilageFcstInfoService_2.0/getUltraSrtNcst")
                        .queryParam("pageNo", "1")
                        .queryParam("numOfRows", "1000")
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", nx)
                        .queryParam("ny", ny)
                        .queryParam("authKey", encodedApiKey)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class) // 에러 응답 본문을 포함시키기 위해 수정
                                .flatMap(errorBody -> Mono.error(new WebClientResponseException(
                                        "API error with body: " + errorBody,
                                        clientResponse.statusCode().value(),
                                        null, null, null, null
                                )))
                )
                .bodyToMono(WeatherResponse.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    System.err.println("API 호출 실패. 상태 코드: " + e.getStatusCode());
                    System.err.println("응답 본문: " + e.getResponseBodyAsString());
                    return Mono.empty();
                });
    }
}

