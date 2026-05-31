package com.example.mr_backend_wh.service;

import com.example.mr_backend_wh.DTO.StockPedidoCrearDTO;
import com.example.mr_backend_wh.model.Guitarra;
import com.example.mr_backend_wh.repository.GuitarraRepository;
import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class VentaService {

    @Autowired
    private final GuitarraRepository guitarraRepository;



    public Map<String, String> createCheckoutSession(List<StockPedidoCrearDTO> cartItems) {
        StripeClient client = new StripeClient("sk_test_51TZCFqJ94S5Ke5YLbx13JJ8V2XQMjictMMRkjF3AGrqNWVZnrlolOG6Cve91QJQx5juysuBMAQfTsmxEunE5qt6V00bMY4ewRf");

        try {
    SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
            .setUiMode(SessionCreateParams.UiMode.ELEMENTS)
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setReturnUrl("http://localhost:3000/complete?session_id={CHECKOUT_SESSION_ID}");

    // Add each product as a line item
            for (
    StockPedidoCrearDTO item : cartItems) {
                Guitarra guitarra = new Guitarra();
                guitarra = guitarraRepository.findById(item.getGuitarraid())
                        .orElseThrow(() -> new RuntimeException("Guitarra no encontrada con ID: " + item.getGuitarraid()));
        paramsBuilder.addLineItem(
                SessionCreateParams.LineItem.builder()
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("eur")
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName(guitarra.getNombre())
                                                        .build()
                                        )
                                        .setUnitAmount(guitarra.getPrecio().multiply(new BigDecimal("100")).longValue()) // your DTO price in cents
                                        .build()
                        )
                        .setQuantity(item.getCantidad()) //item.getQuantity()
                        .build()
        );
    }

    Session session = client.v1().checkout().sessions().create(paramsBuilder.build());

    Map<String, String> response = new HashMap<>();
            response.put("clientSecret", session.getClientSecret()); // Direct getter method
            return response;
    } catch (Exception e) {
        throw new RuntimeException("Error creating checkout session", e);
        }
    }
}
