package com.example.mr_backend_wh.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "direccion", schema = "manuel_romero")
public class Direccion {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "usuarioid", nullable = false)
    private Usuario usuarioid;

    @Column(name = "nombre_destinatario", nullable = false, length = 150)
    private String nombreDestinatario;

    @Column(name = "direccion_calle", nullable = false, length = 200)
    private String direccionCalle;

    @Column(name = "codigo_postal", nullable = false, length = 20)
    private String codigoPostal;

    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    @Column(name = "provincia", length = 100)
    private String provincia;

    @Column(name = "pais", nullable = false, length = 100)
    private String pais;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "documento_id", length = 30)
    private String documentoId;

    @Column(name = "tipo_direccion", nullable = false, length = 20)
    private String tipoDireccion;

    @ColumnDefault("now()")
    @Column(name = "fechacreacion")
    private OffsetDateTime fechacreacion;

}