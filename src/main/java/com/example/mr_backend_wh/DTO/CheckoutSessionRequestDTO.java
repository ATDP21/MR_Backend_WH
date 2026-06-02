package com.example.mr_backend_wh.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionRequestDTO {
    private List<StockPedidoCrearDTO> cartItems;
    private Integer direccionEntregaId;
    private Integer direccionFacturacionId;
    private String email;
    private String nombreCliente;
}


