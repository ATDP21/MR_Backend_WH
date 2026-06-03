package com.example.mr_backend_wh.repository;

import com.example.mr_backend_wh.model.GuitarraImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuitarraImagenRepository extends JpaRepository<GuitarraImagen, Integer> {

    List<GuitarraImagen> findByGuitarra_IdOrderByPrincipalDescOrdenAscIdAsc(Integer guitarraId);

    Optional<GuitarraImagen> findFirstByGuitarra_IdAndPrincipalTrue(Integer guitarraId);

    Optional<GuitarraImagen> findByIdAndGuitarra_Id(Integer id, Integer guitarraId);

    long countByGuitarra_Id(Integer guitarraId);
}
