package com.example.mr_backend_wh.DTO;

import com.example.mr_backend_wh.model.TipoDireccion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarDireccionDTO {
    private String nombreDestinatario;
    private String direccionCalle;
    private String codigoPostal;
    private String ciudad;
    private String provincia;
    private String pais;
    private String telefono;
    private String documentoId;
    private TipoDireccion tipoDireccion;
}

