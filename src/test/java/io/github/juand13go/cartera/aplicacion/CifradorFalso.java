package io.github.juand13go.cartera.aplicacion;

import io.github.juand13go.cartera.aplicacion.puertos.CifradorPin;

/** Cifrador falso y predecible para las pruebas: "1234" se "cifra" como "cifrado:1234". */
class CifradorFalso implements CifradorPin {

    @Override
    public String cifrar(String pin) {
        return "cifrado:" + pin;
    }

    @Override
    public boolean coincide(String pin, String cifrado) {
        return cifrado.equals(cifrar(pin));
    }
}
