package io.github.juand13go.cartera.adaptadores.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.juand13go.cartera.aplicacion.CrearCliente;
import io.github.juand13go.cartera.aplicacion.DirectorioClientes;
import io.github.juand13go.cartera.aplicacion.puertos.CifradorPin;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioClientes;

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
}
