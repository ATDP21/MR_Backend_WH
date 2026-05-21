package com.example.mr_backend_wh.repository;

import com.example.mr_backend_wh.model.Pedido;
import com.example.mr_backend_wh.model.StockPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockPedidoRepository extends JpaRepository<StockPedido, Integer> {

}