package com.example.mr_backend_wh.controller;

import com.example.mr_backend_wh.DTO.GuitarraDTO;
import com.example.mr_backend_wh.DTO.GuitarraImagenDTO;
import com.example.mr_backend_wh.model.Guitarra;
import com.example.mr_backend_wh.service.GuitarraService;
import com.example.mr_backend_wh.service.GuitarraImagenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/guitarra")
public class GuitarraController {

    @Autowired
    private GuitarraService guitarraService;

    @Autowired
    private GuitarraImagenService guitarraImagenService;


    @PostMapping("/crear")
    public Guitarra crearGuitarra(@RequestBody GuitarraDTO guitarraDTO){

        return guitarraService.crearGuitarra(guitarraDTO);
    }
    @GetMapping("/verStock")
    public Optional<List<Guitarra>> verGuitarras(){
        return guitarraService.obtenerGuitarrasEnStock();
    }
    @GetMapping("/verCarrito")
    public List<Guitarra> verCarrito(@RequestBody List<Integer> ids) {
        return guitarraService.obtenerGuitarrasPorIds(ids);
    }
    @GetMapping("/ver/{id}")
    public Optional<Guitarra> verGuitarra(@PathVariable Integer id){
        return guitarraService.obtenerGuitarraPorId(id);
    }

    @PostMapping(value = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<GuitarraImagenDTO> subirImagenes(
            @PathVariable Integer id,
            @RequestParam("files") List<MultipartFile> files
    ) {
        return guitarraImagenService.subirImagenes(id, files);
    }

    @GetMapping("/{id}/imagenes")
    public List<GuitarraImagenDTO> verImagenes(@PathVariable Integer id) {
        return guitarraImagenService.obtenerImagenes(id);
    }

    @PutMapping("/{guitarraId}/imagenes/{imagenId}/principal")
    public GuitarraImagenDTO marcarComoPrincipal(@PathVariable Integer guitarraId, @PathVariable Integer imagenId) {
        return guitarraImagenService.marcarComoPrincipal(guitarraId, imagenId);
    }

    @DeleteMapping("/{guitarraId}/imagenes/{imagenId}")
    public void eliminarImagen(@PathVariable Integer guitarraId, @PathVariable Integer imagenId) {
        guitarraImagenService.eliminarImagen(guitarraId, imagenId);
    }

}
