package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dst_teh_vil")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DstTehVil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dst")
    private String dst;  // District

    @Column(name = "teh")
    private String teh;  // Tehsil

    @Column(name = "vil")
    private String vil;  // Village

}