package io.github.juand13go.cartera.aplicacion.puertos;

/**
 * Puerto: cifrar y comprobar PIN y contraseñas sin que la aplicación sepa qué algoritmo se usa.
 * El cifrado es de una sola vía: no se puede sacar el PIN del texto cifrado, solo comprobar si coincide.
 */
public interface CifradorPin {

    /** Texto cifrado del PIN; es lo único que se guarda. */
    String cifrar(String pin);

    /** true si el PIN escrito corresponde al texto cifrado guardado. */
    boolean coincide(String pin, String cifrado);
}
