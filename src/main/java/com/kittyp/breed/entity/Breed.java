package com.kittyp.breed.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "breed_master")
@Getter
@Setter
public class Breed extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String species;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, columnDefinition = "text[]")
    @ColumnDefault("ARRAY[]::text[]")
    private List<String> aliases = new ArrayList<>();
}
