package com.kittyp.user.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PetPhotosDto {
    private List<String> photos;
}
