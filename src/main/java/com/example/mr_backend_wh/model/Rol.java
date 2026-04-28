package com.example.mr_backend_wh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "rol", schema = "manuel_romero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Rol {
    @Id
    @ColumnDefault("nextval('manuel_romero.roles_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "rol", nullable = false, length = 50)
    private String rol;

}