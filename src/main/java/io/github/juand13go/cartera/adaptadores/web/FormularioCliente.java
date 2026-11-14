package io.github.juand13go.cartera.adaptadores.web;

import java.math.BigDecimal;

import io.github.juand13go.cartera.aplicacion.DatosNuevoCliente;

/**
 * Los campos del formulario de cliente nuevo. Spring los llena con los setters al recibir el POST
 * (esto es la capa web: aquí sí hay setters; el dominio no los tiene).
 */
public class FormularioCliente {
    private String cedula;
    private String nombre;
    private String telefono;
    private String direccion;
    private String referencia;
    private BigDecimal cupoMaximo;
    private String notas;
    private String pin;

    /** Pasa los datos al caso de uso. Los espacios de cédula y teléfono se quitan por si el usuario los escribió. */
    public DatosNuevoCliente aDatos() {
        return new DatosNuevoCliente(sinEspacios(cedula), nombre, sinEspacios(telefono), direccion, referencia,
                cupoMaximo, notas, pin);
    }

    private static String sinEspacios(String texto) {
        return texto == null ? null : texto.replace(" ", "");
    }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public BigDecimal getCupoMaximo() { return cupoMaximo; }
    public void setCupoMaximo(BigDecimal cupoMaximo) { this.cupoMaximo = cupoMaximo; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
