Compose UI es un plugin de renderizado de interfaces para AutoJs6. Los scripts declaran su interfaz mediante el objeto global `compose` integrado en el host, y el plugin la renderiza dentro del proceso del host con Jetpack Compose y Material 3, ofreciendo una unica solucion declarativa para el contenido de las actividades en modo `"ui";` y para las ventanas flotantes.

Vista previa de desarrollo P2: las 30 entradas del catálogo V1 están implementadas en un host de prueba dedicado, con 29 componentes de nodo y el comando Snackbar. La vista previa incluye campos de texto nativos, listas de carga diferida, Scaffold, diálogos e indicadores de progreso. La API pública de scripts compose sigue prevista para P3.

### Uso

1. Instale AutoJs6 6.8.0 (5316) o una version posterior
2. Instale el APK de este plugin (no hay nada que abrir, el plugin no tiene entrada en el lanzador)
3. Confirme en el centro de plugins de AutoJs6 que Compose UI se reconoce y esta habilitado
4. Pruebe esta vista previa mediante el host de prueba dedicado; la entrada pública de scripts `compose` está prevista para P3

### Compatibilidad

- Version minima de AutoJs6: 6.8.0 (5316) o posterior; los hosts anteriores marcan el plugin como incompatible en el centro de plugins
- Version de Android: 7.0 (API 24) o posterior
- Arquitectura del procesador: arm64-v8a / armeabi-v7a / x86_64 / x86 (las cuatro integradas en el unico APK, sin elegir por arquitectura)
- Version de Compose: incluida en el plugin (BOM 2026.09.00), independiente del runtime de Compose del host
- Version del contrato: 1; el host y el plugin negocian la version del contrato y rechazan la carga con un error claro cuando no coincide

### Preguntas frecuentes

- Por que no aparece el icono del plugin tras instalarlo? El plugin no tiene interfaz propia ni entrada en el lanzador; busquelo en el centro de plugins de AutoJs6
- Por qué `compose` todavía no funciona en los scripts? Esta vista previa de desarrollo P2 se ejecuta mediante un host de prueba dedicado; la entrada de scripts compose está prevista para P3
- Hay que desinstalar otros plugins de interfaz? No, Compose UI no interfiere con el modulo `ui` existente ni con otros plugins
- Hay que modificar los scripts tras actualizar el plugin? No mientras la version del contrato se mantenga; las actualizaciones del contrato se indican explicitamente en el registro de cambios

### Permisos y seguridad

- Proteccion de componentes: tanto la Wake Activity como el servicio INFO estan protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo el host AutoJs6 puede acceder a ellos
- Sin actividad en segundo plano: el plugin no tiene servicios residentes, receptores de difusion ni tareas programadas, y no consume recursos mientras el host no lo carga
- Limite de datos: el plugin nunca lee ni escribe datos de scripts ni archivos del usuario; el estado de la interfaz solo existe en la memoria del proceso del host
- Politica de copias de seguridad: la copia de seguridad de la aplicacion y la transferencia entre dispositivos estan deshabilitadas, y el plugin no guarda datos que migrar

Mas informacion (inicio rapido, notas de compilacion, hoja de ruta) en la pagina del proyecto: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
