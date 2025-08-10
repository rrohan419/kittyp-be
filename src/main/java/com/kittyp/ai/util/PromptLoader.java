package com.kittyp.ai.util;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;

import com.kittyp.ai.prompts.SystemPrompt;
import com.kittyp.common.util.Mapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromptLoader {
    
    private final Mapper mapper;

    public SystemPrompt loadPrompt(String filePath) {
        try (InputStream inputStream = PromptLoader.class.getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Prompt file not found: " + filePath);
            }
            return mapper.readValueFromStream(inputStream, SystemPrompt.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt file: " + filePath, e);
        }
    }
}
