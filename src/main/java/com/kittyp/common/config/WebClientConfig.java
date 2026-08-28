// package com.kittyp.common.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.reactive.function.client.ExchangeStrategies;
// import org.springframework.web.reactive.function.client.WebClient;

// @Configuration
// public class WebClientConfig {

//     @Bean
//     public WebClient webClient(WebClient.Builder builder) {
//         // Increase memory limit for large streams if needed
//         ExchangeStrategies strategies = ExchangeStrategies.builder()
//             .codecs(config -> config.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
//             .build();

//         return builder
//             .exchangeStrategies(strategies)
//             .build();
//     }
// }
