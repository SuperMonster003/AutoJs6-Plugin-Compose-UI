Compose UI es un plugin de renderizado de interfaces para AutoJs6. Los scripts declaran interfaces mediante la entrada `compose` / `$compose` del host, y el plugin las renderiza dentro del proceso del host con Jetpack Compose y Material 3. La vista previa actual admite contenido de actividades `"ui";`; las ventanas flotantes están previstas para P3.4.

Vista previa de desarrollo P3: la entrada invocable compose / $compose, 29 fábricas de nodos, manejadores persistentes, state/render/ref reactivos, batch/post/theme y el montaje en scripts UI funcionan con una compilación local compatible del host AutoJs6. Las ventanas flotantes siguen previstas para P3.4. Los ejemplos incluidos, la documentación completa de la API, las declaraciones de tipos y la matriz ampliada de verificación siguen pendientes. Es una vista previa local sin publicación oficial.

### Uso

1. Instale una compilación local compatible de AutoJs6 que incluya la entrada compose (mínimo 6.8.0 / 5316)
2. Instale el APK de este plugin (no hay nada que abrir, el plugin no tiene entrada en el lanzador)
3. Confirme en el centro de plugins de AutoJs6 que Compose UI se reconoce y esta habilitado
4. Use `compose` o `$compose` en los scripts; monte el contenido de la actividad desde un script `"ui";`

### Compatibilidad

- Version minima de AutoJs6: 6.8.0 (5316) o posterior; los hosts anteriores marcan el plugin como incompatible en el centro de plugins
- Version de Android: 7.0 (API 24) o posterior
- Arquitectura del procesador: arm64-v8a / armeabi-v7a / x86_64 / x86 (las cuatro integradas en el unico APK, sin elegir por arquitectura)
- Version de Compose: incluida en el plugin (BOM 2026.09.00), independiente del runtime de Compose del host
- Version del contrato: 1; el host y el plugin negocian la version del contrato y rechazan la carga con un error claro cuando no coincide

### Preguntas frecuentes

- Por que no aparece el icono del plugin tras instalarlo? El plugin no tiene interfaz propia ni entrada en el lanzador; busquelo en el centro de plugins de AutoJs6
- Por qué falta `compose`? El objeto global lo proporciona la compilación local compatible del host; instalar solo el APK del plugin no lo añade
- Hay que desinstalar otros plugins de interfaz? No, Compose UI no interfiere con el modulo `ui` existente ni con otros plugins
- Hay que modificar los scripts tras actualizar el plugin? No mientras la version del contrato se mantenga; las actualizaciones del contrato se indican explicitamente en el registro de cambios

### Permisos y seguridad

- Proteccion de componentes: tanto la Wake Activity como el servicio INFO estan protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo el host AutoJs6 puede acceder a ellos
- Sin actividad en segundo plano: el plugin no tiene servicios residentes, receptores de difusion ni tareas programadas, y no consume recursos mientras el host no lo carga
- Limite de datos: el plugin nunca lee ni escribe datos de scripts ni archivos del usuario; el estado de la interfaz solo existe en la memoria del proceso del host
- Politica de copias de seguridad: la copia de seguridad de la aplicacion y la transferencia entre dispositivos estan deshabilitadas, y el plugin no guarda datos que migrar

Mas informacion (inicio rapido, notas de compilacion, hoja de ruta) en la pagina del proyecto: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
