package com.example.mr_backend_wh.repository;

import com.example.mr_backend_wh.model.Pedido;
import com.example.mr_backend_wh.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    Optional<Pedido> findByStripeSessionId(String stripeSessionId);

    // Devuelve todos los pedidos de un usuario (ordenados por fecha descendente)
    List<Pedido> findAllByUsuarioidOrderByFechaDesc(Usuario usuario);

    // Alternativa por id de usuario (aunque la otra debería funcionar, añado esta por compatibilidad)
    List<Pedido> findAllByUsuarioidIdOrderByFechaDesc(Integer usuarioId);
}
