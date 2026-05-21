package com.example.mr_backend_wh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "pedido", schema = "manuel_romero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuarioid", nullable = false)
    private Usuario usuarioid;

    @CreationTimestamp
    @Column(name = "fecha")
    private OffsetDateTime fecha;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "nombreDestinatario", column = @Column(name = "entrega_nombre_destinatario", length = 150)),
            @AttributeOverride(name = "direccionCalle", column = @Column(name = "entrega_direccion_calle", length = 200)),
            @AttributeOverride(name = "codigoPostal", column = @Column(name = "entrega_codigo_postal", length = 20)),
            @AttributeOverride(name = "ciudad", column = @Column(name = "entrega_ciudad", length = 100)),
            @AttributeOverride(name = "provincia", column = @Column(name = "entrega_provincia", length = 100)),
            @AttributeOverride(name = "pais", column = @Column(name = "entrega_pais", length = 100)),
            @AttributeOverride(name = "telefono", column = @Column(name = "entrega_telefono", length = 30)),
            @AttributeOverride(name = "documentoId", column = @Column(name = "entrega_documento_id", length = 30))
    })
    private DireccionSnapshot direccionEntrega;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "nombreDestinatario", column = @Column(name = "fact_nombre_destinatario", length = 150)),
            @AttributeOverride(name = "direccionCalle", column = @Column(name = "fact_direccion_calle", length = 200)),
            @AttributeOverride(name = "codigoPostal", column = @Column(name = "fact_codigo_postal", length = 20)),
            @AttributeOverride(name = "ciudad", column = @Column(name = "fact_ciudad", length = 100)),
            @AttributeOverride(name = "provincia", column = @Column(name = "fact_provincia", length = 100)),
            @AttributeOverride(name = "pais", column = @Column(name = "fact_pais", length = 100)),
            @AttributeOverride(name = "telefono", column = @Column(name = "fact_telefono", length = 30)),
            @AttributeOverride(name = "documentoId", column = @Column(name = "fact_documento_id", length = 30))
    })
    private DireccionSnapshot direccionFacturacion;

    @OneToMany(mappedBy = "pedidoid")
    private List<StockPedido> listaProductos;
}