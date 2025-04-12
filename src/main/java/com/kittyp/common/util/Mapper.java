/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.modelmapper.ConfigurationException;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;

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
	
	public <T> T convert(Object srcObj, Class<T> targetClass) {
		try {

			return modelMapper.map(srcObj, targetClass);

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
}
