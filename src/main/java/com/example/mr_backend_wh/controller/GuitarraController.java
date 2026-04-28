package com.example.mr_backend_wh.controller;

import com.example.mr_backend_wh.DTO.GuitarraDTO;
import com.example.mr_backend_wh.model.Guitarra;
import com.example.mr_backend_wh.service.GuitarraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/guitarra")
public class GuitarraController {

    @Autowired
    private GuitarraService guitarraService;



    @PostMapping("/crear")
    public Guitarra crearGuitarra(@RequestBody GuitarraDTO guitarraDTO){

        return guitarraService.crearGuitarra(guitarraDTO);
    }
    @GetMapping("/verStock")
    public Optional<List<Guitarra>> verGuitarras(){
        return guitarraService.obtenerGuitarrasEnStock();
    }
    @GetMapping("/verCarrito")
    public List<Guitarra> verCarrito(@RequestBody List<Long> ids) {
        return guitarraService.obtenerGuitarrasPorIds(ids);
    }
    @GetMapping("/ver/{numserie}")
    public Optional<Guitarra> verGuitarra(@PathVariable String numserie){
        return guitarraService.obtenerGuitarraPorNumserie(numserie);
    }

}
