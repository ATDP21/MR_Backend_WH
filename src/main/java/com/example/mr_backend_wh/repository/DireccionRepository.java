package com.example.mr_backend_wh.repository;

import com.example.mr_backend_wh.model.Direccion;
import com.example.mr_backend_wh.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DireccionRepository extends JpaRepository<Direccion, Integer> {
    Optional<List<Direccion>> findAllByUsuarioid(Usuario usuario);

    Direccion getDireccionById(Integer id);
}
