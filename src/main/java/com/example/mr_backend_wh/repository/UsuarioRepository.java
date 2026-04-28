package com.example.mr_backend_wh.repository;


import com.example.mr_backend_wh.model.Usuario;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Usar consultas derivadas en lugar de nativeQuery para evitar desajustes de nombres de columnas
    Optional<Usuario> findTopByNomusuario(String nomusuario);

    Optional<Usuario> findTopByEmail(String email);

    @Transactional
    @Query(value = "SELECT * FROM manuel_romero.usuario WHERE nomusuario = :nombre", nativeQuery = true)
    Optional<Usuario> findTopByNombre(@Param("nombre") String nombre);

    @Transactional
    @Query(value = "Select * from diccionario.usuario order by puntuacion desc limit 3", nativeQuery = true)
    List<Usuario> findTop3ByOrderByPuntuacionDesc();
}
