package com.example.mr_backend_wh.controller;


import com.example.mr_backend_wh.DTO.CheckoutRequest;
import com.example.mr_backend_wh.DTO.StockPedidoCrearDTO;
import com.example.mr_backend_wh.DTO.StockPedidoCrearListDTO;
import com.example.mr_backend_wh.DTO.StockPedidoDTO;
import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.mr_backend_wh.service.VentaService;

@RestController
@RequestMapping("api/checkout")
public class CheckoutController {

    @Autowired
    private VentaService ventaService;


    @PostMapping("/create-checkout-session")
    public Map<String, String> createCheckoutSession(@RequestBody List<StockPedidoCrearDTO> cartItems) {
        try {
            return ventaService.createCheckoutSession(cartItems);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}