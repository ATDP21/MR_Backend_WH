package com.example.mr_backend_wh.controller;



import com.example.mr_backend_wh.DTO.UsuarioSecureDTO;
import com.example.mr_backend_wh.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

  @Autowired
  private UsuarioService perfilService;

  @GetMapping("/loggeado")
  public UsuarioSecureDTO obtenerPerfilLoggeado() {
    return perfilService.obtenerPerfilUsuarioLoggeado();
  }

}

