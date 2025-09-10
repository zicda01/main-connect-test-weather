package com.example.main_connect_test_weather.service;

import com.example.main_connect_test_weather.client.WeatherClient;
import com.example.main_connect_test_weather.client.WeatherDto.WeatherForecastDto;
import com.example.main_connect_test_weather.client.WeatherDto.WeatherItem;
import com.example.main_connect_test_weather.client.WeatherDto.WeatherResponse;
import com.example.main_connect_test_weather.utils.GeoConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

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
    public Mono<WeatherForecastDto> getCurrentWeather(double lat, double lon) {
        GeoConverter.Grid grid = geoConverter.convertToGrid(lat, lon);
        String nx = String.valueOf(grid.x);
        String ny = String.valueOf(grid.y);

        return weatherClient.getCurrentWeather(nx, ny)
                .map(this::processWeatherResponse);
    }

    private WeatherForecastDto processWeatherResponse(WeatherResponse weatherResponse) {
        WeatherForecastDto weatherForecastDto = new WeatherForecastDto();

        List<WeatherItem> items = weatherResponse.getResponse().getBody().getItems().getItem();

        // 날씨 데이터를 key-value 형태로 가공
        var weatherDataMap = items.stream()
                .collect(Collectors.toMap(WeatherItem::getCategory, WeatherItem::getFcstValue));

        // DTO 필드에 데이터 매핑
        weatherForecastDto.setFcstDate(items.get(0).getFcstDate());
        weatherForecastDto.setFcstTime(items.get(0).getFcstTime());
        weatherForecastDto.setTemperature(Double.parseDouble(weatherDataMap.getOrDefault("TMP", "0.0")));
        weatherForecastDto.setWindSpeed(Double.parseDouble(weatherDataMap.getOrDefault("WSD", "0.0")));
        weatherForecastDto.setSkyStatus(convertSkyStatus(weatherDataMap.getOrDefault("SKY", "0")));
        weatherForecastDto.setPrecipitationType(convertPrecipitationType(weatherDataMap.getOrDefault("PTY", "0")));
        weatherForecastDto.setPrecipitationProbability(Integer.parseInt(weatherDataMap.getOrDefault("POP", "0")));

        // 디버그 코드 추가
        System.out.println("가공된 최종 데이터: " + weatherForecastDto.toString());

        return weatherForecastDto;
    }

    private String convertPrecipitationType(String code) {
        return switch (code) {
            case "0" -> "강수 없음";
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            default -> "알 수 없음";
        };
    }

    private String convertSkyStatus(String code) {
        return switch (code) {
            case "1" -> "맑음";
            case "3" -> "구름 많음";
            case "4" -> "흐림";
            default -> "알 수 없음";
        };
    }

}
