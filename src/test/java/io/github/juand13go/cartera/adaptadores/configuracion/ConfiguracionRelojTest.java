package io.github.juand13go.cartera.adaptadores.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

class ConfiguracionRelojTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Test
    void sinFechaSimuladaHoyEsLaFechaRealEnBogota() {
        Clock reloj = ConfiguracionReloj.crearReloj(BOGOTA, "");

        assertEquals(LocalDate.now(BOGOTA), LocalDate.now(reloj));
        assertEquals(BOGOTA, reloj.getZone());
    }

    @Test
    void conFechaSimuladaHoyEsEsaFecha() {
        Clock reloj = ConfiguracionReloj.crearReloj(BOGOTA, "2027-01-20");

        assertEquals(LocalDate.of(2027, 1, 20), LocalDate.now(reloj));
    }

    @Test
    void unaFechaSimuladaMalEscritaNoSeIgnoraEnSilencio() {
        assertThrows(DateTimeParseException.class, () -> ConfiguracionReloj.crearReloj(BOGOTA, "20/01/2027"));
    }
}
