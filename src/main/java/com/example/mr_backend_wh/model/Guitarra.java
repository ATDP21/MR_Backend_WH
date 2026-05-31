package com.example.mr_backend_wh.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "guitarra", schema = "manuel_romero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Guitarra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "numserie", nullable = false, length = 50)
    private String numserie;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "tipomadera", nullable = false, length = 50)
    private String tipomadera;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @CreationTimestamp
    @Column(name = "fechacreacion")
    private OffsetDateTime fechacreacion;

    @CreationTimestamp
    @Column(name = "ultimamodificacion")
    private OffsetDateTime ultimamodificacion;

}