package com.kittyp.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.model.WeatherResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleServiceImpl implements GoogleService {

    private final RestClient restClient;
    private final Environment env;

    @Value("${google.api.key}")
    private String apiKey;

    @Override
    public WeatherResponse getCurrentWeather(String latitude, String longitude) {
        
        String uri = String.format(env.getProperty(AppConstant.GOOGLE_WEATHER_API_URL), apiKey, latitude, longitude);

       return restClient.get()
                .uri(uri)
                .retrieve()
                .body(WeatherResponse.class);
    }
    
}
