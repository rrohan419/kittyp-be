/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.modelmapper.ConfigurationException;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.modelmapper.config.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.ai.prompts.SystemPrompt;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Component
@RequiredArgsConstructor
public class Mapper {

	private final ModelMapper modelMapper;
	private final ObjectMapper objectMapper;
	private final Environment env;

	@PostConstruct
	public void init() {
		modelMapper.addConverter(ctx -> {
			Collection<?> source = ctx.getSource();
			return source == null ? null : new HashSet<>(source);
		}, Collection.class, Set.class);

		modelMapper.getConfiguration()
				.setAmbiguityIgnored(true)
				.setSkipNullEnabled(true)
				.setFieldMatchingEnabled(true)
				.setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);
	}

	public <T> T convert(Object srcObj, Class<T> targetClass) {
		try {
			return modelMapper.map(srcObj, targetClass);

		} catch (IllegalArgumentException argumentException) {

			throw new CustomException(env.getProperty(ExceptionConstant.SOURCE_OR_DESTINATION_IS_NULL));

		} catch (MappingException | ConfigurationException eRuntimeException) {

			throw new CustomException(eRuntimeException.getMessage());
		}
	}

	public void map(Object srcObj, Object destObj) {
		try {
			modelMapper.map(srcObj, destObj);

		} catch (IllegalArgumentException argumentException) {

			throw new CustomException(env.getProperty(ExceptionConstant.SOURCE_OR_DESTINATION_IS_NULL));

		} catch (MappingException | ConfigurationException eRuntimeException) {

			throw new CustomException(eRuntimeException.getMessage());
		}
	}

	public <S, T> List<T> convertToList(List<S> srcList, Class<T> targetClass) {
		List<T> response = new ArrayList<>();

		if (srcList != null) {
			srcList.stream().forEach(source -> response.add(convert(source, targetClass)));
		}

		return response;
	}

	public <T> T convertJsonToObejct(JSONObject orderJsonObject, Class<T> targetClass) {
		try {
			// Convert the JSONObject to a Map
			Map<String, Object> orderMap = objectMapper.readValue(orderJsonObject.toString(),
					new TypeReference<Map<String, Object>>() {
					});
			// Convert the Map to CreateOrderModel
			return convert(orderMap, targetClass);
		} catch (JsonProcessingException ex) {
			throw new CustomException("Error converting Order JSONObject to CreateOrderModel: " + ex.getMessage());
		}
	}

	public Map<String, String> convertStringToHashMap(String jsonString) {
		Map<String, String> map = new HashMap<>();
		try {
			map = objectMapper.readValue(jsonString, new TypeReference<Map<String, String>>() {
			});
		} catch (JsonProcessingException ex) {
			throw new CustomException(ex.getMessage());
		}
		return map;
	}

	public String convertObjectToJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new CustomException("Error converting object to JSON string: " + e.getMessage());
		}
	}

	public <T> List<T> convertJsonToList(String jsonString, Class<T> elementClass) {
    try {
        return objectMapper.readValue(
            jsonString,
            objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass)
        );
    } catch (JsonProcessingException e) {
        throw new CustomException("Error converting JSON string to List<" + elementClass.getSimpleName() + ">: " + e.getMessage());
    }
}

	public <T> T readValueFromStream(InputStream inputStream, Class<T> targetClass) {
		try {
			return objectMapper.readValue(inputStream, targetClass);
		} catch (IOException ex) {
			throw new CustomException(ex.getMessage());
		}
	}

	public <T> T readValueFromString(String inputString, Class<T> targetClass) {
		try {
			return objectMapper.readValue(inputString, targetClass);
		} catch (IOException ex) {
			throw new CustomException(ex.getMessage());
		}
	}

	public String writeValueAsString(Map<String, Object> mapData) {
		try {
			return objectMapper.writeValueAsString(mapData);
		} catch (IOException ex) {
			throw new CustomException(ex.getMessage());
		}
	}

	public void validateStringJson(String jsonData) {
		try {
			objectMapper.readTree(jsonData);
		} catch (Exception ex) {
			throw new CustomException(ex.getMessage());
		}
	}


}
