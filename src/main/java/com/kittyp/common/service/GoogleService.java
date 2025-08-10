package com.kittyp.common.service;

import com.kittyp.common.model.WeatherResponse;

public interface GoogleService {
    
    WeatherResponse getCurrentWeather(String latitude, String longitude);
}
