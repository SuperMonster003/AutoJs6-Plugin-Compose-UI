Compose UI es un plugin de renderizado de interfaces para AutoJs6. Los scripts declaran su interfaz mediante el objeto global `compose` integrado en el host, y el plugin la renderiza dentro del proceso del host con Jetpack Compose y Material 3, ofreciendo una unica solucion declarativa para el contenido de las actividades en modo `"ui";` y para las ventanas flotantes.

La version actual es una vista previa de desarrollo P0. El repositorio contiene un esqueleto de plugin compilable, el servicio INFO y el protocolo de activacion Wake Activity, pero el renderizador y la API de script aun no se han entregado, por lo que tras la instalacion los scripts todavia no pueden renderizar nada mediante `compose`. Consulte la hoja de ruta para conocer los proximos hitos y el progreso.

### Uso

1. Instale AutoJs6 6.8.0 (5308) o una version posterior
2. Instale el APK de este plugin (no hay nada que abrir, el plugin no tiene entrada en el lanzador)
3. Confirme en el centro de plugins de AutoJs6 que Compose UI se reconoce y esta habilitado
4. Use directamente el objeto global `compose` en los scripts (el renderizado llega con la version 1.0.0)

### Compatibilidad

- Version de AutoJs6: 6.8.0 (5308) o posterior; los hosts anteriores marcan el plugin como incompatible en el centro de plugins
- Version de Android: 7.0 (API 24) o posterior
- Arquitectura del procesador: cualquiera (APK de bytecode puro, sin bibliotecas nativas)
- Version de Compose: incluida en el plugin (BOM 2026.09.00), independiente del runtime de Compose del host
- Version del contrato: 1; el host y el plugin negocian la version del contrato y rechazan la carga con un error claro cuando no coincide

### Preguntas frecuentes

- Por que no aparece el icono del plugin tras instalarlo? El plugin no tiene interfaz propia ni entrada en el lanzador; busquelo en el centro de plugins de AutoJs6
- Por que `compose` todavia no funciona en los scripts? Esta es una vista previa de desarrollo P0; el renderizador y la API de script llegaran en hitos posteriores
- Hay que desinstalar otros plugins de interfaz? No, Compose UI no interfiere con el modulo `ui` existente ni con otros plugins
- Hay que modificar los scripts tras actualizar el plugin? No mientras la version del contrato se mantenga; las actualizaciones del contrato se indican explicitamente en el registro de cambios

### Permisos y seguridad

- Proteccion de componentes: tanto la Wake Activity como el servicio INFO estan protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo el host AutoJs6 puede acceder a ellos
- Sin actividad en segundo plano: el plugin no tiene servicios residentes, receptores de difusion ni tareas programadas, y no consume recursos mientras el host no lo carga
- Limite de datos: el plugin nunca lee ni escribe datos de scripts ni archivos del usuario; el estado de la interfaz solo existe en la memoria del proceso del host
- Politica de copias de seguridad: la copia de seguridad de la aplicacion y la transferencia entre dispositivos estan deshabilitadas, y el plugin no guarda datos que migrar

Mas informacion (inicio rapido, notas de compilacion, hoja de ruta) en la pagina del proyecto: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
