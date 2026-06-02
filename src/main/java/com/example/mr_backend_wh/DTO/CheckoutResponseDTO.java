package com.example.mr_backend_wh.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDTO {
    private String sessionId;
    private String sessionUrl;
    private String clientSecret;
    private String status;
}

