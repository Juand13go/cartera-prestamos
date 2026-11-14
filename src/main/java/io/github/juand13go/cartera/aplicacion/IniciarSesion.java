package io.github.juand13go.cartera.aplicacion;

import io.github.juand13go.cartera.aplicacion.puertos.CifradorPin;
import io.github.juand13go.cartera.dominio.modelo.Cliente;

/**
 * Caso de uso: comprobar quién entra.
 * - Prestamista: usuario y contraseña de la configuración (la contraseña guardada solo como hash BCrypt).
 * - Cliente: cédula (se busca en la tabla hash del directorio) y PIN.
 * Si algo no coincide, el mensaje es siempre el mismo, para no revelar si falló el usuario o la clave.
 */
public class IniciarSesion {

    static final String CREDENCIALES_INCORRECTAS = "Usuario o clave incorrectos";

    private final DirectorioClientes directorio;
    private final CifradorPin cifrador;
    private final String usuarioPrestamista;
    private final String claveCifradaPrestamista;

    public IniciarSesion(DirectorioClientes directorio, CifradorPin cifrador,
                         String usuarioPrestamista, String claveCifradaPrestamista) {
        this.directorio = directorio;
        this.cifrador = cifrador;
        this.usuarioPrestamista = usuarioPrestamista;
        this.claveCifradaPrestamista = claveCifradaPrestamista;
    }

    public UsuarioSesion prestamista(String usuario, String clave) {
        if (usuario == null || clave == null || !usuario.trim().equals(usuarioPrestamista)
                || !cifrador.coincide(clave, claveCifradaPrestamista)) {
            throw new IllegalArgumentException(CREDENCIALES_INCORRECTAS);
        }
        return new UsuarioSesion(Rol.PRESTAMISTA, null, usuarioPrestamista);
    }

    /** La búsqueda por cédula es O(1): usa la tabla hash del directorio. */
    public UsuarioSesion cliente(String cedula, String pin) {
        Cliente cliente = directorio.buscar(cedula);
        if (cliente == null || pin == null || !cifrador.coincide(pin, cliente.getPinCifrado())) {
            throw new IllegalArgumentException(CREDENCIALES_INCORRECTAS);
        }
        return new UsuarioSesion(Rol.CLIENTE, cliente.getCedula(), cliente.getNombre());
    }
}
