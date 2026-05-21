package com.example.mr_backend_wh.DTO;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Data
@Getter
public class StockPedidoDTO {

    private GuitarraDTO guitarraid;
    private Integer cantidad;
    private BigDecimal precioUnidad;

}
