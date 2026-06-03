package com.example.mr_backend_wh.service;

import com.example.mr_backend_wh.DTO.*;
import com.example.mr_backend_wh.model.*;
import com.example.mr_backend_wh.repository.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
@lombok.extern.slf4j.Slf4j
public class PedidoService {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private GuitarraRepository guitarraRepository;
    @Autowired
    private DireccionRepository direccionRepository;
    @Autowired
    private StockPedidoRepository stockPedidoRepository;

    @Transactional
    public PedidoDTO crearPedido(PedidoCrearRequestDTO dto) {
        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado();

        Usuario usuario = usuarioRepository.findTopByNomusuario(usuarioSecure.getNomusuario())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));
        return crearPedidoInterno(usuario, dto, "PENDIENTE", null);
    }

    @Transactional
    public PedidoDTO crearPedidoConfirmadoDesdePago(String email,
                                                    Integer direccionEntregaId,
                                                    Integer direccionFacturacionId,
                                                    List<StockPedidoCrearDTO> lineas) {
        return crearPedidoConfirmadoDesdePago(email, direccionEntregaId, direccionFacturacionId, lineas, null);
    }

    @Transactional
    public PedidoDTO crearPedidoConfirmadoDesdePago(String email,
                                                    Integer direccionEntregaId,
                                                    Integer direccionFacturacionId,
                                                    List<StockPedidoCrearDTO> lineas,
                                                    String stripeSessionId) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email inválido para crear pedido confirmado");
        }

        // Validar idempotencia: si ya existe un pedido con este stripeSessionId, devolverlo
        if (stripeSessionId != null && !stripeSessionId.isBlank()) {
            var pedidoExistente = pedidoRepository.findByStripeSessionId(stripeSessionId);
            if (pedidoExistente.isPresent()) {
                log.warn("crearPedidoConfirmadoDesdePago: pedido ya existe para stripeSessionId={}", stripeSessionId);
                return toPedidoDTO(pedidoExistente.get());
            }
        }

        Usuario usuario = usuarioRepository.findTopByEmail(email)
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado por email: " + email));

        PedidoCrearRequestDTO dto = new PedidoCrearRequestDTO();
        dto.setDireccionEntregaId(direccionEntregaId);
        dto.setDireccionFacturacionId(direccionFacturacionId);
        dto.setStockPedidos(lineas);

        return crearPedidoInterno(usuario, dto, "CONFIRMADO", stripeSessionId);
    }

    private PedidoDTO crearPedidoInterno(Usuario usuario, PedidoCrearRequestDTO dto, String estadoPedido) {
        return crearPedidoInterno(usuario, dto, estadoPedido, null);
    }

    private PedidoDTO crearPedidoInterno(Usuario usuario, PedidoCrearRequestDTO dto, String estadoPedido, String stripeSessionId) {
        if (dto == null || dto.getStockPedidos() == null || dto.getStockPedidos().isEmpty()) {
            throw new RuntimeException("El pedido debe incluir al menos una línea de producto");
        }
        if (dto.getDireccionEntregaId() == null || dto.getDireccionFacturacionId() == null) {
            throw new RuntimeException("Las direcciones de entrega y facturación son obligatorias");
        }

        Pedido pedido = new Pedido();

        pedido.setUsuarioid(usuario);

        Direccion direccionEntrega = direccionRepository.getDireccionById(dto.getDireccionEntregaId());
        pedido.setDireccionEntrega(DireccionSnapshot.from(direccionEntrega));

        Direccion direccionFacturacion = direccionRepository.getDireccionById(dto.getDireccionFacturacionId());
        pedido.setDireccionFacturacion(DireccionSnapshot.from(direccionFacturacion));

        pedido.setEstado(estadoPedido);
        pedido.setTotal(BigDecimal.ZERO);
        if (stripeSessionId != null && !stripeSessionId.isBlank()) {
            pedido.setStripeSessionId(stripeSessionId);
        }

        Pedido guardado = pedidoRepository.save(pedido);

        List<StockPedido> listaProductos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;


        for (StockPedidoCrearDTO linea : dto.getStockPedidos()) {
            Guitarra guitarra = guitarraRepository.findById(linea.getGuitarraid())
                    .orElseThrow(() -> new RuntimeException("❌ Guitarra no encontrada: " + linea.getGuitarraid()));

            BigDecimal precioUnidad = guitarra.getPrecio();
            long cantidad = (linea.getCantidad() == null ? 0L : linea.getCantidad());
            if (cantidad <= 0) {
                throw new RuntimeException("❌ Cantidad inválida para guitarra " + linea.getGuitarraid());
            }


            StockPedido sp = new StockPedido();
            // IMPORTANTE: en bidireccional, este campo debe existir (y coincidir con mappedBy)
            sp.setPedidoid(guardado);              // <- clave (FK se rellena al persistir)

            sp.setGuitarraid(guitarra);
            sp.setCantidad(Long.valueOf(cantidad));
            sp.setPrecioUnidad(precioUnidad);

            total = total.add(precioUnidad.multiply(BigDecimal.valueOf(cantidad)));
            stockPedidoRepository.save(sp);
            listaProductos.add(sp);
        }
        pedido.setTotal(total);
        pedido.setListaProductos(listaProductos);

        Pedido actualizado = pedidoRepository.save(pedido);
        return toPedidoDTO(actualizado);
    }

    @Transactional
    public List<PedidoDTO> obtenerTodosLosPedidos() {
        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado();

        Usuario usuario = usuarioRepository.findTopByNomusuario(usuarioSecure.getNomusuario())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));
        if (Objects.equals(usuario.getRolid(), new Rol(2, "ADMIN"))) {
            List<Pedido> pedidos = pedidoRepository.findAllByOrderByFechaDesc();
            List<PedidoDTO> resultado = new ArrayList<>();
            if (pedidos != null) {
                for (Pedido p : pedidos) {
                    resultado.add(toPedidoDTO(p));
                }
            }
            return resultado;

        } else {
            List<PedidoDTO> resultado = new ArrayList<>();
            return resultado;
        }

    }

    @Transactional
    public List<PedidoDTO> obtenerPedidosUsuarioLoggeado() {
        // Obtener datos del usuario autenticado
        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado();

        Usuario usuario = usuarioRepository.findTopByNomusuario(usuarioSecure.getNomusuario())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));

        // Usar búsqueda por id para evitar problemas de resolución
        List<Pedido> pedidos = pedidoRepository.findAllByUsuarioidIdOrderByFechaDesc(usuario.getId());

        List<PedidoDTO> resultado = new ArrayList<>();
        if (pedidos != null) {
            for (Pedido p : pedidos) {
                resultado.add(toPedidoDTO(p));
            }
        }
        return resultado;
    }

    @Transactional
    public PedidoDTO obtenerPedidoPorIdParaUsuarioLoggeado(Integer pedidoId) {
        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado();
        Usuario usuario = usuarioRepository.findTopByNomusuario(usuarioSecure.getNomusuario())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("❌ Pedido no encontrado"));

        // Si es admin, puede ver cualquier pedido. Si no, solo el suyo.
        if (!usuarioSecure.isEsAdmin() && !pedido.getUsuarioid().getId().equals(usuario.getId())) {
            throw new RuntimeException("Acceso denegado: el pedido no pertenece al usuario autenticado");
        }

        return toPedidoDTO(pedido);
    }

    private PedidoDTO toPedidoDTO(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setFecha(pedido.getFecha());
        dto.setEstado(pedido.getEstado());
        dto.setTotal(pedido.getTotal());
        dto.setStripeSessionId(pedido.getStripeSessionId());

        UsuarioSecureDTO u = new UsuarioSecureDTO();
        Usuario usuPedido = pedido.getUsuarioid();
        u.setNomusuario(usuPedido.getNomusuario());
        u.setEmail(usuPedido.getEmail());
        u.setNombreCompleto(usuPedido.getNomcompleto());
        u.setEsAdmin(false);

        dto.setUsuario(u);

        // Mapear DireccionSnapshot -> DireccionDTO (si existe)
        if (pedido.getDireccionEntrega() != null) {
            DireccionDTO de = new DireccionDTO();
            de.setId(null);
            de.setNombreDestinatario(pedido.getDireccionEntrega().getNombreDestinatario());
            de.setDireccionCalle(pedido.getDireccionEntrega().getDireccionCalle());
            de.setCodigoPostal(pedido.getDireccionEntrega().getCodigoPostal());
            de.setCiudad(pedido.getDireccionEntrega().getCiudad());
            de.setProvincia(pedido.getDireccionEntrega().getProvincia());
            de.setPais(pedido.getDireccionEntrega().getPais());
            de.setTelefono(pedido.getDireccionEntrega().getTelefono());
            de.setDocumentoId(pedido.getDireccionEntrega().getDocumentoId());
            dto.setDireccionEntrega(de);
        }

        if (pedido.getDireccionFacturacion() != null) {
            DireccionDTO df = new DireccionDTO();
            df.setId(null);
            df.setNombreDestinatario(pedido.getDireccionFacturacion().getNombreDestinatario());
            df.setDireccionCalle(pedido.getDireccionFacturacion().getDireccionCalle());
            df.setCodigoPostal(pedido.getDireccionFacturacion().getCodigoPostal());
            df.setCiudad(pedido.getDireccionFacturacion().getCiudad());
            df.setProvincia(pedido.getDireccionFacturacion().getProvincia());
            df.setPais(pedido.getDireccionFacturacion().getPais());
            df.setTelefono(pedido.getDireccionFacturacion().getTelefono());
            df.setDocumentoId(pedido.getDireccionFacturacion().getDocumentoId());
            dto.setDireccionFacturacion(df);
        }

        // Mapear líneas de pedido
        if (pedido.getListaProductos() != null && !pedido.getListaProductos().isEmpty()) {
            List<StockPedidoDTO> lista = new ArrayList<>();
            for (StockPedido sp : pedido.getListaProductos()) {
                StockPedidoDTO spdto = new StockPedidoDTO();
                if (sp.getGuitarraid() != null) {
                    Guitarra g = sp.getGuitarraid();
                    GuitarraDTO gdto = new GuitarraDTO();
                    gdto.setId(g.getId());
                    gdto.setNumserie(g.getNumserie());
                    gdto.setNombre(g.getNombre());
                    gdto.setEstado(g.getEstado());
                    gdto.setPrecio(g.getPrecio());
                    gdto.setTipoMadera(g.getTipomadera());
                    gdto.setTipo(g.getTipo());
                    spdto.setGuitarraid(gdto);
                }
                spdto.setCantidad(sp.getCantidad() == null ? 0 : sp.getCantidad().intValue());
                spdto.setPrecioUnidad(sp.getPrecioUnidad());
                lista.add(spdto);
            }
            dto.setStockPedidos(lista);
        }

        return dto;
    }
}
