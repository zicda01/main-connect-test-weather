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

    private static final List<String> BASE_TIMES = List.of(
            "0200", "0500", "0800", "1100",
            "1400", "1700", "2000", "2300"
    );

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
        int nowHour = now.getHour();
        int nowMinute = now.getMinute();

        // 현재 시간을 'HHmm' 형식의 정수로 변환
        int currentTime = nowHour * 100 + nowMinute;

        // BASE_TIMES 리스트를 역순으로 순회하며 가장 최신 발표 시간 찾기
        for (int i = BASE_TIMES.size() - 1; i >= 0; i--) {
            int baseTime = Integer.parseInt(BASE_TIMES.get(i));
            // 발표 시각 10분 이후부터 데이터 호출 가능
            if (currentTime >= (baseTime + 10)) {
                return BASE_TIMES.get(i);
            }
        }

        // 위 반복문에서 해당하는 발표 시각을 찾지 못한 경우
        // (예: 현재 시간이 00:00 ~ 02:09)
        // 전날의 가장 늦은 시간인 "2300"을 반환
        return "2300";
    }

    private String getFormattedDate(LocalDateTime now, String baseTime) {
        // base_time이 "2300"이고, 현재 시간이 02시 10분 이전인 경우에만 날짜를 하루 뺍니다.
        int nowHour = now.getHour();
        int nowMinute = now.getMinute();
        int currentTime = nowHour * 100 + nowMinute;

        if ("2300".equals(baseTime) && currentTime < 210) {
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

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/VilageFcstInfoService_2.0/getVilageFcst")
                        .queryParam("authKey", encodedApiKey)
                        .queryParam("numOfRows", "10")
                        .queryParam("pageNo", "1")
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", nx)
                        .queryParam("ny", ny)
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

