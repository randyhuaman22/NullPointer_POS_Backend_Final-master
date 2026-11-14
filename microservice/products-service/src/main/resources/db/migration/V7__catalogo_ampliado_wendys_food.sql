-- Carga idempotente de productos de demostración para las cuatro áreas de Wendy's Food.
-- Los precios son REFERENCIALES, no deben considerarse la carta oficial del negocio.
-- Catálogo inicial de demostración. Los precios son referenciales y el administrador puede modificarlos.
INSERT INTO esquema_menu.categorias_productos(id_area_negocio,nombre,descripcion,orden_visualizacion)
SELECT a.id_area_negocio, 'Caldos', 'Categoría caldos de menu', 1 FROM esquema_menu.areas_negocio a WHERE a.codigo='MENU' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Caldos'))
UNION ALL
SELECT a.id_area_negocio, 'Chifa', 'Categoría chifa de menu', 2 FROM esquema_menu.areas_negocio a WHERE a.codigo='MENU' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Chifa'))
UNION ALL
SELECT a.id_area_negocio, 'Criollos', 'Categoría criollos de menu', 3 FROM esquema_menu.areas_negocio a WHERE a.codigo='MENU' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Criollos'))
UNION ALL
SELECT a.id_area_negocio, 'Hamburguesas y sánguches', 'Categoría hamburguesas y sánguches de menu', 4 FROM esquema_menu.areas_negocio a WHERE a.codigo='MENU' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Hamburguesas y sánguches'))
UNION ALL
SELECT a.id_area_negocio, 'Anticuchos', 'Categoría anticuchos de menu', 5 FROM esquema_menu.areas_negocio a WHERE a.codigo='MENU' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Anticuchos'))
UNION ALL
SELECT a.id_area_negocio, 'Menú del día', 'Categoría menú del día de menu', 6 FROM esquema_menu.areas_negocio a WHERE a.codigo='MENU' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Menú del día'))
UNION ALL
SELECT a.id_area_negocio, 'Sopas', 'Categoría sopas de menu', 7 FROM esquema_menu.areas_negocio a WHERE a.codigo='MENU' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Sopas'))
UNION ALL
SELECT a.id_area_negocio, 'Pollo a la brasa', 'Categoría pollo a la brasa de polleria', 8 FROM esquema_menu.areas_negocio a WHERE a.codigo='POLLERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Pollo a la brasa'))
UNION ALL
SELECT a.id_area_negocio, 'Broaster y plancha', 'Categoría broaster y plancha de polleria', 9 FROM esquema_menu.areas_negocio a WHERE a.codigo='POLLERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Broaster y plancha'))
UNION ALL
SELECT a.id_area_negocio, 'Mostritos y salchipapas', 'Categoría mostritos y salchipapas de polleria', 10 FROM esquema_menu.areas_negocio a WHERE a.codigo='POLLERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Mostritos y salchipapas'))
UNION ALL
SELECT a.id_area_negocio, 'Alitas', 'Categoría alitas de polleria', 11 FROM esquema_menu.areas_negocio a WHERE a.codigo='POLLERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Alitas'))
UNION ALL
SELECT a.id_area_negocio, 'Guarniciones', 'Categoría guarniciones de polleria', 12 FROM esquema_menu.areas_negocio a WHERE a.codigo='POLLERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Guarniciones'))
UNION ALL
SELECT a.id_area_negocio, 'Combos familiares', 'Categoría combos familiares de polleria', 13 FROM esquema_menu.areas_negocio a WHERE a.codigo='POLLERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Combos familiares'))
UNION ALL
SELECT a.id_area_negocio, 'Helados', 'Categoría helados de heladeria', 14 FROM esquema_menu.areas_negocio a WHERE a.codigo='HELADERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Helados'))
UNION ALL
SELECT a.id_area_negocio, 'Conos y vasos', 'Categoría conos y vasos de heladeria', 15 FROM esquema_menu.areas_negocio a WHERE a.codigo='HELADERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Conos y vasos'))
UNION ALL
SELECT a.id_area_negocio, 'Postres y waffles', 'Categoría postres y waffles de heladeria', 16 FROM esquema_menu.areas_negocio a WHERE a.codigo='HELADERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Postres y waffles'))
UNION ALL
SELECT a.id_area_negocio, 'Frutas y vasos', 'Categoría frutas y vasos de heladeria', 17 FROM esquema_menu.areas_negocio a WHERE a.codigo='HELADERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Frutas y vasos'))
UNION ALL
SELECT a.id_area_negocio, 'Jugos', 'Categoría jugos de jugueria', 18 FROM esquema_menu.areas_negocio a WHERE a.codigo='JUGUERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Jugos'))
UNION ALL
SELECT a.id_area_negocio, 'Batidos', 'Categoría batidos de jugueria', 19 FROM esquema_menu.areas_negocio a WHERE a.codigo='JUGUERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Batidos'))
UNION ALL
SELECT a.id_area_negocio, 'Frappés y frozen', 'Categoría frappés y frozen de jugueria', 20 FROM esquema_menu.areas_negocio a WHERE a.codigo='JUGUERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Frappés y frozen'))
UNION ALL
SELECT a.id_area_negocio, 'Cafetería fría', 'Categoría cafetería fría de jugueria', 21 FROM esquema_menu.areas_negocio a WHERE a.codigo='JUGUERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Cafetería fría'))
UNION ALL
SELECT a.id_area_negocio, 'Yogures', 'Categoría yogures de jugueria', 22 FROM esquema_menu.areas_negocio a WHERE a.codigo='JUGUERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Yogures'))
UNION ALL
SELECT a.id_area_negocio, 'Extractos', 'Categoría extractos de jugueria', 23 FROM esquema_menu.areas_negocio a WHERE a.codigo='JUGUERIA' AND NOT EXISTS (SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre)=LOWER('Extractos'));
INSERT INTO esquema_menu.productos
(codigo_producto,id_categoria,id_estacion_cocina,nombre,descripcion,precio_unitario,disponible,activo,tiempo_preparacion_minutos,es_destacado,orden_visualizacion)
SELECT v.codigo, c.id_categoria, e.id_estacion_cocina, v.nombre, v.descripcion, v.precio, TRUE, TRUE, v.tiempo, v.destacado, v.orden
FROM (VALUES
('CALDO_GALLINA','MENU','Caldos','Caldo de Gallina','Caldo de Gallina',16,'MENU',12,TRUE,1),
('CALDO_GALLINA_ACEV','MENU','Caldos','Caldo de Gallina Acevichado','Caldo de Gallina Acevichado',19,'MENU',12,FALSE,2),
('ARROZ_CHAUFA','MENU','Chifa','Arroz Chaufa','Arroz Chaufa',15,'MENU',12,TRUE,3),
('AEROPUERTO','MENU','Chifa','Aeropuerto','Aeropuerto',17,'MENU',12,FALSE,4),
('TALLARIN_SALTADO','MENU','Chifa','Tallarín Saltado','Tallarín Saltado',17,'MENU',12,FALSE,5),
('WANTAN_FRITO','MENU','Chifa','Wantán Frito','Wantán Frito',10,'MENU',12,FALSE,6),
('SALTADO_POLLO','MENU','Criollos','Saltado de Pollo','Saltado de Pollo',18,'MENU',12,FALSE,7),
('CHICHARRON_POLLO_FREJOL','MENU','Criollos','Chicharrón de Pollo con Frejol','Chicharrón de Pollo con Frejol',19,'MENU',12,FALSE,8),
('CHICHARRON_GALLINA_CLAS','MENU','Criollos','Chicharrón de Gallina Clásico','Chicharrón de Gallina Clásico',22,'MENU',12,FALSE,9),
('CHICHARRON_GALLINA_ACEV','MENU','Criollos','Chicharrón de Gallina Acevichado','Chicharrón de Gallina Acevichado',24,'MENU',12,FALSE,10),
('PAN_POLLO','MENU','Hamburguesas y sánguches','Pan con Pollo','Pan con Pollo',8,'MENU',12,FALSE,11),
('BURG_CLASICA','MENU','Hamburguesas y sánguches','Burguer Artesanal Clásica','Burguer Artesanal Clásica',14,'MENU',12,FALSE,12),
('BURG_POBRE','MENU','Hamburguesas y sánguches','Burguer Artesanal a lo Pobre','Burguer Artesanal a lo Pobre',18,'MENU',12,FALSE,13),
('BURG_WENDYS','MENU','Hamburguesas y sánguches','Burguer Artesanal La Wendy''s','Burguer Artesanal La Wendy''s',19,'MENU',12,TRUE,14),
('BURG_CHEESE','MENU','Hamburguesas y sánguches','Burguer Artesanal La Cheese','Burguer Artesanal La Cheese',17,'MENU',12,FALSE,15),
('ANTICUCHO','MENU','Anticuchos','Anticucho','Anticucho',8,'PARRILLA',12,FALSE,16),
('MENU_DIA','MENU','Menú del día','Menú del Día','Menú del Día',12,'MENU',12,FALSE,17),
('SOPA_DIA','MENU','Sopas','Sopa del Día','Sopa del Día',8,'MENU',12,FALSE,18),
('LOMO_SALTADO','MENU','Criollos','Lomo Saltado','Lomo Saltado',20,'MENU',12,FALSE,19),
('AJI_GALLINA','MENU','Criollos','Ají de Gallina','Ají de Gallina',16,'MENU',12,FALSE,20),
('ARROZ_POLLO','MENU','Criollos','Arroz con Pollo','Arroz con Pollo',15,'MENU',12,FALSE,21),
('TALLARIN_VERDE','MENU','Criollos','Tallarines Verdes con Pollo','Tallarines Verdes con Pollo',16,'MENU',12,FALSE,22),
('MILANESA_POLLO','MENU','Criollos','Milanesa de Pollo','Milanesa de Pollo',17,'MENU',12,FALSE,23),
('PESCADO_FRITO','MENU','Criollos','Pescado Frito','Pescado Frito',20,'MENU',12,FALSE,24),
('POLLO_ENTERO','POLLERIA','Pollo a la brasa','Pollo a la Brasa Entero','Pollo a la Brasa Entero',68,'PARRILLA',15,TRUE,25),
('POLLO_MEDIO','POLLERIA','Pollo a la brasa','Medio Pollo a la Brasa','Medio Pollo a la Brasa',36,'PARRILLA',15,FALSE,26),
('POLLO_CUARTO','POLLERIA','Pollo a la brasa','1/4 de Pollo a la Brasa','1/4 de Pollo a la Brasa',20,'PARRILLA',15,TRUE,27),
('POLLO_OCTAVO','POLLERIA','Pollo a la brasa','1/8 de Pollo a la Brasa','1/8 de Pollo a la Brasa',13,'PARRILLA',15,FALSE,28),
('BROASTER_FREJOL','POLLERIA','Broaster y plancha','Broaster con Frejol','Broaster con Frejol',18,'PARRILLA',15,FALSE,29),
('SALCHIBROASTER','POLLERIA','Broaster y plancha','Salchibroaster','Salchibroaster',17,'PARRILLA',15,FALSE,30),
('POLLO_PLANCHA','POLLERIA','Broaster y plancha','Pollo a la Plancha','Pollo a la Plancha',18,'PARRILLA',15,FALSE,31),
('BROASTER_1P','POLLERIA','Broaster y plancha','Pollo Broaster Personal','Pollo Broaster Personal',16,'PARRILLA',15,FALSE,32),
('MOSTRITO','POLLERIA','Mostritos y salchipapas','Mostrito','Mostrito',20,'PARRILLA',15,TRUE,33),
('MOSTRO_BRASA','POLLERIA','Mostritos y salchipapas','Mostrito de Pollo a la Brasa','Mostrito de Pollo a la Brasa',22,'PARRILLA',15,FALSE,34),
('SALCHIPAPA','POLLERIA','Mostritos y salchipapas','Salchipapa','Salchipapa',12,'PARRILLA',15,FALSE,35),
('SALCHIPOLLO','POLLERIA','Mostritos y salchipapas','Salchipollo','Salchipollo',15,'PARRILLA',15,FALSE,36),
('ALITAS_BBQ','POLLERIA','Alitas','Alitas BBQ','Alitas BBQ',18,'PARRILLA',15,FALSE,37),
('ALITAS_MARACUYA','POLLERIA','Alitas','Alitas Maracuyá','Alitas Maracuyá',19,'PARRILLA',15,FALSE,38),
('ALITAS_ACEV','POLLERIA','Alitas','Alitas Acevichadas','Alitas Acevichadas',19,'PARRILLA',15,FALSE,39),
('ALITAS_BUFALO','POLLERIA','Alitas','Alitas Búfalo','Alitas Búfalo',19,'PARRILLA',15,FALSE,40),
('ALITAS_BBQ_12','POLLERIA','Alitas','Alitas BBQ x 12','Alitas BBQ x 12',32,'PARRILLA',15,FALSE,41),
('PAPAS_FRITAS','POLLERIA','Guarniciones','Papas Fritas','Papas Fritas',7,'COCINA',15,FALSE,42),
('ARROZ_PORCION','POLLERIA','Guarniciones','Porción de Arroz','Porción de Arroz',4,'COCINA',15,FALSE,43),
('FREJOL_PORCION','POLLERIA','Guarniciones','Porción de Frejol','Porción de Frejol',5,'COCINA',15,FALSE,44),
('ENSALADA','POLLERIA','Guarniciones','Ensalada Fresca','Ensalada Fresca',6,'COCINA',15,FALSE,45),
('YUCA_FRITA','POLLERIA','Guarniciones','Yuca Frita','Yuca Frita',7,'COCINA',15,FALSE,46),
('PLATANO_FRITO','POLLERIA','Guarniciones','Plátano Frito','Plátano Frito',4,'COCINA',15,FALSE,47),
('COMBO_FAMILIAR','POLLERIA','Combos familiares','Combo Familiar Pollo Entero','Combo Familiar Pollo Entero',82,'PARRILLA',15,FALSE,48),
('COMBO_MEDIO','POLLERIA','Combos familiares','Combo Medio Pollo','Combo Medio Pollo',45,'PARRILLA',15,FALSE,49),
('COMBO_PAREJA','POLLERIA','Combos familiares','Combo Pareja 1/2 Pollo','Combo Pareja 1/2 Pollo',43,'PARRILLA',15,FALSE,50),
('HELADO_VASO','HELADERIA','Helados','Helado en Vaso','Helado en Vaso',7,'HELADERIA',5,FALSE,51),
('BOLA_HELADO','HELADERIA','Helados','Bola de Helado','Bola de Helado',4,'HELADERIA',5,FALSE,52),
('CONO_HELADO','HELADERIA','Conos y vasos','Cono de Helado','Cono de Helado',5,'HELADERIA',5,FALSE,53),
('BANANA_SPLIT','HELADERIA','Postres y waffles','Banana Split','Banana Split',14,'HELADERIA',5,TRUE,54),
('VASO_ENFRUTADO','HELADERIA','Frutas y vasos','Vaso Enfrutado','Vaso Enfrutado',12,'HELADERIA',5,FALSE,55),
('WAFFLE','HELADERIA','Postres y waffles','Waffle','Waffle',12,'HELADERIA',5,TRUE,56),
('WAFFLE_HELADO','HELADERIA','Postres y waffles','Waffle con Helado','Waffle con Helado',16,'HELADERIA',5,FALSE,57),
('SUNDAE','HELADERIA','Helados','Sundae','Sundae',9,'HELADERIA',5,FALSE,58),
('COPA_HELADO','HELADERIA','Helados','Copa de Helado','Copa de Helado',13,'HELADERIA',5,FALSE,59),
('BROWNIE_HELADO','HELADERIA','Postres y waffles','Brownie con Helado','Brownie con Helado',14,'HELADERIA',5,FALSE,60),
('ENSALADA_FRUTAS','HELADERIA','Frutas y vasos','Ensalada de Frutas','Ensalada de Frutas',10,'HELADERIA',5,FALSE,61),
('JUGO_NARANJA','JUGUERIA','Jugos','Jugo de Naranja','Jugo de Naranja',8,'JUGUERIA',4,FALSE,62),
('JUGO_FRESA','JUGUERIA','Jugos','Jugo de Fresa','Jugo de Fresa',8,'JUGUERIA',4,FALSE,63),
('JUGO_PAPAYA','JUGUERIA','Jugos','Jugo de Papaya','Jugo de Papaya',8,'JUGUERIA',4,FALSE,64),
('JUGO_PINA','JUGUERIA','Jugos','Jugo de Piña','Jugo de Piña',8,'JUGUERIA',4,FALSE,65),
('JUGO_SURTIDO','JUGUERIA','Jugos','Jugo Surtido','Jugo Surtido',10,'JUGUERIA',4,TRUE,66),
('BATIDO_FRESA','JUGUERIA','Batidos','Batido de Fresa','Batido de Fresa',10,'JUGUERIA',4,FALSE,67),
('BATIDO_PLATANO','JUGUERIA','Batidos','Batido de Plátano','Batido de Plátano',10,'JUGUERIA',4,FALSE,68),
('MILKSHAKE_CHOC','JUGUERIA','Batidos','Milkshake de Chocolate','Milkshake de Chocolate',12,'JUGUERIA',4,TRUE,69),
('MILKSHAKE_FRESA','JUGUERIA','Batidos','Milkshake de Fresa','Milkshake de Fresa',12,'JUGUERIA',4,FALSE,70),
('FRAPPE_CAFE','JUGUERIA','Frappés y frozen','Frappé de Café','Frappé de Café',13,'JUGUERIA',4,FALSE,71),
('FROZEN_LIMON','JUGUERIA','Frappés y frozen','Frozen de Limón','Frozen de Limón',9,'JUGUERIA',4,FALSE,72),
('CAPPUCCINO_FRIO','JUGUERIA','Cafetería fría','Capuccino Frío','Capuccino Frío',10,'JUGUERIA',4,FALSE,73),
('YOGURT_FRUTA','JUGUERIA','Yogures','Yogurt con Fruta','Yogurt con Fruta',10,'JUGUERIA',4,FALSE,74),
('YOGURT_BEBIBLE','JUGUERIA','Yogures','Yogurt de Fruta Bebible','Yogurt de Fruta Bebible',8,'JUGUERIA',4,FALSE,75),
('EXTRACTO_VERDE','JUGUERIA','Extractos','Extracto Verde','Extracto Verde',10,'JUGUERIA',4,FALSE,76),
('LIMONADA','JUGUERIA','Jugos','Limonada','Limonada',7,'JUGUERIA',4,FALSE,77),
('AGUA','JUGUERIA','Jugos','Agua sin Gas','Agua sin Gas',3,'JUGUERIA',4,FALSE,78),
('GASEOSA','JUGUERIA','Jugos','Gaseosa Personal','Gaseosa Personal',4,'JUGUERIA',4,FALSE,79),
('CAFE_AMERICANO','JUGUERIA','Cafetería fría','Café Americano','Café Americano',6,'JUGUERIA',4,FALSE,80)
) AS v(codigo,area,categoria,nombre,descripcion,precio,estacion,tiempo,destacado,orden)
JOIN esquema_menu.areas_negocio a ON a.codigo=v.area
JOIN esquema_menu.categorias_productos c ON c.id_area_negocio=a.id_area_negocio AND LOWER(c.nombre)=LOWER(v.categoria)
JOIN esquema_menu.estaciones_cocina e ON e.nombre=v.estacion
WHERE NOT EXISTS (
    SELECT 1
    FROM esquema_menu.productos existente
    JOIN esquema_menu.categorias_productos categoria_existente
      ON categoria_existente.id_categoria = existente.id_categoria
    WHERE categoria_existente.id_area_negocio = a.id_area_negocio
      AND LOWER(existente.nombre) = LOWER(v.nombre)
)
ON CONFLICT (codigo_producto) DO NOTHING;
