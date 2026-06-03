package com.example.mr_backend_wh.service;

import com.example.mr_backend_wh.DTO.GuitarraDTO;
import com.example.mr_backend_wh.model.Guitarra;
import com.example.mr_backend_wh.repository.GuitarraRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class GuitarraService {

    private final GuitarraRepository guitarraRepository;


    public Guitarra crearGuitarra(GuitarraDTO guitarraDTO) {
        Guitarra guitarra = new Guitarra();
        guitarra.setNumserie(guitarraDTO.getNumserie());
        guitarra.setEstado(guitarraDTO.getEstado());
        guitarra.setPrecio(guitarraDTO.getPrecio());
        guitarra.setTipomadera(guitarraDTO.getTipoMadera());
        guitarra.setTipo(guitarraDTO.getTipo());

        return guitarraRepository.save(guitarra);
    }
    public Optional<Guitarra> obtenerGuitarraPorId(Integer id) {
        return guitarraRepository.findById(id);
    }
    public Optional<List<Guitarra>> obtenerGuitarrasEnStock() {
        return guitarraRepository.findGuitarraByEstado("Stock");
    }

    public List<Guitarra> obtenerGuitarrasPorIds(List<Integer> ids) {
        return guitarraRepository.findAllById(ids);
    }
}
