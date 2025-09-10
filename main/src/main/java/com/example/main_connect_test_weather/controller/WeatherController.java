package com.example.main_connect_test_weather.controller;

import com.example.main_connect_test_weather.service.WeatherService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


/**
 * 날씨 정보 API 요청을 처리하는 restful 컨트롤러입니다.
 */
@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService weatherService;

    @Autowired
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public Mono<String> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lon) {

        return weatherService.getCurrentWeather(lat, lon);
    }
}