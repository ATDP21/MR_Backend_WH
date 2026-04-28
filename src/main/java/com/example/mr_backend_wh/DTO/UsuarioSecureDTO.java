package com.example.mr_backend_wh.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UsuarioSecureDTO {
    private String nomusuario;
    private String email;
    private String nombreCompleto;
    private boolean esAdmin;
}
