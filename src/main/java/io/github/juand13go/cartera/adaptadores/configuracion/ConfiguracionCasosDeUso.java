package io.github.juand13go.cartera.adaptadores.configuracion;

import java.math.BigDecimal;
import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.juand13go.cartera.aplicacion.ArmarPanel;
import io.github.juand13go.cartera.aplicacion.CancelarPrestamo;
import io.github.juand13go.cartera.aplicacion.CondonarMulta;
import io.github.juand13go.cartera.aplicacion.ConsultarPrestamos;
import io.github.juand13go.cartera.aplicacion.CrearCliente;
import io.github.juand13go.cartera.aplicacion.CrearPrestamo;
import io.github.juand13go.cartera.aplicacion.DeshacerUltimoPago;
import io.github.juand13go.cartera.aplicacion.DirectorioClientes;
import io.github.juand13go.cartera.aplicacion.GenerarRecibo;
import io.github.juand13go.cartera.aplicacion.IniciarSesion;
import io.github.juand13go.cartera.aplicacion.RegistrarPago;
import io.github.juand13go.cartera.aplicacion.SimularAbono;
import io.github.juand13go.cartera.aplicacion.puertos.CifradorPin;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioClientes;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;

/**
 * Conecta los casos de uso con sus adaptadores. Los casos de uso son clases de Java puro
 * (sin anotaciones de Spring): aquí es el único lugar donde Spring los crea y les pasa lo que necesitan.
 */
@Configuration
public class ConfiguracionCasosDeUso {

    /** Una sola instancia para toda la aplicación: la tabla hash se carga una vez al arrancar. */
    @Bean
    public DirectorioClientes directorioClientes(RepositorioClientes repositorio) {
        return new DirectorioClientes(repositorio);
    }

    @Bean
    public CrearCliente crearCliente(RepositorioClientes repositorio, DirectorioClientes directorio,
                                     CifradorPin cifrador) {
        return new CrearCliente(repositorio, directorio, cifrador);
    }

    @Bean
    public CrearPrestamo crearPrestamo(RepositorioPrestamos repositorio, DirectorioClientes directorio,
                                       @Value("${app.monto-minimo}") BigDecimal montoMinimo) {
        return new CrearPrestamo(repositorio, directorio, montoMinimo);
    }

    @Bean
    public ConsultarPrestamos consultarPrestamos(RepositorioPrestamos repositorio, Clock reloj) {
        return new ConsultarPrestamos(repositorio, reloj);
    }

    @Bean
    public RegistrarPago registrarPago(RepositorioPrestamos repositorio, Clock reloj) {
        return new RegistrarPago(repositorio, reloj);
    }

    @Bean
    public DeshacerUltimoPago deshacerUltimoPago(RepositorioPrestamos repositorio) {
        return new DeshacerUltimoPago(repositorio);
    }

    @Bean
    public CondonarMulta condonarMulta(RepositorioPrestamos repositorio, Clock reloj) {
        return new CondonarMulta(repositorio, reloj);
    }

    @Bean
    public CancelarPrestamo cancelarPrestamo(RepositorioPrestamos repositorio, Clock reloj) {
        return new CancelarPrestamo(repositorio, reloj);
    }

    @Bean
    public ArmarPanel armarPanel(ConsultarPrestamos consultar, DirectorioClientes directorio) {
        return new ArmarPanel(consultar, directorio);
    }

    @Bean
    public GenerarRecibo generarRecibo(RepositorioPrestamos repositorio, DirectorioClientes directorio) {
        return new GenerarRecibo(repositorio, directorio);
    }

    @Bean
    public IniciarSesion iniciarSesion(DirectorioClientes directorio, CifradorPin cifrador,
                                       @Value("${app.prestamista.usuario}") String usuario,
                                       @Value("${app.prestamista.clave-cifrada}") String claveCifrada) {
        return new IniciarSesion(directorio, cifrador, usuario, claveCifrada);
    }

    @Bean
    public SimularAbono simularAbono(RepositorioPrestamos repositorio, Clock reloj) {
        return new SimularAbono(repositorio, reloj);
    }
}
