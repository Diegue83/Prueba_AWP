package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ProductoNoEncontradoException extends NegocioException {

    public ProductoNoEncontradoException(Integer idProducto) {
        super(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO", "No existe el producto " + idProducto + " en el catálogo");
    }
}
