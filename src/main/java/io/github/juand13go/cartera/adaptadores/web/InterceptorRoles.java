package io.github.juand13go.cartera.adaptadores.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.juand13go.cartera.aplicacion.UsuarioSesion;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/**
 * Revisa en el backend CADA petición antes de que llegue al controlador. Ocultar botones en la vista
 * no basta: alguien podría escribir la dirección a mano o enviar un POST con otra herramienta.
 *
 * Funciona como lista blanca (solo pasa lo que está permitido explícitamente):
 * - Sin sesión: a /login. (Las rutas públicas, /login, /css y /error, ni siquiera pasan por aquí.)
 * - Prestamista: todo, menos el portal del cliente.
 * - Cliente: solo GET bajo /mis-prestamos, y solo de SUS préstamos; además puede salir. Lo demás: 403.
 */
@Component
public class InterceptorRoles implements HandlerInterceptor {

    private static final String PORTAL = "/mis-prestamos";

    private final RepositorioPrestamos prestamos;

    public InterceptorRoles(RepositorioPrestamos prestamos) {
        this.prestamos = prestamos;
    }

    /** Devuelve true si la petición puede seguir; false si ya se respondió (redirección o 403). */
    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object controlador)
            throws IOException {
        String ruta = rutaSinParametros(peticion);
        UsuarioSesion usuario = SesionWeb.usuario(peticion);

        if (usuario == null) {
            respuesta.sendRedirect(peticion.getContextPath() + "/login");
            return false;
        }
        if (usuario.esPrestamista()) {
            return ruta.startsWith(PORTAL) ? prohibir(respuesta) : true;
        }
        return permitirCliente(usuario, ruta, peticion, respuesta);
    }

    private boolean permitirCliente(UsuarioSesion cliente, String ruta, HttpServletRequest peticion,
                                    HttpServletResponse respuesta) throws IOException {
        boolean esGet = "GET".equals(peticion.getMethod());
        if (ruta.equals("/salir") && "POST".equals(peticion.getMethod())) {
            return true;
        }
        if (!esGet) {
            return prohibir(respuesta); // el cliente nunca modifica datos
        }
        if (ruta.equals("/")) {
            respuesta.sendRedirect(peticion.getContextPath() + PORTAL);
            return false;
        }
        if (ruta.equals(PORTAL)) {
            return true;
        }
        if (ruta.startsWith(PORTAL + "/")) {
            return esSuPrestamo(cliente, ruta) ? true : prohibir(respuesta);
        }
        return prohibir(respuesta);
    }

    /**
     * Para /mis-prestamos/{id}/...: el préstamo debe existir y ser del cliente de la sesión.
     * Si no existe también se prohíbe, para no revelar qué números de préstamo existen.
     */
    private boolean esSuPrestamo(UsuarioSesion cliente, String ruta) {
        String resto = ruta.substring((PORTAL + "/").length());
        String numero = resto.contains("/") ? resto.substring(0, resto.indexOf('/')) : resto;
        try {
            Prestamo prestamo = prestamos.buscarPorId(Long.parseLong(numero));
            return prestamo != null && prestamo.getCedulaCliente().equals(cliente.cedula());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * La ruta pedida, sin el contexto y sin lo que venga después de ';'.
     * Algunos servidores pegan ";jsessionid=..." a la dirección; si no se quitara,
     * "/mis-prestamos;jsessionid=abc" no sería igual a "/mis-prestamos" y se bloquearía por error.
     */
    private static String rutaSinParametros(HttpServletRequest peticion) {
        String ruta = peticion.getRequestURI().substring(peticion.getContextPath().length());
        int puntoYComa = ruta.indexOf(';');
        return puntoYComa >= 0 ? ruta.substring(0, puntoYComa) : ruta;
    }

    private static boolean prohibir(HttpServletResponse respuesta) throws IOException {
        respuesta.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
