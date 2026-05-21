package com.example.mr_backend_wh.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PedidoDTO {
    private Integer id;
    private UsuarioSecureDTO usuario;
    private OffsetDateTime fecha;
    private String estado;
    private BigDecimal total;
    private DireccionDTO direccionEntrega;
    private DireccionDTO direccionFacturacion;
    private List<StockPedidoDTO> stockPedidos;
}
