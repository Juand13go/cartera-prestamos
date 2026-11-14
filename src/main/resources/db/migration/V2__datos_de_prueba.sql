-- Datos de prueba INVENTADOS (no son personas reales) para la demostración.
-- Todos los clientes tienen PIN 1234 (aquí va solo su hash BCrypt).
-- Solo se guarda lo que pasó: contratos y pagos. Estados, cuotas y multas los calcula el sistema.

INSERT INTO cliente (cedula, nombre, telefono, direccion, referencia, cupo_maximo, notas, pin_cifrado) VALUES
    ('1000000001', 'Laura Gómez (prueba)',   '3000000001', 'Calle 10 # 20-30', 'Su hermana Diana',  5000000, 'Paga puntual.',                   '$2a$10$zAGyC/15rXuoYDRbWCVnW.tmbofm.Pit9KRlAgCFYXuTeJ6LZl7.m'),
    ('1000000002', 'Carlos Mejía (prueba)',  '3000000002', 'Carrera 5 # 6-7',  'El tendero Pedro',  3000000, 'Se atrasa; llamar antes del 5.',  '$2a$10$zAGyC/15rXuoYDRbWCVnW.tmbofm.Pit9KRlAgCFYXuTeJ6LZl7.m'),
    ('1000000003', 'Marta Ospina (prueba)',  '3000000003', 'Diagonal 8 # 9-10', 'Su jefe Andrés',    8000000, 'Abona a capital cuando puede.',   '$2a$10$zAGyC/15rXuoYDRbWCVnW.tmbofm.Pit9KRlAgCFYXuTeJ6LZl7.m'),
    ('1000000004', 'Jorge Castaño (prueba)', '3000000004', '',                  '',                  2000000, '',                                '$2a$10$zAGyC/15rXuoYDRbWCVnW.tmbofm.Pit9KRlAgCFYXuTeJ6LZl7.m');

-- 1. Laura: 2.000.000 al 8 % mensual, el día 10, valores por defecto. Al día.
INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso, frecuencia, dia_cobro,
    calendario, politica_mora, valor_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion)
VALUES ('1000000001', 2000000, 8, 'CAPITAL_INICIAL', DATE '2026-08-10', 'MENSUAL', 10,
    'TODOS_LOS_DIAS', 'SIN_MULTA', NULL, 2, 'MULTAS_PRIMERO', 'TRATAR_COMO_MORA', 'PERIODO_COMPLETO');

-- 2. Laura: 300.000 al 3 % semanal, los martes, cuotas primero y 1 % diario de multa. Al día.
INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso, frecuencia, dia_cobro,
    calendario, politica_mora, valor_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion)
VALUES ('1000000001', 300000, 3, 'CAPITAL_INICIAL', DATE '2026-11-03', 'SEMANAL', 2,
    'TODOS_LOS_DIAS', 'PORCENTAJE_DIARIO', 1, 2, 'CUOTAS_PRIMERO', 'TRATAR_COMO_MORA', 'PERIODO_COMPLETO');

-- 3. Carlos: 1.500.000 al 10 % mensual, el día 5, multa de 2.000 diarios. Dejó de pagar en octubre: en mora.
INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso, frecuencia, dia_cobro,
    calendario, politica_mora, valor_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion)
VALUES ('1000000002', 1500000, 10, 'CAPITAL_INICIAL', DATE '2026-07-05', 'MENSUAL', 5,
    'TODOS_LOS_DIAS', 'MULTA_FIJA_DIARIA', 2000, 2, 'MULTAS_PRIMERO', 'TRATAR_COMO_MORA', 'PERIODO_COMPLETO');

-- 4. Marta: 5.000.000 al 3 % quincenal, capital vigente, sin domingos, 5 % único de multa.
--    El 15 oct abonó 1.000.000 a capital: sus cuotas bajaron de 150.000 a 120.000.
INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso, frecuencia, dia_cobro,
    calendario, politica_mora, valor_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion)
VALUES ('1000000003', 5000000, 3, 'CAPITAL_VIGENTE', DATE '2026-09-15', 'QUINCENAL', NULL,
    'SIN_DOMINGOS', 'PORCENTAJE_UNICO', 5, 2, 'MULTAS_PRIMERO', 'TRATAR_COMO_MORA', 'PERIODO_COMPLETO');

-- 5. Jorge: 1.000.000 al 10 % mensual, interés por días al cancelar. Lo canceló el 1 sep: cerrado.
INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso, frecuencia, dia_cobro,
    calendario, politica_mora, valor_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion)
VALUES ('1000000004', 1000000, 10, 'CAPITAL_INICIAL', DATE '2026-06-20', 'MENSUAL', 20,
    'TODOS_LOS_DIAS', 'SIN_MULTA', NULL, 2, 'MULTAS_PRIMERO', 'TRATAR_COMO_MORA', 'POR_DIAS');

-- Pagos, en orden. El préstamo se identifica por cliente y fecha de desembolso para no depender de los ids.
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-09-10', 160000, FALSE, TIMESTAMP '2026-09-10 09:15:00' FROM prestamo
WHERE cedula_cliente = '1000000001' AND fecha_desembolso = DATE '2026-08-10';
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-10-10', 160000, FALSE, TIMESTAMP '2026-10-10 10:02:00' FROM prestamo
WHERE cedula_cliente = '1000000001' AND fecha_desembolso = DATE '2026-08-10';
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-11-10', 160000, FALSE, TIMESTAMP '2026-11-10 08:47:00' FROM prestamo
WHERE cedula_cliente = '1000000001' AND fecha_desembolso = DATE '2026-08-10';

INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-11-10', 9000, FALSE, TIMESTAMP '2026-11-10 17:30:00' FROM prestamo
WHERE cedula_cliente = '1000000001' AND fecha_desembolso = DATE '2026-11-03';

INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-08-05', 150000, FALSE, TIMESTAMP '2026-08-05 11:20:00' FROM prestamo
WHERE cedula_cliente = '1000000002' AND fecha_desembolso = DATE '2026-07-05';
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-09-07', 150000, FALSE, TIMESTAMP '2026-09-07 16:05:00' FROM prestamo
WHERE cedula_cliente = '1000000002' AND fecha_desembolso = DATE '2026-07-05';

INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-09-30', 150000, FALSE, TIMESTAMP '2026-09-30 12:00:00' FROM prestamo
WHERE cedula_cliente = '1000000003' AND fecha_desembolso = DATE '2026-09-15';
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-10-15', 1150000, FALSE, TIMESTAMP '2026-10-15 12:10:00' FROM prestamo
WHERE cedula_cliente = '1000000003' AND fecha_desembolso = DATE '2026-09-15';
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-10-31', 120000, FALSE, TIMESTAMP '2026-10-31 09:40:00' FROM prestamo
WHERE cedula_cliente = '1000000003' AND fecha_desembolso = DATE '2026-09-15';

INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-07-20', 100000, FALSE, TIMESTAMP '2026-07-20 10:00:00' FROM prestamo
WHERE cedula_cliente = '1000000004' AND fecha_desembolso = DATE '2026-06-20';
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-08-20', 100000, FALSE, TIMESTAMP '2026-08-20 10:00:00' FROM prestamo
WHERE cedula_cliente = '1000000004' AND fecha_desembolso = DATE '2026-06-20';
-- Cancelación por días: periodo 20 ago - 20 sep (31 días), 12 días corridos.
-- 100.000 × 12 / 31 = 38.709,68 → 38.710 de interés + 1.000.000 de capital. El sistema lo recalcula al cargar.
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-09-01', 1038710, TRUE, TIMESTAMP '2026-09-01 15:30:00' FROM prestamo
WHERE cedula_cliente = '1000000004' AND fecha_desembolso = DATE '2026-06-20';
