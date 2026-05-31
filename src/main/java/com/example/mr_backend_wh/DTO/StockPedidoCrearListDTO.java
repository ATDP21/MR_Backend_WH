package com.example.mr_backend_wh.DTO;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Getter
public class StockPedidoCrearListDTO {
    private List<StockPedidoDTO> listaPedidos;

}
