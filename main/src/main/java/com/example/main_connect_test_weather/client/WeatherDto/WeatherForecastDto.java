package com.example.main_connect_test_weather.client.WeatherDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeatherForecastDto {
    private String fcstDate;    // 예보 날짜
    private String fcstTime;    // 예보 시간
    private double temperature; // 기온
    private double windSpeed;   // 풍속
    private String skyStatus;   // 하늘 상태 (맑음/흐림 등)
    private String precipitationType; // 강수 형태 (비/눈 등)
    private int precipitationProbability; // 강수 확률

    // toString() 메서드를 추가하면 디버깅에 용이합니다.
    @Override
    public String toString() {
        return "WeatherForecastDto{" +
                "fcstDate='" + fcstDate + '\'' +
                ", fcstTime='" + fcstTime + '\'' +
                ", temperature=" + temperature +
                ", windSpeed=" + windSpeed +
                ", skyStatus='" + skyStatus + '\'' +
                ", precipitationType='" + precipitationType + '\'' +
                ", precipitationProbability=" + precipitationProbability +
                '}';
    }
}