package com.example.mr_backend_wh.controller;

import com.example.mr_backend_wh.DTO.PedidoCrearRequestDTO;
import com.example.mr_backend_wh.DTO.PedidoDTO;
import com.example.mr_backend_wh.model.Pedido;
import com.example.mr_backend_wh.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedido")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;


    @PostMapping("/crear")
    public PedidoDTO crearPedido(@RequestBody PedidoCrearRequestDTO pedidoCrearRequestDTO) {

        return pedidoService.crearPedido(pedidoCrearRequestDTO); // Redirige a la página de ver pedidos después de crear uno nuevo
    }
}
