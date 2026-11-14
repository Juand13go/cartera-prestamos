package io.github.juand13go.cartera.aplicacion;

import io.github.juand13go.cartera.aplicacion.puertos.CifradorPin;
import io.github.juand13go.cartera.dominio.modelo.Cliente;

/**
 * Caso de uso: comprobar quién entra. El usuario no escoge su rol: el sistema lo deduce de la cuenta.
 * - Si el identificador es el usuario del prestamista (de la configuración), se verifica su contraseña
 *   (guardada solo como hash BCrypt) y entra como PRESTAMISTA.
 * - Si no, se busca como cédula en la tabla hash del directorio y se verifica el PIN: entra como CLIENTE.
 * Si algo no coincide, el mensaje es siempre el mismo, para no revelar si falló el usuario o la clave.
 */
public class IniciarSesion {

    static final String CREDENCIALES_INCORRECTAS = "Usuario o clave incorrectos";

    private final DirectorioClientes directorio;
    private final CifradorPin cifrador;
    private final String usuarioPrestamista;
    private final String claveCifradaPrestamista;

    /**
     * El usuario del prestamista no puede ser solo números: se confundiría con la cédula de un cliente.
     * Como esto se construye al arrancar, una configuración así impide que la aplicación arranque, con el motivo.
     */
    public IniciarSesion(DirectorioClientes directorio, CifradorPin cifrador,
                         String usuarioPrestamista, String claveCifradaPrestamista) {
        if (usuarioPrestamista == null || usuarioPrestamista.isBlank()) {
            throw new IllegalArgumentException("Falta el usuario del prestamista en la configuración");
        }
        if (usuarioPrestamista.trim().matches("\\d+")) {
            throw new IllegalArgumentException("El usuario del prestamista no puede ser solo números ("
                    + usuarioPrestamista + "): se confundiría con una cédula");
        }
        this.directorio = directorio;
        this.cifrador = cifrador;
        this.usuarioPrestamista = usuarioPrestamista.trim();
        this.claveCifradaPrestamista = claveCifradaPrestamista;
    }

    /**
     * Un solo punto de entrada para todos.
     *
     * @param identificador el usuario del prestamista o la cédula de un cliente
     * @param clave         la contraseña del prestamista o el PIN del cliente
     */
    public UsuarioSesion entrar(String identificador, String clave) {
        if (identificador == null || clave == null) {
            throw new IllegalArgumentException(CREDENCIALES_INCORRECTAS);
        }
        String id = identificador.trim();
        if (id.equals(usuarioPrestamista)) {
            return prestamista(clave);
        }
        return cliente(id, clave);
    }

    private UsuarioSesion prestamista(String clave) {
        if (!cifrador.coincide(clave, claveCifradaPrestamista)) {
            throw new IllegalArgumentException(CREDENCIALES_INCORRECTAS);
        }
        return new UsuarioSesion(Rol.PRESTAMISTA, null, usuarioPrestamista);
    }

    /** La búsqueda por cédula es O(1): usa la tabla hash del directorio. */
    private UsuarioSesion cliente(String cedula, String pin) {
        Cliente cliente = directorio.buscar(cedula);
        if (cliente == null || !cifrador.coincide(pin, cliente.getPinCifrado())) {
            throw new IllegalArgumentException(CREDENCIALES_INCORRECTAS);
        }
        return new UsuarioSesion(Rol.CLIENTE, cliente.getCedula(), cliente.getNombre());
    }
}
