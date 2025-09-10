package com.example.main_connect_test_weather.service;

import com.example.main_connect_test_weather.client.WeatherClient;
import com.example.main_connect_test_weather.utils.GeoConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WeatherService {
    private final WeatherClient weatherClient;
    private final GeoConverter geoConverter;

    /**
     * 의존성 주입: WeatherClient와 Converter 빈을 주입받습니다.
     * @param weatherClient 날씨 API 호출을 담당하는 클라이언트
     * @param geoConverter 위도/경도 좌표를 격자 좌표로 변환하는 컴포넌트
     */

    @Autowired
    public WeatherService(WeatherClient weatherClient, GeoConverter geoConverter) {
        this.weatherClient = weatherClient;
        this.geoConverter = geoConverter;
    }

    /**
     * 위도와 경도를 받아 실시간 날씨 정보를 조회합니다.
     * @param lat 위도
     * @param lon 경도
     * @return API 응답을 담고 있는 Mono<String>
     */
    public Mono<String> getCurrentWeather(double lat, double lon) {
        // Converter를 사용해 위도/경도를 격자 좌표(Grid)로 변환
        GeoConverter.Grid grid = geoConverter.convertToGrid(lat, lon);

        // nx, ny를 String으로 변환
        String nx = String.valueOf(grid.x);
        String ny = String.valueOf(grid.y);

        // 변환된 좌표를 Client에 전달하여 API 호출
        return weatherClient.getCurrentWeather(nx, ny);
    }

}
