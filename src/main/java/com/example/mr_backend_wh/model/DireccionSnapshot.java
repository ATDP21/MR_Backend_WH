package com.example.mr_backend_wh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DireccionSnapshot {

    @Column(length = 150)
    private String nombreDestinatario;

    @Column(length = 200)
    private String direccionCalle;

    @Column(length = 20)
    private String codigoPostal;

    @Column(length = 100)
    private String ciudad;

    @Column(length = 100)
    private String provincia;

    @Column(length = 100)
    private String pais;

    @Column(length = 30)
    private String telefono;

    @Column(length = 30)
    private String documentoId;


    public static DireccionSnapshot from(Direccion direccion) {
        if (direccion == null) return null;
        return new DireccionSnapshot(
                direccion.getNombreDestinatario(),
                direccion.getDireccionCalle(),
                direccion.getCodigoPostal(),
                direccion.getCiudad(),
                direccion.getProvincia(),
                direccion.getPais(),
                direccion.getTelefono(),
                direccion.getDocumentoId()
        );
    }
}
