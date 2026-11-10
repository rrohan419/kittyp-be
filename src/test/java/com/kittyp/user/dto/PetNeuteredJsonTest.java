package com.kittyp.user.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.util.Mapper;
import com.kittyp.user.dao.PetDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.models.PetModel;
import com.kittyp.user.service.PetServiceImpl;

class PetNeuteredJsonTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsBothNeuteredPropertyNamesForPetRequests() throws Exception {
        assertTrue(objectMapper.readValue("{\"name\":\"Milo\",\"isNeutered\":true}",
                PetDetailDto.class).isNeutered());
        assertTrue(objectMapper.readValue("{\"name\":\"Milo\",\"neutered\":true}",
                PetDetailDto.class).isNeutered());
        assertFalse(objectMapper.readValue("{\"name\":\"Milo\",\"isNeutered\":false}",
                PetDetailDto.class).isNeutered());
    }

    @Test
    void serializesNeuteredUsingTheFrontendPropertyName() throws Exception {
        PetModel pet = new PetModel();
        pet.setNeutered(true);
        var json = objectMapper.readTree(objectMapper.writeValueAsString(pet));
        assertTrue(json.get("isNeutered").asBoolean());
        assertFalse(json.has("neutered"));
        assertEquals(1, json.properties().stream().filter(entry ->
                entry.getKey().toLowerCase().contains("neutered")).count());
    }

    @Test
    void addAndUpdatePreserveTheSelectedStatus() throws Exception {
        PetDetailDto request = objectMapper.readValue("{\"name\":\"Milo\",\"isNeutered\":true}",
                PetDetailDto.class);
        ModelMapper modelMapper = new ModelMapper();
        Pet newPet = modelMapper.map(request, Pet.class);
        assertTrue(newPet.isNeutered());

        Pet existing = Pet.builder().uuid("pet-1").name("Milo").build();
        User owner = User.builder().email("owner@example.com").build();
        owner.setPets(List.of(existing));
        PetDao petDao = mock(PetDao.class);
        UserDao userDao = mock(UserDao.class);
        Mapper mapper = mock(Mapper.class);
        when(userDao.userByEmail("owner@example.com")).thenReturn(owner);
        when(petDao.savePets(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.convert(any(Pet.class), org.mockito.ArgumentMatchers.eq(PetModel.class)))
                .thenAnswer(invocation -> modelMapper.map(invocation.getArgument(0), PetModel.class));

        PetModel updated = new PetServiceImpl(petDao, mapper, userDao, null)
                .updatePet(request, "pet-1", "owner@example.com");
        assertTrue(existing.isNeutered());
        assertTrue(updated.isNeutered());

        PetDetailDto falseRequest = objectMapper.readValue("{\"name\":\"Milo\",\"isNeutered\":false}",
                PetDetailDto.class);
        assertFalse(new PetServiceImpl(petDao, mapper, userDao, null)
                .updatePet(falseRequest, "pet-1", "owner@example.com").isNeutered());
    }
}
