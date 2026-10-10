
package com.inventariopetrolero.service;

import com.inventariopetrolero.model.DetallePedido;
import com.inventariopetrolero.model.Pedido;
import com.inventariopetrolero.model.Producto;
import com.inventariopetrolero.repository.PedidoRepository;
import com.inventariopetrolero.repository.ProductoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository) {

        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    public Pedido guardarPedido(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }

    public void eliminarPedido(Long id) {
        pedidoRepository.deleteById(id);
    }

    @Transactional
    public void cambiarEstado(Long pedidoId, String nuevoEstado) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El pedido no existe."
                        )
                );

        String estadoActual = pedido.getEstado();

        // Validar las transiciones permitidas
        boolean transicionValida =
                ("Pendiente".equals(estadoActual)
                        && ("Aprobado".equals(nuevoEstado)
                        || "Cancelado".equals(nuevoEstado)))
                        ||
                        ("Aprobado".equals(estadoActual)
                                && ("Entregado".equals(nuevoEstado)
                                || "Cancelado".equals(nuevoEstado)));

        if (!transicionValida) {
            throw new IllegalStateException(
                    "No se permite cambiar el pedido de "
                            + estadoActual + " a " + nuevoEstado + "."
            );
        }

        // Descontar inventario únicamente al entregar
        if ("Entregado".equals(nuevoEstado)) {

            if (pedido.getDetalles() == null
                    || pedido.getDetalles().isEmpty()) {

                throw new IllegalStateException(
                        "El pedido no tiene materiales registrados."
                );
            }

            // Agrupar cantidades por producto
            Map<Long, Integer> cantidades = new HashMap<>();

            for (DetallePedido detalle : pedido.getDetalles()) {

                if (detalle.getProducto() == null
                        || detalle.getProducto().getId() == null
                        || detalle.getCantidad() == null
                        || detalle.getCantidad() <= 0) {

                    throw new IllegalStateException(
                            "El pedido contiene un detalle inválido."
                    );
                }

                Long productoId = detalle.getProducto().getId();

                cantidades.merge(
                        productoId,
                        detalle.getCantidad(),
                        Integer::sum
                );
            }

            // Verificar existencias antes de descontar
            Map<Long, Producto> productos = new HashMap<>();

            for (Map.Entry<Long, Integer> item
                    : cantidades.entrySet()) {

                Producto producto = productoRepository
                        .findById(item.getKey())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Uno de los materiales no existe."
                                )
                        );

                if (producto.getCantidad() == null
                        || producto.getCantidad() < item.getValue()) {

                    throw new IllegalStateException(
                            "Stock insuficiente para: "
                                    + producto.getNombre()
                    );
                }

                productos.put(item.getKey(), producto);
            }

            // Descontar las unidades
            for (Map.Entry<Long, Integer> item
                    : cantidades.entrySet()) {

                Producto producto = productos.get(item.getKey());

                producto.setCantidad(
                        producto.getCantidad() - item.getValue()
                );

                productoRepository.save(producto);
            }
        }

        pedido.setEstado(nuevoEstado);
        pedidoRepository.save(pedido);
    }
}
