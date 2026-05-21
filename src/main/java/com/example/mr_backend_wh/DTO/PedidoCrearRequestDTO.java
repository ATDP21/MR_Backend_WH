package com.example.mr_backend_wh.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PedidoCrearRequestDTO {
    private Integer direccionEntregaId;
    private Integer direccionFacturacionId;
    private List<StockPedidoCrearDTO> stockPedidos;
}
