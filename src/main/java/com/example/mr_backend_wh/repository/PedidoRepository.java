package com.example.mr_backend_wh.repository;

import com.example.mr_backend_wh.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    Optional<Pedido> findByStripeSessionId(String stripeSessionId);
}
