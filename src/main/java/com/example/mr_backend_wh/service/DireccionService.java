// src/main/java/com/example/mr_backend_wh/service/DireccionService.java
package com.example.mr_backend_wh.service;

import com.example.mr_backend_wh.DTO.ActualizarDireccionDTO;
import com.example.mr_backend_wh.DTO.CrearDireccionDTO;
import com.example.mr_backend_wh.DTO.DireccionDTO;
import com.example.mr_backend_wh.DTO.UsuarioSecureDTO;
import com.example.mr_backend_wh.model.Direccion;
import com.example.mr_backend_wh.model.Usuario;
import com.example.mr_backend_wh.repository.DireccionRepository;
import com.example.mr_backend_wh.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DireccionService {

    @Autowired
    private DireccionRepository direccionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    public Direccion crearDireccionParaUsuarioLoggeado(CrearDireccionDTO dto) {
        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado();

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

    public List<DireccionDTO> verDireccionesUsuarioLoggeado() {
        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado();

        Usuario usuario = usuarioRepository.findTopByNomusuario(usuarioSecure.getNomusuario())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));

        List<Direccion> direcciones = direccionRepository.findAllByUsuarioid(usuario).orElse(List.of());

        return direcciones.stream().map(this::toDTO).toList();
    }

    public DireccionDTO editarDireccionUsuarioLoggeado(Integer id, ActualizarDireccionDTO dto) {
        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado();

        Usuario usuario = usuarioRepository.findTopByNomusuario(usuarioSecure.getNomusuario())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));

        Direccion direccion = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Dirección no encontrada"));

        // Verificar que la dirección pertenece al usuario loggeado
        if (!direccion.getUsuarioid().getId().equals(usuario.getId())) {
            throw new RuntimeException("❌ No tienes permiso para editar esta dirección");
        }

        // Actualizar campos
        if (dto.getNombreDestinatario() != null && !dto.getNombreDestinatario().isBlank()) {
            direccion.setNombreDestinatario(dto.getNombreDestinatario());
        }
        if (dto.getDireccionCalle() != null && !dto.getDireccionCalle().isBlank()) {
            direccion.setDireccionCalle(dto.getDireccionCalle());
        }
        if (dto.getCodigoPostal() != null && !dto.getCodigoPostal().isBlank()) {
            direccion.setCodigoPostal(dto.getCodigoPostal());
        }
        if (dto.getCiudad() != null && !dto.getCiudad().isBlank()) {
            direccion.setCiudad(dto.getCiudad());
        }
        if (dto.getProvincia() != null && !dto.getProvincia().isBlank()) {
            direccion.setProvincia(dto.getProvincia());
        }
        if (dto.getPais() != null && !dto.getPais().isBlank()) {
            direccion.setPais(dto.getPais());
        }
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
            direccion.setTelefono(dto.getTelefono());
        }
        if (dto.getDocumentoId() != null && !dto.getDocumentoId().isBlank()) {
            direccion.setDocumentoId(dto.getDocumentoId());
        }
        if (dto.getTipoDireccion() != null) {
            direccion.setTipoDireccion(dto.getTipoDireccion());
        }

        Direccion actualizada = direccionRepository.save(direccion);
        return toDTO(actualizada);
    }

    public void eliminarDireccionUsuarioLoggeado(Integer id) {
        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado();

        Usuario usuario = usuarioRepository.findTopByNomusuario(usuarioSecure.getNomusuario())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));

        Direccion direccion = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Dirección no encontrada"));

        // Verificar que la dirección pertenece al usuario loggeado
        if (!direccion.getUsuarioid().getId().equals(usuario.getId())) {
            throw new RuntimeException("❌ No tienes permiso para eliminar esta dirección");
        }

        direccionRepository.delete(direccion);
    }

    private DireccionDTO toDTO(Direccion d) {
        return new DireccionDTO(
                d.getId(),
                d.getNombreDestinatario(),
                d.getDireccionCalle(),
                d.getCodigoPostal(),
                d.getCiudad(),
                d.getProvincia(),
                d.getPais(),
                d.getTelefono(),
                d.getDocumentoId(),
                d.getTipoDireccion(),
                d.getFechacreacion()
        );
    }
}
