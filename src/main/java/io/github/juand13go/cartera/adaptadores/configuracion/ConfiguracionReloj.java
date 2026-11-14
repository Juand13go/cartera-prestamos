package io.github.juand13go.cartera.adaptadores.configuracion;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El "hoy" del sistema. Todo el código pide la fecha a este reloj, nunca a LocalDate.now() suelto,
 * para que en la demo se pueda simular otra fecha con la propiedad app.fecha-simulada.
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock reloj(@Value("${app.zona-horaria}") String zonaHoraria,
                       @Value("${app.fecha-simulada:}") String fechaSimulada) {
        return crearReloj(ZoneId.of(zonaHoraria), fechaSimulada);
    }

    /**
     * Sin fecha simulada: el reloj real en la zona dada.
     * Con fecha simulada: el reloj real corrido los días que haga falta para que "hoy" sea esa fecha.
     * La hora sigue siendo la real, así los recibos muestran una hora con sentido.
     */
    static Clock crearReloj(ZoneId zona, String fechaSimulada) {
        Clock real = Clock.system(zona);
        if (fechaSimulada == null || fechaSimulada.isBlank()) {
            return real;
        }
        LocalDate hoyReal = LocalDate.now(real);
        LocalDate simulada = LocalDate.parse(fechaSimulada.trim());
        Duration corrimiento = Duration.ofDays(ChronoUnit.DAYS.between(hoyReal, simulada));
        return Clock.offset(real, corrimiento);
    }
}
