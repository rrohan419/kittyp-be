package com.kittyp.breed.dto;

import java.util.List;

public record BreedModel(Long id, String species, String name, List<String> aliases) {}
