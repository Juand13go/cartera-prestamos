package io.github.juand13go.cartera.aplicacion;

import java.io.Serializable;

/**
 * Quién inició sesión. Se guarda en la sesión HTTP; por eso es Serializable.
 *
 * @param cedula la del cliente; null para el prestamista
 */
public record UsuarioSesion(Rol rol, String cedula, String nombre) implements Serializable {

    public boolean esPrestamista() {
        return rol == Rol.PRESTAMISTA;
    }

    public boolean esCliente() {
        return rol == Rol.CLIENTE;
    }
}
