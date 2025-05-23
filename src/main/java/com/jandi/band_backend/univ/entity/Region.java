package com.jandi.band_backend.univ.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "region")
@Getter
@Setter
@NoArgsConstructor
public class Region {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id")
    private Integer id;
    
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;
    
    @Column(name = "name", nullable = false, length = 50)
    private String name;
} 