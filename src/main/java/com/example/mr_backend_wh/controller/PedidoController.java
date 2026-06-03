package com.example.mr_backend_wh.controller;

import com.example.mr_backend_wh.DTO.PedidoCrearRequestDTO;
import com.example.mr_backend_wh.DTO.PedidoDTO;
import com.example.mr_backend_wh.model.Pedido;
import com.example.mr_backend_wh.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/pedido")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;


    @PostMapping("/crear")
    public PedidoDTO crearPedido(@RequestBody PedidoCrearRequestDTO pedidoCrearRequestDTO) {

        return pedidoService.crearPedido(pedidoCrearRequestDTO); // Redirige a la página de ver pedidos después de crear uno nuevo
    }

    @GetMapping("/mios")
    public List<PedidoDTO> misPedidos() {
        return pedidoService.obtenerPedidosUsuarioLoggeado();
    }

    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<PedidoDTO> todosLosPedidos() {
        return pedidoService.obtenerTodosLosPedidos();
    }

    @GetMapping("/{id}")
    public PedidoDTO verPedido(@PathVariable("id") Integer id) {
        return pedidoService.obtenerPedidoPorIdParaUsuarioLoggeado(id);
    }
}
