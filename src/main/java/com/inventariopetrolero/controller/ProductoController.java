
package com.inventariopetrolero.controller;

import com.inventariopetrolero.model.Producto;
import com.inventariopetrolero.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // Mostrar el inventario
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listarProductos());
        model.addAttribute("stockBajo", productoService.obtenerProductosStockBajo());
        return "productos/lista";
    }

    // Mostrar formulario de registro
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("producto", new Producto());
        return "productos/formulario";
    }

    // Guardar un producto
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("producto") Producto producto,
                          BindingResult resultado) {

        if (resultado.hasErrors()) {
            return "productos/formulario";
        }

        productoService.guardarProducto(producto);
        return "redirect:/productos";
    }

    // Mostrar formulario de edición
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Producto producto = productoService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + id));

        model.addAttribute("producto", producto);
        return "productos/formulario";
    }

    // Eliminar un producto
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return "redirect:/productos";
    }
}


