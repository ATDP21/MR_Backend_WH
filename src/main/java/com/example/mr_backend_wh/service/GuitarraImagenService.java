package com.example.mr_backend_wh.service;

import com.example.mr_backend_wh.DTO.GuitarraImagenDTO;
import com.example.mr_backend_wh.model.Guitarra;
import com.example.mr_backend_wh.model.GuitarraImagen;
import com.example.mr_backend_wh.repository.GuitarraImagenRepository;
import com.example.mr_backend_wh.repository.GuitarraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuitarraImagenService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final GuitarraRepository guitarraRepository;
    private final GuitarraImagenRepository guitarraImagenRepository;

    @Value("${app.uploads.guitarras-dir:uploads/guitarras}")
    private String guitarrasDir;

    @Transactional
    public List<GuitarraImagenDTO> subirImagenes(Integer guitarraId, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar al menos una imagen");
        }

        Guitarra guitarra = guitarraRepository.findById(guitarraId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la guitarra indicada"));

        long ordenBase = guitarraImagenRepository.countByGuitarra_Id(guitarraId);
        boolean hayPrincipal = guitarraImagenRepository.findFirstByGuitarra_IdAndPrincipalTrue(guitarraId).isPresent();
        List<Path> archivosCreados = new ArrayList<>();

        try {
            for (int i = 0; i < archivos.size(); i++) {
                MultipartFile archivo = archivos.get(i);
                GuitarraImagen imagen = guardarArchivo(guitarra, archivo, (int) ordenBase + i, !hayPrincipal && i == 0, archivosCreados);
                guitarraImagenRepository.save(imagen);
            }
        } catch (RuntimeException ex) {
            limpiarArchivos(archivosCreados);
            throw ex;
        }

        return guitarraImagenRepository.findByGuitarra_IdOrderByPrincipalDescOrdenAscIdAsc(guitarraId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GuitarraImagenDTO> obtenerImagenes(Integer guitarraId) {
        return guitarraImagenRepository.findByGuitarra_IdOrderByPrincipalDescOrdenAscIdAsc(guitarraId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public GuitarraImagenDTO marcarComoPrincipal(Integer guitarraId, Integer imagenId) {
        List<GuitarraImagen> imagenes = guitarraImagenRepository.findByGuitarra_IdOrderByPrincipalDescOrdenAscIdAsc(guitarraId);
        if (imagenes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La guitarra no tiene imágenes");
        }

        GuitarraImagen imagenObjetivo = guitarraImagenRepository.findByIdAndGuitarra_Id(imagenId, guitarraId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La imagen no pertenece a esta guitarra"));

        imagenes.forEach(imagen -> imagen.setPrincipal(false));
        imagenObjetivo.setPrincipal(true);
        guitarraImagenRepository.saveAll(imagenes);
        return toDto(guitarraImagenRepository.save(imagenObjetivo));
    }

    @Transactional
    public void eliminarImagen(Integer guitarraId, Integer imagenId) {
        GuitarraImagen imagen = guitarraImagenRepository.findByIdAndGuitarra_Id(imagenId, guitarraId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La imagen no pertenece a esta guitarra"));

        borrarArchivo(guitarraId, imagen);
        boolean eraPrincipal = imagen.isPrincipal();
        guitarraImagenRepository.delete(imagen);

        if (eraPrincipal) {
            guitarraImagenRepository.findByGuitarra_IdOrderByPrincipalDescOrdenAscIdAsc(guitarraId)
                    .stream()
                    .findFirst()
                    .ifPresent(siguiente -> {
                        siguiente.setPrincipal(true);
                        guitarraImagenRepository.save(siguiente);
                    });
        }
    }

    private GuitarraImagen guardarArchivo(Guitarra guitarra, MultipartFile archivo, int orden, boolean principal, List<Path> archivosCreados) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede subir una imagen vacía");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten archivos de imagen");
        }

        String extension = extraerExtension(archivo);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagen no permitido");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        if (!StringUtils.hasText(nombreOriginal)) {
            nombreOriginal = "imagen";
        }

        String nombreAlmacenado = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path directorioGuitarra = obtenerDirectorioGuitarra(guitarra.getId());
        Path destino = directorioGuitarra.resolve(nombreAlmacenado);

        try {
            Files.createDirectories(directorioGuitarra);
            archivo.transferTo(destino);
            archivosCreados.add(destino);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen", ex);
        }

        GuitarraImagen imagen = new GuitarraImagen();
        imagen.setGuitarra(guitarra);
        imagen.setNombreOriginal(nombreOriginal);
        imagen.setNombreAlmacenado(nombreAlmacenado);
        imagen.setContentType(contentType);
        imagen.setTamano(archivo.getSize());
        imagen.setRutaPublica(crearRutaPublica(guitarra.getId(), nombreAlmacenado));
        imagen.setImageUrl(imagen.getRutaPublica());
        imagen.setMainImage(imagen.getRutaPublica());
        imagen.setPrincipal(principal);
        imagen.setOrden(orden);
        return imagen;
    }

    private void limpiarArchivos(List<Path> archivosCreados) {
        for (Path archivo : archivosCreados) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ignored) {
                // Se prioriza no ocultar la causa original del fallo de subida.
            }
        }
    }

    private void borrarArchivo(Integer guitarraId, GuitarraImagen imagen) {
        Path archivo = obtenerDirectorioGuitarra(guitarraId).resolve(imagen.getNombreAlmacenado());
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo borrar la imagen del disco", ex);
        }
    }

    private Path obtenerDirectorioGuitarra(Integer guitarraId) {
        return Paths.get(guitarrasDir).toAbsolutePath().normalize().resolve(String.valueOf(guitarraId));
    }

    private String crearRutaPublica(Integer guitarraId, String nombreAlmacenado) {
        return "/media/guitarras/" + guitarraId + "/" + nombreAlmacenado;
    }

    private String extraerExtension(MultipartFile archivo) {
        String extensionDesdeNombre = StringUtils.getFilenameExtension(archivo.getOriginalFilename());
        if (StringUtils.hasText(extensionDesdeNombre)) {
            return extensionDesdeNombre.toLowerCase(Locale.ROOT);
        }

        String extensionDesdeTipo = CONTENT_TYPE_TO_EXTENSION.get(archivo.getContentType());
        return extensionDesdeTipo != null ? extensionDesdeTipo.toLowerCase(Locale.ROOT) : null;
    }

    private GuitarraImagenDTO toDto(GuitarraImagen imagen) {
        return new GuitarraImagenDTO(
                imagen.getId(),
                imagen.getGuitarra().getId(),
                imagen.getNombreOriginal(),
                imagen.getNombreAlmacenado(),
                imagen.getContentType(),
                imagen.getTamano(),
                imagen.getRutaPublica(),
                imagen.isPrincipal(),
                imagen.getOrden(),
                imagen.getFechacreacion()
        );
    }
}
