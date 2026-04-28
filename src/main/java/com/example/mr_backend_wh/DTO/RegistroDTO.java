package com.example.mr_backend_wh.DTO;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroDTO {
  private String nombreUsuario;
  private String password;
  private String email;
  private String nombreCompleto;
}
