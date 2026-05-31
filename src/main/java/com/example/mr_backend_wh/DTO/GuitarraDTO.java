package com.example.mr_backend_wh.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GuitarraDTO {
    private Integer id;
    private String nombre;
    private String estado;
    private BigDecimal precio;
    private String tipoMadera;
    private String tipo;
}
