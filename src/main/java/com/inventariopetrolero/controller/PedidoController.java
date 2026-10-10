
package com.inventariopetrolero.controller;

import com.inventariopetrolero.model.DetallePedido;
import com.inventariopetrolero.model.Pedido;
import com.inventariopetrolero.model.Producto;
import com.inventariopetrolero.repository.ProductoRepository;
import com.inventariopetrolero.service.PedidoService;
import com.inventariopetrolero.service.ProductoService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final ProductoService productoService;
    private final ProductoRepository productoRepository;

    public PedidoController(
            PedidoService pedidoService,
            ProductoService productoService,
            ProductoRepository productoRepository) {

        this.pedidoService = pedidoService;
        this.productoService = productoService;
        this.productoRepository = productoRepository;
    }

    // Mostrar los pedidos registrados
    @GetMapping
    public String listarPedidos(Model model) {

        model.addAttribute(
                "pedidos",
                pedidoService.listarPedidos()
        );

        return "pedidos/lista";
    }

    // Mostrar el formulario para registrar pedidos
    @GetMapping("/nuevo")
    public String nuevoPedido(Model model) {

        Pedido pedido = new Pedido();

        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado("Pendiente");

        pedido.agregarDetalle(new DetallePedido());

        model.addAttribute("pedido", pedido);

        model.addAttribute(
                "productos",
                productoService.listarProductos()
        );

        return "pedidos/formulario";
    }

    // Guardar el pedido y validar los materiales
    @PostMapping("/guardar")
    public String guardarPedido(
            @Valid @ModelAttribute("pedido") Pedido pedido,
            BindingResult resultado,
            Model model) {

        // Verificar que el pedido tenga materiales
        if (pedido.getDetalles() == null
                || pedido.getDetalles().isEmpty()) {

            resultado.reject(
                    "pedido.sinMaterial",
                    "Debe seleccionar un material."
            );

        } else {

            DetallePedido detalle = pedido.getDetalles().get(0);

            Long productoId = detalle.getProducto() == null
                    ? null
                    : detalle.getProducto().getId();

            if (productoId == null) {

                resultado.reject(
                        "pedido.sinProducto",
                        "Seleccione un material válido."
                );

            } else {

                Producto producto = productoRepository
                        .findById(productoId)
                        .orElse(null);

                if (producto == null) {

                    resultado.reject(
                            "pedido.productoInvalido",
                            "El material seleccionado no existe."
                    );

                } else {

                    detalle.setProducto(producto);
                    detalle.setPedido(pedido);

                    if (detalle.getCantidad() == null
                            || detalle.getCantidad() < 1) {

                        resultado.reject(
                                "pedido.cantidadInvalida",
                                "La cantidad debe ser mayor a cero."
                        );

                    } else if (producto.getCantidad() == null
                            || detalle.getCantidad() > producto.getCantidad()) {

                        resultado.reject(
                                "pedido.stockInsuficiente",
                                "La cantidad solicitada supera el stock disponible."
                        );
                    }
                }
            }
        }

        // Si existen errores, regresar al formulario
        // conservando la información ingresada
        if (resultado.hasErrors()) {

            model.addAttribute(
                    "productos",
                    productoService.listarProductos()
            );

            return "pedidos/formulario";
        }

        // Guardar el pedido cuando todo sea válido
        pedidoService.guardarPedido(pedido);

        return "redirect:/pedidos";
    }

    // Aprobar, cancelar o entregar pedidos
    @PostMapping("/{id}/estado")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado,
            RedirectAttributes redirectAttributes) {

        try {

            pedidoService.cambiarEstado(id, estado);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "El estado del pedido se actualizó correctamente."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/pedidos";
    }
}
