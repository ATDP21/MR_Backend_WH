package com.example.mr_backend_wh.service;

import com.example.mr_backend_wh.DTO.*;
import com.example.mr_backend_wh.model.*;
import com.example.mr_backend_wh.repository.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
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
    public PedidoDTO crearPedido( PedidoCrearRequestDTO dto) {

        UsuarioSecureDTO usuarioSecure = usuarioService.obtenerPerfilUsuarioLoggeado();

        Usuario usuario = usuarioRepository.findTopByNomusuario(usuarioSecure.getNomusuario())
                .orElseThrow(() -> new RuntimeException("❌ Usuario no encontrado"));

        Pedido pedido = new Pedido();

        pedido.setUsuarioid(usuario);

//        DireccionSnapshot direccionEntrega = new DireccionSnapshot();
//        DireccionDTO direccionDTOEntrega = dto.getDireccionEntrega();
//        direccionEntrega.setNombreDestinatario(direccionDTOEntrega.getNombreDestinatario());
//        direccionEntrega.setDireccionCalle(direccionDTOEntrega.getDireccionCalle());
//        direccionEntrega.setCodigoPostal(direccionDTOEntrega.getCodigoPostal());
//        direccionEntrega.setCiudad(direccionDTOEntrega.getCiudad());
//        direccionEntrega.setProvincia(direccionDTOEntrega.getProvincia());
//        direccionEntrega.setPais(direccionDTOEntrega.getPais());
//        direccionEntrega.setTelefono(direccionDTOEntrega.getTelefono());
//        direccionEntrega.setDocumentoId(direccionDTOEntrega.getDocumentoId());

        Direccion direccionEntregaId = direccionRepository.getDireccionById(dto.getDireccionEntregaId());

        pedido.setDireccionEntrega(DireccionSnapshot.from(direccionEntregaId));

//        DireccionSnapshot direccionFacturacion = new DireccionSnapshot();
//        DireccionDTO direccionDTOFacturacion = dto.getDireccionFacturacion();
//        direccionFacturacion.setNombreDestinatario(direccionDTOFacturacion.getNombreDestinatario());
//        direccionFacturacion.setDireccionCalle(direccionDTOFacturacion.getDireccionCalle());
//        direccionFacturacion.setCodigoPostal(direccionDTOFacturacion.getCodigoPostal());
//        direccionFacturacion.setCiudad(direccionDTOFacturacion.getCiudad());
//        direccionFacturacion.setProvincia(direccionDTOFacturacion.getProvincia());
//        direccionFacturacion.setPais(direccionDTOFacturacion.getPais());
//        direccionFacturacion.setTelefono(direccionDTOFacturacion.getTelefono());
//        direccionFacturacion.setDocumentoId(direccionDTOFacturacion.getDocumentoId());

        Direccion direccionFacturacionId = direccionRepository.getDireccionById(dto.getDireccionFacturacionId());

        pedido.setDireccionFacturacion(DireccionSnapshot.from(direccionFacturacionId));

        pedido.setEstado("Pendiente");
        pedido.setTotal(BigDecimal.ZERO);

        Pedido guardado = pedidoRepository.save(pedido);

        List<StockPedido> listaProductos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;


        for (StockPedidoCrearDTO linea : dto.getStockPedidos()) {
            Guitarra guitarra = guitarraRepository.findById(linea.getGuitarraid())
                    .orElseThrow(() -> new RuntimeException("❌ Guitarra no encontrada: " + linea.getGuitarraid()));

            BigDecimal precioUnidad = guitarra.getPrecio();
            Long cantidad = (linea.getCantidad() == null ? 0L : linea.getCantidad());


            StockPedido sp = new StockPedido();
            // IMPORTANTE: en bidireccional, este campo debe existir (y coincidir con mappedBy)
            sp.setPedidoid(guardado);              // <- clave (FK se rellena al persistir)

            sp.setGuitarraid(guitarra);
            sp.setCantidad(cantidad);
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
    private PedidoDTO toPedidoDTO(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setFecha(pedido.getFecha());
        dto.setEstado(pedido.getEstado());
        dto.setTotal(pedido.getTotal());

        UsuarioSecureDTO u = new UsuarioSecureDTO();
        Usuario usuPedido = pedido.getUsuarioid();
        u.setNomusuario(usuPedido.getNomusuario());
        u.setEmail(usuPedido.getEmail());
        u.setNombreCompleto(u.getNombreCompleto());
        u.setEsAdmin(false);

        dto.setUsuario(u);



        // Direcciones: si guardas snapshots, aquí necesitarás mapear snapshot -> DireccionDTO
        // dto.setDireccionEntrega(...)
        // dto.setDireccionFacturacion(...)

        // Líneas: si tu PedidoDTO las necesita, mapea pedido.getListaProductos() -> List<StockPedidoDTO>
        // dto.setStockPedidos(...)

        return dto;
    }
}
