package com.kittyp.ai.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.kittyp.ai.dto.EnvironmentDataDto;
import com.kittyp.ai.dto.NutritionistRecommendationRequest;
import com.kittyp.ai.dto.PetNutritionRecommendationDto;
import com.kittyp.ai.model.NutritionRecommendationResponse;
import com.kittyp.ai.prompts.SystemPrompt;
import com.kittyp.ai.util.PromptLoader;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.WeatherResponse;
import com.kittyp.common.service.GoogleService;
import com.kittyp.common.util.Mapper;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiNutritionRecommendationServiceImpl implements AiNutritionRecommendationService {

    private final PromptLoader promptLoader;
    private final Client geminiClient;
    private final Mapper mapper;
    private final GoogleService googleService;
    private final UserDao userDao;
    private final NutritionPlanService nutritionPlanService;

    @Override
    public NutritionRecommendationResponse generateNutritionRecommendation(PetNutritionRecommendationDto petDetailDto,
            EnvironmentDataDto environmentData) {
        try {
            // 1. Load the system prompt
            SystemPrompt systemPrompt = loadPrompt();

            // 2. Build user input JSON (pet profile + environment)
            Map<String, Object> userData = new HashMap<>();
            userData.put("petProfileData", petDetailDto);
            userData.put("environmentalData", environmentData);

            String userJson = mapper.writeValueAsString(userData);

            String fullPrompt = systemPrompt.getContent() + "\n\nUser Data:\n" + userJson +
                    "\n\nRespond with ONLY valid JSON, no markdown code blocks, no explanatory text:";

            // 3. Call Gemini API
            Part part =  Part.builder().text(systemPrompt.getContent()).build();
            Content content = Content.builder().role("model").parts(part).build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .maxOutputTokens(8000)
                    .temperature(0.2F)
                    .systemInstruction(content)
                    .build();

            GenerateContentResponse response = geminiClient.models.generateContent(
                    "gemini-2.5-flash", // or your chosen model
                    fullPrompt,
                    config // optional config
            );

            String aiOutput = response.text();

            String cleanedJson = cleanJsonResponse(aiOutput);

            // 5. Validate JSON format before returning (optional enterprise safeguard)
            mapper.validateStringJson(cleanedJson); // will throw if invalid JSON

            return mapper.readValueFromString(cleanedJson, NutritionRecommendationResponse.class);

        } catch (Exception e) {
            throw new CustomException("Failed to generate nutrition recommendation", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private SystemPrompt loadPrompt() {
        return promptLoader.loadPrompt("AiNurittionistPrompt.json");
    }

    @Override
    public NutritionRecommendationResponse getNutritionRecommendationRequest(
            NutritionistRecommendationRequest nutritionistRecommendationRequest,
            HttpServletRequest httpServletRequest) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userDao.userByEmail(email);

        WeatherResponse weatherResponse = googleService.getCurrentWeather(
                nutritionistRecommendationRequest.getLocation().getLatitude(),
                nutritionistRecommendationRequest.getLocation().getLongitude());

        EnvironmentDataDto environmentDataDto = new EnvironmentDataDto();
        environmentDataDto.setTemperature(weatherResponse.getTemperature().getDegrees());
        environmentDataDto.setUnit(weatherResponse.getTemperature().getUnit());
        environmentDataDto.setHumidity(weatherResponse.getRelativeHumidity());
        environmentDataDto.setWeatherCondition(weatherResponse.getWeatherCondition().getDescription().getText());
        environmentDataDto.setWindSpeed(weatherResponse.getWind().getSpeed().getValue());
        environmentDataDto.setWindUnit(weatherResponse.getWind().getSpeed().getUnit());
        environmentDataDto.setUvIndex(weatherResponse.getUvIndex());
        environmentDataDto.setPrecipitation(weatherResponse.getPrecipitation().getQpf().getQuantity());

        PetNutritionRecommendationDto petProfile = nutritionistRecommendationRequest.getPetProfile();

        NutritionRecommendationResponse response = generateNutritionRecommendation(
                nutritionistRecommendationRequest.getPetProfile(), environmentDataDto);
        nutritionPlanService.saveNutritionPlanAsync(petProfile.getUuid(), user.getUuid(), response, environmentDataDto,
                petProfile.getName() + "'s Nutrition Plan"+"-"+LocalDate.now());
        return response;
    }

    private String cleanJsonResponse(String aiOutput) {
        if (aiOutput == null || aiOutput.trim().isEmpty()) {
            throw new IllegalArgumentException("AI response is null or empty");
        }

        String cleaned = aiOutput.trim();

        // Remove markdown code blocks if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove ```json
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove ```
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // Remove any leading/trailing whitespace again
        cleaned = cleaned.trim();

        // Check if it starts and ends with proper JSON braces
        if (!cleaned.startsWith("{")) {
            // Try to find the first { and start from there
            int firstBrace = cleaned.indexOf('{');
            if (firstBrace != -1) {
                cleaned = cleaned.substring(firstBrace);
            } else {
                throw new IllegalArgumentException("No valid JSON object found in response");
            }
        }

        if (!cleaned.endsWith("}")) {
            // Try to find the last } and end there
            int lastBrace = cleaned.lastIndexOf('}');
            if (lastBrace != -1) {
                cleaned = cleaned.substring(0, lastBrace + 1);
            } else {
                throw new IllegalArgumentException("No valid JSON object found in response");
            }
        }

        return cleaned;
    }

}
