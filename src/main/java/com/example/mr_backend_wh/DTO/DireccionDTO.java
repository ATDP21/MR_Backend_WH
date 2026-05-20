package com.example.mr_backend_wh.DTO;

import com.example.mr_backend_wh.model.TipoDireccion;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DireccionDTO {
    private Integer id;
    private String nombreDestinatario;
    private String direccionCalle;
    private String codigoPostal;
    private String ciudad;
    private String provincia;
    private String pais;
    private String telefono;
    private String documentoId;
    private TipoDireccion tipoDireccion;
    private OffsetDateTime fechacreacion;
}