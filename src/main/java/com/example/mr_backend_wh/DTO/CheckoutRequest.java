package com.example.mr_backend_wh.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
    private String productName;
    private Long amount; // en céntimos
    private String currency;
}