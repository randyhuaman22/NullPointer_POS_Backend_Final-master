# Cambios sobre el backend entregado por el usuario

Esta revisión se construyó sobre `NullPointer_POS_Backend_Inicio_1Click_Postman_4Niveles(1).zip`.
Se conservaron los microservicios existentes, especialmente Caja y sus rutas actuales; no se sustituyó Caja por código de una revisión anterior.

## Usuarios del administrador
- `GET /api/v1/administracion/usuarios`: listar.
- `GET /api/v1/administracion/usuarios/{id}`: consultar por ID.
- `POST /api/v1/administracion/usuarios`: crear.
- `PUT /api/v1/administracion/usuarios/{id}`: editar.
- `DELETE /api/v1/administracion/usuarios/{id}`: baja lógica, conserva el historial.
- `PATCH /api/v1/administracion/usuarios/{id}/desactivar`: desactivar.
- `PATCH /api/v1/administracion/usuarios/{id}/activar`: reactivar.
- `GET /api/v1/administracion/usuarios/auditoria`: auditoría.

## Categorías y productos
Categorías y productos tienen consulta por ID, listado, creación, edición, baja lógica con `DELETE` y restauración con `PATCH .../restaurar`. Las categorías no se pueden desactivar mientras tengan productos o subcategorías activos; primero hay que desactivar esos elementos.

## Unión de mesas
- `POST /api/v1/mesas/grupos`: une dos o más mesas libres y requiere seleccionar una mesa principal.
- `GET /api/v1/mesas/grupos/{idGrupo}`: consulta el grupo y sus mesas.
- `PATCH /api/v1/mesas/grupos/{idGrupo}/finalizar`: cancela la unión si no hay pedidos activos.
La primera comanda del grupo se abre en la mesa principal. Al pagar o anular la última comanda activa del grupo, las mesas se liberan y el grupo queda finalizado.

## Catálogo de demostración
La migración `V6__catalogo_inicial_wendys_food.sql` agrega 80 productos de demostración en Pollería, Menú, Heladería y Juguería. Incluye los productos mencionados por el usuario y opciones habituales como 1/4, 1/2 y pollo entero a la brasa, salchipapa, lomo saltado, ají de gallina, combos, postres, jugos y bebidas.
Los precios son referenciales y deben ajustarse a la carta real. Los productos se insertan por código para que la migración no duplique los registros al ejecutarse en una base ya existente.

## Docker
- Se retiró el servicio de una sola ejecución `inicializador-bases-datos`; las bases se crean en la inicialización de PostgreSQL mediante `base-de-datos/crear-bases-datos.sh`.
- Se eliminaron los prefijos `nullpointer-` de los nombres visibles de contenedores y se asignaron nombres de imagen sencillos (`servicio-caja:local`, etc.).
- Se conservan los nombres de volumen que aparecen en la instalación anterior: `nullpointer_pos_backend_inicio_1click_postman_4niveles_nullpointer_pgdata` y `nullpointer_pos_backend_inicio_1click_postman_4niveles_nullpointer_rabbitdata`. Así, al reemplazar el backend, Compose no crea otro almacenamiento solo por cambiar el nombre de carpeta.

## Postman
La colección única `postman/NullPointer_POS.postman_collection.json` está organizada en cuatro niveles:
1. NullPointer POS (colección)
2. Rol/área
3. Módulo
4. Endpoint HTTP

Se retiró el archivo de colección separada `NullPointer_POS_Sprint1.postman_collection.json` para evitar que se importe por error. Se añadieron operaciones de usuarios, CRUD de categorías y productos, y unión de mesas.

## Verificación
Se validaron estáticamente la estructura del ZIP, el YAML de Compose, el JSON de Postman, la jerarquía de carpetas y los paquetes/rutas Java. No se pudo ejecutar `mvn clean verify` ni iniciar Docker en el entorno de preparación, porque no hay Maven ni Docker instalados. La compilación integrada de esta revisión debe comprobarse en el equipo del usuario.
