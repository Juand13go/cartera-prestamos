-- Esquema de la cartera. La base de datos guarda solo lo que pasó:
-- clientes, el contrato de cada préstamo, sus pagos en orden y las condonaciones de multa.
-- Cuotas, estados, multas y saldos NO se guardan: el dominio los calcula repitiendo los pagos.
-- SQL estándar, para que funcione igual en H2 (modo PostgreSQL) y en PostgreSQL.

CREATE TABLE cliente (
    cedula       VARCHAR(10)   PRIMARY KEY,
    nombre       VARCHAR(100)  NOT NULL,
    telefono     VARCHAR(10)   NOT NULL,
    direccion    VARCHAR(200)  NOT NULL DEFAULT '',
    referencia   VARCHAR(200)  NOT NULL DEFAULT '',
    cupo_maximo  NUMERIC(15,0) NOT NULL CHECK (cupo_maximo >= 0),
    notas        VARCHAR(1000) NOT NULL DEFAULT '',
    pin_cifrado  VARCHAR(100)  NOT NULL
);

-- Cada fila es un préstamo con todas las condiciones de su contrato.
CREATE TABLE prestamo (
    id                  BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cedula_cliente      VARCHAR(10)   NOT NULL REFERENCES cliente (cedula),
    capital             NUMERIC(15,0) NOT NULL CHECK (capital > 0),
    tasa_por_periodo    NUMERIC(7,4)  NOT NULL CHECK (tasa_por_periodo > 0),
    base_interes        VARCHAR(20)   NOT NULL CHECK (base_interes IN ('CAPITAL_INICIAL', 'CAPITAL_VIGENTE')),
    fecha_desembolso    DATE          NOT NULL,
    frecuencia          VARCHAR(10)   NOT NULL CHECK (frecuencia IN ('MENSUAL', 'QUINCENAL', 'SEMANAL')),
    -- Mensual: día del mes (1 a 31). Semanal: día de la semana (1 = lunes ... 7 = domingo). Quincenal: vacío.
    dia_cobro           INTEGER,
    calendario          VARCHAR(20)   NOT NULL CHECK (calendario IN ('TODOS_LOS_DIAS', 'SIN_DOMINGOS')),
    politica_mora       VARCHAR(20)   NOT NULL
        CHECK (politica_mora IN ('SIN_MULTA', 'MULTA_FIJA_DIARIA', 'PORCENTAJE_UNICO', 'PORCENTAJE_DIARIO')),
    -- Valor diario (multa fija) o porcentaje (las dos de porcentaje). Vacío con SIN_MULTA.
    valor_mora          NUMERIC(15,4),
    dias_gracia         INTEGER       NOT NULL CHECK (dias_gracia >= 0),
    orden_imputacion    VARCHAR(20)   NOT NULL CHECK (orden_imputacion IN ('MULTAS_PRIMERO', 'CUOTAS_PRIMERO')),
    al_vencer_plazo     VARCHAR(20)   NOT NULL CHECK (al_vencer_plazo IN ('TRATAR_COMO_MORA', 'SEGUIR_COBRANDO')),
    interes_cancelacion VARCHAR(20)   NOT NULL CHECK (interes_cancelacion IN ('PERIODO_COMPLETO', 'POR_DIAS'))
);

CREATE INDEX idx_prestamo_cliente ON prestamo (cedula_cliente);

-- Pagos en el orden en que ocurrieron: el id crece con cada pago, así que ordenar por id da ese orden.
-- El id también es el número de recibo.
CREATE TABLE pago (
    id             BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prestamo_id    BIGINT        NOT NULL REFERENCES prestamo (id),
    fecha          DATE          NOT NULL,
    monto          NUMERIC(15,0) NOT NULL CHECK (monto > 0),
    cancelacion    BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Fecha y hora en que se registró, para el recibo.
    registrado_en  TIMESTAMP     NOT NULL
);

CREATE INDEX idx_pago_prestamo ON pago (prestamo_id);

-- Una condonación por cuota como máximo: la llave es (préstamo, número de cuota).
CREATE TABLE condonacion (
    prestamo_id      BIGINT        NOT NULL REFERENCES prestamo (id),
    numero_cuota     INTEGER       NOT NULL CHECK (numero_cuota >= 1),
    -- Lo que ya estaba pagado de la multa al condonar; la multa queda fija en ese valor.
    multa_fijada_en  NUMERIC(15,0) NOT NULL CHECK (multa_fijada_en >= 0),
    PRIMARY KEY (prestamo_id, numero_cuota)
);
