// src/main/java/com/example/mr_backend_wh/controller/DireccionController.java
package com.example.mr_backend_wh.controller;

import com.example.mr_backend_wh.DTO.CrearDireccionDTO;
import com.example.mr_backend_wh.DTO.DireccionDTO;
import com.example.mr_backend_wh.model.Direccion;
import com.example.mr_backend_wh.service.DireccionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/direccion")
public class DireccionController {

    @Autowired
    private DireccionService direccionService;

    @PostMapping("/crear")
    public Direccion crear(@RequestBody CrearDireccionDTO dto) {
        return direccionService.crearDireccionParaUsuarioLoggeado(dto);
    }

    @GetMapping("/ver")
    public List<DireccionDTO> ver() {
        return direccionService.verDireccionesUsuarioLoggeado();
    }
}
