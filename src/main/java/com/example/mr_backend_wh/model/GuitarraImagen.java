package com.example.mr_backend_wh.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "guitarra_imagen", schema = "manuel_romero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuitarraImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guitarraid", nullable = false)
    @JsonIgnore
    private Guitarra guitarra;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", nullable = false, length = 255, unique = true)
    private String nombreAlmacenado;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "tamano", nullable = false)
    private Long tamano;

    @Column(name = "ruta_publica", nullable = false, length = 500)
    private String rutaPublica;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "main_image", nullable = false, length = 500)
    private String mainImage;

    @Column(name = "principal", nullable = false)
    private boolean principal;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @CreationTimestamp
    @Column(name = "fechacreacion", nullable = false, updatable = false)
    private OffsetDateTime fechacreacion;

    @UpdateTimestamp
    @Column(name = "ultimamodificacion")
    private OffsetDateTime ultimamodificacion;
}
