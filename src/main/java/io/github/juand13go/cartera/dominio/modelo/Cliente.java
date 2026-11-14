package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;

/**
 * Persona a la que el prestamista le presta. Su llave natural es la cédula.
 * El PIN se guarda solo cifrado (hash BCrypt): el dominio nunca ve el PIN real
 * ni sabe cómo se cifra; eso lo hace un adaptador.
 */
public class Cliente {
    private final String cedula;
    private final String nombre;
    private final String telefono;
    private final String direccion;
    private final String referencia;
    private final BigDecimal cupoMaximo;
    private final String notas;
    private final String pinCifrado;

    /**
     * @param cedula     6 a 10 dígitos
     * @param nombre     obligatorio
     * @param telefono   celular de 10 dígitos (se usa para el enlace de WhatsApp)
     * @param direccion  opcional
     * @param referencia opcional: persona que da referencias del cliente
     * @param cupoMaximo cuánto capital puede tener prestado a la vez; 0 o más
     * @param notas      opcional: notas internas que solo ve el prestamista
     * @param pinCifrado hash del PIN, nunca el PIN real
     */
    public Cliente(String cedula, String nombre, String telefono, String direccion, String referencia,
                   BigDecimal cupoMaximo, String notas, String pinCifrado) {
        if (cedula == null || !cedula.matches("\\d{6,10}")) {
            throw new IllegalArgumentException("La cédula debe tener entre 6 y 10 dígitos");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (telefono == null || !telefono.matches("\\d{10}")) {
            throw new IllegalArgumentException("El teléfono debe tener 10 dígitos");
        }
        if (cupoMaximo == null || cupoMaximo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El cupo no puede ser negativo");
        }
        if (pinCifrado == null || pinCifrado.isBlank()) {
            throw new IllegalArgumentException("El cliente necesita un PIN");
        }
        this.cedula = cedula;
        this.nombre = nombre.trim();
        this.telefono = telefono;
        this.direccion = textoOpcional(direccion);
        this.referencia = textoOpcional(referencia);
        this.cupoMaximo = cupoMaximo;
        this.notas = textoOpcional(notas);
        this.pinCifrado = pinCifrado;
    }

    /** Los datos opcionales nunca quedan en null: si no vienen, quedan como texto vacío. */
    private static String textoOpcional(String texto) {
        return texto == null ? "" : texto.trim();
    }

    public String getCedula() {
        return cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public BigDecimal getCupoMaximo() {
        return cupoMaximo;
    }

    public String getNotas() {
        return notas;
    }

    public String getPinCifrado() {
        return pinCifrado;
    }
}
