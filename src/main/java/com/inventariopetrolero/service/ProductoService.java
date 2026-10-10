
package com.inventariopetrolero.service;

import com.inventariopetrolero.model.Producto;
import com.inventariopetrolero.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    // Consultar todos los productos
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    // Buscar un producto por su ID
    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    // Guardar o actualizar un producto
    public Producto guardarProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    // Eliminar un producto
    public void eliminarProducto(Long id) {
        productoRepository.deleteById(id);
    }

    // Consultar productos con stock bajo
    public List<Producto> obtenerProductosStockBajo() {
        return productoRepository.findAll()
                .stream()
                .filter(producto -> producto.getCantidad() != null
                        && producto.getStockMinimo() != null
                        && producto.getCantidad() <= producto.getStockMinimo())
                .toList();
    }
}


