package io.github.juand13go.cartera.adaptadores.seguridad;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import io.github.juand13go.cartera.aplicacion.puertos.CifradorPin;

/**
 * Adaptador: cifra con BCrypt (de spring-security-crypto).
 * BCrypt agrega una "sal" aleatoria, así que el mismo PIN da un texto distinto cada vez,
 * y es lento a propósito para que adivinar PIN por fuerza bruta sea costoso.
 */
@Component
public class CifradorPinBCrypt implements CifradorPin {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String cifrar(String pin) {
        return bcrypt.encode(pin);
    }

    @Override
    public boolean coincide(String pin, String cifrado) {
        return bcrypt.matches(pin, cifrado);
    }
}
