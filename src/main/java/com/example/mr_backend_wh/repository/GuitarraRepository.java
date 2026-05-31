package com.example.mr_backend_wh.repository;

import com.example.mr_backend_wh.model.Guitarra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuitarraRepository extends JpaRepository<Guitarra, Integer> {


    Optional<List<Guitarra>> findGuitarraByEstado(String estado);

    Optional<Guitarra> findById(Integer number);
}
