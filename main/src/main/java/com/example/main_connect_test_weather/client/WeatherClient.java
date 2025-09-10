package com.example.main_connect_test_weather.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

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

    // 실제 api 호출 수행시 하드코딩된 더미를 지우고 lat, lon 변수를 직접 전달하는 식으로 메서드가 실행됨.
    public Mono<String> getWeather() {
        LocalDateTime now = LocalDateTime.now();
        String baseTime = getFormattedTime(now);
        String baseDate = getFormattedDate(now, baseTime);

        // 유성구청 좌표 (더미 하드코딩)
        double lat = 36.3622;
        double lon = 127.3568;

        Converter.Grid grid = Converter.convertToGrid(lat, lon);
        String nx = String.valueOf(grid.x);
        String ny = String.valueOf(grid.y);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/VilageFcstInfoService_2.0/getVilageFcst") // 단기 예보 API 경로로 변경
                        .queryParam("serviceKey", apiKey)
                        .queryParam("numOfRows", "1000")
                        .queryParam("pageNo", "1")
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", nx)
                        .queryParam("ny", ny)
                        .build())
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        Mono.error(new WebClientResponseException("API error", clientResponse.statusCode().value(), null, null, null, null)))
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    System.err.println("API call failed: " + e.getMessage());
                    return Mono.empty();
                });
    }
}

