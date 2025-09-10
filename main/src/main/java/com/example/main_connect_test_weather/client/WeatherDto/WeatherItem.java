package com.example.main_connect_test_weather.client.WeatherDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeatherItem {
    @JsonProperty("baseDate")
    private String baseDate;
    @JsonProperty("baseTime")
    private String baseTime;
    @JsonProperty("category")
    private String category;
    @JsonProperty("fcstDate")
    private String fcstDate;
    @JsonProperty("fcstTime")
    private String fcstTime;
    @JsonProperty("fcstValue")
    private String fcstValue;
    @JsonProperty("nx")
    private int nx;
    @JsonProperty("ny")
    private int ny;
}