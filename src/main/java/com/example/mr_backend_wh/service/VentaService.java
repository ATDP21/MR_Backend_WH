package com.example.mr_backend_wh.service;

import com.example.mr_backend_wh.DTO.CheckoutResponseDTO;
import com.example.mr_backend_wh.DTO.CheckoutSessionRequestDTO;
import com.example.mr_backend_wh.DTO.PedidoDTO;
import com.example.mr_backend_wh.DTO.StockPedidoCrearDTO;
import com.example.mr_backend_wh.model.Guitarra;
import com.example.mr_backend_wh.repository.GuitarraRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VentaService {

    private final GuitarraRepository guitarraRepository;

    @Autowired
    private PedidoService pedidoService;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.front-url}")
    private String frontUrl;

    public VentaService(GuitarraRepository guitarraRepository) {
        this.guitarraRepository = guitarraRepository;
    }

    /**
     * Crear una sesión de pago con Stripe y opcionalmente crear el pedido
     */
    public CheckoutResponseDTO createCheckoutSession(CheckoutSessionRequestDTO request) {
        try {
            if (request == null || request.getCartItems() == null || request.getCartItems().isEmpty()) {
                throw new RuntimeException("El carrito no puede estar vacío");
            }
            if (request.getDireccionEntregaId() == null || request.getDireccionFacturacionId() == null) {
                throw new RuntimeException("Las direcciones de entrega y facturación son obligatorias");
            }
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new RuntimeException("El email del cliente es obligatorio");
            }

            // Inicializar Stripe con la clave API
            Stripe.apiKey = stripeApiKey;

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontUrl + "/checkout/cancelled");

            if (!request.getEmail().isBlank()) {
                paramsBuilder.setCustomerEmail(request.getEmail());
            }

            for (StockPedidoCrearDTO item : request.getCartItems()) {
                if (item == null || item.getGuitarraid() == null) {
                    throw new RuntimeException("Cada línea del carrito debe incluir guitarraid");
                }

                Guitarra guitarra = guitarraRepository.findById(item.getGuitarraid())
                        .orElseThrow(() -> new RuntimeException("Guitarra no encontrada con ID: " + item.getGuitarraid()));

                long cantidad = (item.getCantidad() == null ? 0L : item.getCantidad());
                if (cantidad <= 0) {
                    throw new RuntimeException("La cantidad debe ser mayor a 0");
                }

                BigDecimal precioUnitario = guitarra.getPrecio();
                if (precioUnitario == null || precioUnitario.signum() < 0) {
                    throw new RuntimeException("La guitarra " + item.getGuitarraid() + " no tiene precio válido");
                }

                // Convertir a centavos para Stripe (multiplicar por 100)
                long precioEnCentavos = precioUnitario.multiply(BigDecimal.valueOf(100)).longValue();

                paramsBuilder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                // Prioriza nombre visible en checkout, sin prefijo de ID.
                                                                .setName(resolveDisplayName(guitarra))
                                                                .setDescription("Serie: " + safe(guitarra.getNumserie()))
                                                                .build()
                                                )
                                                .setUnitAmount(precioEnCentavos)
                                                .build()
                                )
                                .setQuantity(cantidad)
                                .build()
                );
            }

            // Agregar metadatos a la sesión para referencia posterior
            paramsBuilder.putMetadata("email", request.getEmail());
            paramsBuilder.putMetadata("nombreCliente", safe(request.getNombreCliente()));
            paramsBuilder.putMetadata("direccionEntrega", request.getDireccionEntregaId().toString());
            paramsBuilder.putMetadata("direccionFacturacion", request.getDireccionFacturacionId().toString());
            paramsBuilder.putMetadata("cartItems", serializeCartItems(request.getCartItems()));

            // Crear la sesión de pago
            Session session = Session.create(paramsBuilder.build());

            log.info("Sesión de Stripe creada exitosamente: {}", session.getId());

            // Preparar la respuesta
            CheckoutResponseDTO response = new CheckoutResponseDTO();
            response.setSessionId(session.getId());
            response.setSessionUrl(session.getUrl());
            response.setClientSecret(session.getClientSecret());
            response.setStatus(session.getStatus());



            return response;

        } catch (StripeException e) {
            log.error("Error al crear sesión de Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear sesión de pago con Stripe: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado al procesar checkout: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar el checkout: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el estado de una sesión de pago
     */
    public CheckoutResponseDTO getSessionStatus(String sessionId) {
        try {
            Stripe.apiKey = stripeApiKey;
            Session session = Session.retrieve(sessionId);

            CheckoutResponseDTO response = new CheckoutResponseDTO();
            response.setSessionId(session.getId());
            response.setStatus(session.getStatus());
            response.setClientSecret(session.getClientSecret());

            return response;
        } catch (StripeException e) {
            log.error("Error al obtener estado de sesión: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener el estado del pago: " + e.getMessage(), e);
        }
    }

    @Transactional
    public PedidoDTO procesarPedidoDespuesDePago(Session session) {
        log.info("procesarPedidoDespuesDePago: inicio sessionId={} status={} payment_status={}", session.getId(), session.getStatus(), session.getPaymentStatus());

        if (session == null || session.getId() == null) {
            log.error("procesarPedidoDespuesDePago: sesión inválida");
            throw new RuntimeException("Sesión de Stripe inválida");
        }
        if (!"complete".equalsIgnoreCase(session.getStatus())) {
            log.warn("procesarPedidoDespuesDePago: sesión no completa status={}", session.getStatus());
            throw new RuntimeException("La sesión de pago no está completada");
        }
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus()) && !"paid".equalsIgnoreCase(session.getStatus())) {
            // Algunos objetos pueden reportar payment_status diferente; registramos ambos
            log.warn("procesarPedidoDespuesDePago: payment status inesperado: {}", session.getPaymentStatus());
        }
        
        // Extraer metadata desde la sesión de Stripe
        String email = session.getMetadata() == null ? null : session.getMetadata().get("email");
        String direccionEntrega = session.getMetadata() == null ? null : session.getMetadata().get("direccionEntrega");
        String direccionFacturacion = session.getMetadata() == null ? null : session.getMetadata().get("direccionFacturacion");
        String cartItems = session.getMetadata() == null ? null : session.getMetadata().get("cartItems");

        log.info("procesarPedidoDespuesDePago: metadata email={} dirEntrega={} dirFact={} cartItems={}", email, direccionEntrega, direccionFacturacion, cartItems == null ? null : (cartItems.length() > 200 ? cartItems.substring(0,200)+"..." : cartItems));

        if (email == null || email.isBlank()) {
            log.error("procesarPedidoDespuesDePago: email ausente en metadata");
            throw new RuntimeException("No se encontró email en metadata de Stripe");
        }
        if (direccionEntrega == null || direccionFacturacion == null) {
            log.error("procesarPedidoDespuesDePago: direcciones ausentes en metadata entrega={} fact={}", direccionEntrega, direccionFacturacion);
            throw new RuntimeException("No se encontraron direcciones en metadata de Stripe");
        }

        List<StockPedidoCrearDTO> lineas = deserializeCartItems(cartItems);
        if (lineas.isEmpty()) {
            log.error("procesarPedidoDespuesDePago: no se encontraron líneas en metadata");
            throw new RuntimeException("No se encontraron líneas de pedido en metadata de Stripe");
        }

        Integer direccionEntregaId;
        Integer direccionFacturacionId;
        try {
            direccionEntregaId = Integer.parseInt(direccionEntrega);
            direccionFacturacionId = Integer.parseInt(direccionFacturacion);
        } catch (Exception e) {
            log.error("procesarPedidoDespuesDePago: error parseando direcciones desde metadata: {}", e.getMessage(), e);
            throw new RuntimeException("Direcciones de metadata inválidas", e);
        }

        log.info("procesarPedidoDespuesDePago: invocando crearPedidoConfirmadoDesdePago email={} direccionEntregaId={} lineasCount={}", email, direccionEntregaId, lineas.size());

        return pedidoService.crearPedidoConfirmadoDesdePago(
                email,
                direccionEntregaId,
                direccionFacturacionId,
                lineas,
                session.getId()
        );
    }

    /**
     * Variante que permite pasar valores alternativos (overrides) cuando la metadata
     * de la sesión de Stripe no contiene toda la información (útil para debug/manual testing).
     */
    @Transactional
    public PedidoDTO procesarPedidoDespuesDePago(Session session,
                                                String emailOverride,
                                                String direccionEntregaOverride,
                                                String direccionFacturacionOverride,
                                                String cartItemsOverride) {
        // Si la session es nula o inválida, delegar a la versión principal (hará las validaciones)
        if (session == null || session.getId() == null) {
            throw new RuntimeException("Sesión de Stripe inválida");
        }

        // Preferir metadata, si no existe usar los overrides
        String email = session.getMetadata() == null ? null : session.getMetadata().get("email");
        String direccionEntrega = session.getMetadata() == null ? null : session.getMetadata().get("direccionEntrega");
        String direccionFacturacion = session.getMetadata() == null ? null : session.getMetadata().get("direccionFacturacion");
        String cartItems = session.getMetadata() == null ? null : session.getMetadata().get("cartItems");

        if ((email == null || email.isBlank()) && emailOverride != null && !emailOverride.isBlank()) {
            email = emailOverride;
        }
        if ((direccionEntrega == null || direccionEntrega.isBlank()) && direccionEntregaOverride != null && !direccionEntregaOverride.isBlank()) {
            direccionEntrega = direccionEntregaOverride;
        }
        if ((direccionFacturacion == null || direccionFacturacion.isBlank()) && direccionFacturacionOverride != null && !direccionFacturacionOverride.isBlank()) {
            direccionFacturacion = direccionFacturacionOverride;
        }
        if ((cartItems == null || cartItems.isBlank()) && cartItemsOverride != null && !cartItemsOverride.isBlank()) {
            cartItems = cartItemsOverride;
        }

        log.info("procesarPedidoDespuesDePago(overrides): metadata email={} dirEntrega={} dirFact={} cartItems={} (session={})",
                email, direccionEntrega, direccionFacturacion, cartItems == null ? null : (cartItems.length() > 200 ? cartItems.substring(0,200)+"..." : cartItems), session.getId());

        // Reusar la lógica existente: validar presencia de email/direcciones/cartItems
        if (email == null || email.isBlank()) {
            log.error("procesarPedidoDespuesDePago: email ausente en metadata y overrides");
            throw new RuntimeException("No se encontró email en metadata de Stripe ni en los datos proporcionados");
        }
        if (direccionEntrega == null || direccionFacturacion == null) {
            log.error("procesarPedidoDespuesDePago: direcciones ausentes en metadata y overrides entrega={} fact={}", direccionEntrega, direccionFacturacion);
            throw new RuntimeException("No se encontraron direcciones en metadata de Stripe ni en los datos proporcionados");
        }

        List<StockPedidoCrearDTO> lineas = deserializeCartItems(cartItems);
        if (lineas.isEmpty()) {
            log.error("procesarPedidoDespuesDePago: no se encontraron líneas en metadata ni en overrides");
            throw new RuntimeException("No se encontraron líneas de pedido en metadata de Stripe ni en los datos proporcionados");
        }

        Integer direccionEntregaId;
        Integer direccionFacturacionId;
        try {
            direccionEntregaId = Integer.parseInt(direccionEntrega);
            direccionFacturacionId = Integer.parseInt(direccionFacturacion);
        } catch (Exception e) {
            log.error("procesarPedidoDespuesDePago: error parseando direcciones desde metadata/overrides: {}", e.getMessage(), e);
            throw new RuntimeException("Direcciones de metadata/overrides inválidas", e);
        }

        return pedidoService.crearPedidoConfirmadoDesdePago(
                email,
                direccionEntregaId,
                direccionFacturacionId,
                lineas,
                session.getId()
        );
    }

    private String resolveDisplayName(Guitarra guitarra) {
        if (guitarra.getNombre() != null && !guitarra.getNombre().isBlank()) {
            return guitarra.getNombre();
        }
        if (guitarra.getTipo() != null && !guitarra.getTipo().isBlank()) {
            return guitarra.getTipo();
        }
        if (guitarra.getNumserie() != null && !guitarra.getNumserie().isBlank()) {
            return "Guitarra serie " + guitarra.getNumserie();
        }
        return "Guitarra";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String serializeCartItems(List<StockPedidoCrearDTO> items) {
        return items.stream()
                .filter(Objects::nonNull)
                .map(item -> item.getGuitarraid() + ":" + (item.getCantidad() == null ? 0L : item.getCantidad()))
                .collect(Collectors.joining("|"));
    }

    private List<StockPedidoCrearDTO> deserializeCartItems(String raw) {
        List<StockPedidoCrearDTO> items = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return items;
        }

        String[] parts = raw.split("\\|");
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String[] tuple = part.split(":");
            if (tuple.length != 2) {
                continue;
            }
            try {
                StockPedidoCrearDTO dto = new StockPedidoCrearDTO();
                dto.setGuitarraid(Integer.parseInt(tuple[0].trim()));
                dto.setCantidad(Long.parseLong(tuple[1].trim()));
                items.add(dto);
            } catch (NumberFormatException ignored) {
                // Ignora líneas malformadas para no bloquear todo por una entrada inválida.
            }
        }
        return items;
    }
}



