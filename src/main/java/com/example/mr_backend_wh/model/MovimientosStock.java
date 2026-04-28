package com.example.mr_backend_wh.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

@Entity
@Table(name = "movimientos_stock", schema = "manuel_romero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MovimientosStock {
    @Id
    @ColumnDefault("nextval('manuel_romero.movimientos_stock_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "guitarraid", nullable = false)
    private Guitarra guitarraid;

    @Column(name = "tipomovimiento", nullable = false, length = 20)
    private String tipomovimiento;

    @ColumnDefault("1")
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @ColumnDefault("now()")
    @Column(name = "fecha")
    private OffsetDateTime fecha;

    @Column(name = "nota", length = Integer.MAX_VALUE)
    private String nota;

}