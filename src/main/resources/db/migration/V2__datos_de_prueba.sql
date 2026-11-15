-- Datos de prueba INVENTADOS para la demostración: los nombres, cédulas y teléfonos parecen reales, pero no son de nadie.
-- Están pensados para que los tres préstamos tengan un cobro en la semana del 14 al 21 de noviembre de 2026.
-- Todos los clientes tienen PIN 1234 (aquí va solo su hash BCrypt).
-- Solo se guarda lo que pasó: contratos y pagos. Estados, cuotas y multas los calcula el sistema.

INSERT INTO cliente (cedula, nombre, telefono, direccion, referencia, cupo_maximo, notas, pin_cifrado) VALUES
    ('43586217',   'Luz Marina Restrepo Arango',  '3127845930', 'Calle 49 # 72-15, Laureles, Medellín',  'Su hija Paula Andrea',                  5000000, 'Paga puntual, casi siempre en efectivo.',          '$2a$10$zAGyC/15rXuoYDRbWCVnW.tmbofm.Pit9KRlAgCFYXuTeJ6LZl7.m'),
    ('71793468',   'Jhon Fredy Zapata Muñoz',     '3006419827', 'Carrera 65 # 98-40, Castilla, Medellín', 'Don Hernán, el de la ferretería',       3000000, 'Se atrasa; llamarlo el miércoles antes del cobro.', '$2a$10$zAGyC/15rXuoYDRbWCVnW.tmbofm.Pit9KRlAgCFYXuTeJ6LZl7.m'),
    ('1037624519', 'Gloria Patricia Cardona Vélez', '3214587063', 'Calle 32 Sur # 43A-21, Envigado',      'Su esposo Wilson',                      8000000, 'Abona a capital cuando le pagan la prima.',         '$2a$10$zAGyC/15rXuoYDRbWCVnW.tmbofm.Pit9KRlAgCFYXuTeJ6LZl7.m');

-- 1. Luz Marina: 2.000.000 al 8 % mensual, el día 18, valores por defecto. Al día.
--    Pagó el 18 sep y el 18 oct; la cuota de 160.000 del miércoles 18 nov está en la agenda.
INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso, frecuencia, dia_cobro,
    calendario, politica_mora, valor_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion)
VALUES ('43586217', 2000000, 8, 'CAPITAL_INICIAL', DATE '2026-08-18', 'MENSUAL', 18,
    'TODOS_LOS_DIAS', 'SIN_MULTA', NULL, 2, 'MULTAS_PRIMERO', 'TRATAR_COMO_MORA', 'PERIODO_COMPLETO');

-- 2. Jhon Fredy: 1.000.000 al 4 % semanal (40.000), los jueves, multa de 2.000 diarios.
--    Pagó el 22 y el 29 oct, pero no el 5 ni el 12 nov: está en mora y además tiene la cuota del jueves 19 nov en la agenda.
INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso, frecuencia, dia_cobro,
    calendario, politica_mora, valor_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion)
VALUES ('71793468', 1000000, 4, 'CAPITAL_INICIAL', DATE '2026-10-15', 'SEMANAL', 4,
    'TODOS_LOS_DIAS', 'MULTA_FIJA_DIARIA', 2000, 2, 'MULTAS_PRIMERO', 'TRATAR_COMO_MORA', 'PERIODO_COMPLETO');

-- 3. Gloria Patricia: 5.000.000 al 3 % quincenal, capital vigente, sin domingos, 5 % único de multa.
--    El 31 oct abonó 1.000.000 a capital: su cuota bajó de 150.000 a 120.000.
--    La del 15 nov cae domingo y pasa al lunes 16 nov.
INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso, frecuencia, dia_cobro,
    calendario, politica_mora, valor_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion)
VALUES ('1037624519', 5000000, 3, 'CAPITAL_VIGENTE', DATE '2026-09-30', 'QUINCENAL', NULL,
    'SIN_DOMINGOS', 'PORCENTAJE_UNICO', 5, 2, 'MULTAS_PRIMERO', 'TRATAR_COMO_MORA', 'PERIODO_COMPLETO');

-- Pagos, en orden. El préstamo se identifica por cliente y fecha de desembolso para no depender de los ids.
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-09-18', 160000, FALSE, TIMESTAMP '2026-09-18 09:20:00' FROM prestamo
WHERE cedula_cliente = '43586217' AND fecha_desembolso = DATE '2026-08-18';
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-10-18', 160000, FALSE, TIMESTAMP '2026-10-18 10:05:00' FROM prestamo
WHERE cedula_cliente = '43586217' AND fecha_desembolso = DATE '2026-08-18';

INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-10-22', 40000, FALSE, TIMESTAMP '2026-10-22 18:10:00' FROM prestamo
WHERE cedula_cliente = '71793468' AND fecha_desembolso = DATE '2026-10-15';
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-10-29', 40000, FALSE, TIMESTAMP '2026-10-29 17:45:00' FROM prestamo
WHERE cedula_cliente = '71793468' AND fecha_desembolso = DATE '2026-10-15';

INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-10-15', 150000, FALSE, TIMESTAMP '2026-10-15 11:30:00' FROM prestamo
WHERE cedula_cliente = '1037624519' AND fecha_desembolso = DATE '2026-09-30';
INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en)
SELECT id, DATE '2026-10-31', 1150000, FALSE, TIMESTAMP '2026-10-31 12:15:00' FROM prestamo
WHERE cedula_cliente = '1037624519' AND fecha_desembolso = DATE '2026-09-30';
