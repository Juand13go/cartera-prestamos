package io.github.juand13go.cartera.adaptadores.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import io.github.juand13go.cartera.aplicacion.UsuarioSesion;

/** Dónde y cómo se guarda en la sesión HTTP quién inició sesión. */
public final class SesionWeb {

    /** Nombre del atributo de la sesión donde va el UsuarioSesion. */
    public static final String USUARIO = "usuario";

    private SesionWeb() {
    }

    /** Quién está en la sesión, o null si nadie ha entrado. No crea una sesión si no existe. */
    public static UsuarioSesion usuario(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        return sesion == null ? null : (UsuarioSesion) sesion.getAttribute(USUARIO);
    }

    /**
     * Empieza una sesión nueva para este usuario. La sesión anterior (si había) se descarta:
     * así nadie puede "plantar" un identificador de sesión antes del login y usarlo después (fijación de sesión).
     */
    public static void iniciar(HttpServletRequest peticion, UsuarioSesion usuario) {
        HttpSession anterior = peticion.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        peticion.getSession(true).setAttribute(USUARIO, usuario);
    }

    /** Cierra la sesión. */
    public static void cerrar(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }
}
