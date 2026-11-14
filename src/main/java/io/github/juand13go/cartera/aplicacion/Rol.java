package io.github.juand13go.cartera.aplicacion;

/** Los dos tipos de usuario del sistema. */
public enum Rol {
    /** Puede todo: clientes, préstamos, pagos, condonaciones. */
    PRESTAMISTA,
    /** Solo lectura, y solo de sus propios préstamos. */
    CLIENTE
}
