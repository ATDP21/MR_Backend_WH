package com.example.mr_backend_wh.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuitarraImagenDTO {
    private Integer id;
    private Integer guitarraId;
    private String nombreOriginal;
    private String nombreAlmacenado;
    private String contentType;
    private Long tamano;
    private String rutaPublica;
    private boolean principal;
    private Integer orden;
    private OffsetDateTime fechacreacion;
}
