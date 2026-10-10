
package com.inventariopetrolero.repository;

import com.inventariopetrolero.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

}

