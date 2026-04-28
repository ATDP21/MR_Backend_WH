package com.example.mr_backend_wh.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UsuarioDTO {
    private Integer id;
    private String nombre;
    private String contraseña;
    private boolean esAdmin;

}
