// src/main/java/com/example/mr_backend_wh/service/DireccionService.java
package com.example.mr_backend_wh.service;

import com.example.mr_backend_wh.DTO.CrearDireccionDTO;
import com.example.mr_backend_wh.DTO.UsuarioSecureDTO;
import com.example.mr_backend_wh.model.Direccion;
import com.example.mr_backend_wh.model.Usuario;
import com.example.mr_backend_wh.repository.UsuarioRepository;
import com.example.mr_backend_wh.repository.DireccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class DireccionService {

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    public Direccion crearDireccionParaUsuarioLoggeado(CrearDireccionDTO dto) {
        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado(); // debe existir en tu UsuarioService
        Usuario usuario = usuarioRepository.findTopByNomusuario(usuarioSecure.getNomusuario())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));
        Direccion d = new Direccion();
        d.setUsuarioid(usuario);
        d.setNombreDestinatario(dto.getNombreDestinatario());
        d.setDireccionCalle(dto.getDireccionCalle());
        d.setCodigoPostal(dto.getCodigoPostal());
        d.setCiudad(dto.getCiudad());
        d.setProvincia(dto.getProvincia());
        d.setPais(dto.getPais());
        d.setTelefono(dto.getTelefono());
        d.setDocumentoId(dto.getDocumentoId());
        d.setTipoDireccion(dto.getTipoDireccion());
        d.setFechacreacion(OffsetDateTime.now());

        return direccionRepository.save(d);
    }
}
