// src/main/java/com/example/mr_backend_wh/controller/DireccionController.java
package com.example.mr_backend_wh.controller;

import com.example.mr_backend_wh.DTO.ActualizarDireccionDTO;
import com.example.mr_backend_wh.DTO.CrearDireccionDTO;
import com.example.mr_backend_wh.DTO.DireccionDTO;
import com.example.mr_backend_wh.model.Direccion;
import com.example.mr_backend_wh.service.DireccionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/direccion")
public class DireccionController {

    @Autowired
    private DireccionService direccionService;

    /**
     * Crear una nueva dirección para el usuario autenticado
     */
    @PostMapping("/crear")
    @PreAuthorize("isAuthenticated()")
    public Direccion crear(@RequestBody CrearDireccionDTO dto) {
        return direccionService.crearDireccionParaUsuarioLoggeado(dto);
    }

    /**
     * Ver todas las direcciones del usuario autenticado
     */
    @GetMapping("/ver")
    @PreAuthorize("isAuthenticated()")
    public List<DireccionDTO> ver() {
        return direccionService.verDireccionesUsuarioLoggeado();
    }

    /**
     * Editar una dirección existente del usuario autenticado
     */
    @PutMapping("/editar/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DireccionDTO> editar(
            @PathVariable Integer id,
            @RequestBody ActualizarDireccionDTO dto) {
        try {
            DireccionDTO actualizada = direccionService.editarDireccionUsuarioLoggeado(id, dto);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(null);
        }
    }

    /**
     * Eliminar una dirección del usuario autenticado
     */
    @DeleteMapping("/eliminar/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> eliminar(@PathVariable Integer id) {
        try {
            direccionService.eliminarDireccionUsuarioLoggeado(id);
            return ResponseEntity.ok("Dirección eliminada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}
