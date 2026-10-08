Compose UI es un plugin de renderizado de interfaces para AutoJs6. Los scripts declaran interfaces mediante `compose` / `$compose` del host y el plugin las renderiza dentro del proceso del host con Jetpack Compose y Material 3. La vista previa admite contenido de actividades `"ui";` y ventanas flotantes desde scripts no UI.

Vista previa local de desarrollo 1.1.0: requiere una compilación AutoJs6 correspondiente y el complemento instalado y activado. Se proporcionan páginas UI, ventanas flotantes, cinco ejemplos, referencia de API y declaraciones TypeScript para esta integración local. La hoja de ruta registra el alcance verificado de compatibilidad y rendimiento. El complemento no figura en el índice oficial ni tiene una publicación oficial. El icono sigue siendo provisional hasta recibir las imágenes definitivas del mantenedor.

### Uso

1. Instale una compilación local compatible de AutoJs6 que incluya la entrada compose (mínimo 6.8.0 / 5322)
2. Instale el APK de este plugin (no hay nada que abrir, el plugin no tiene entrada en el lanzador)
3. Confirme en el centro de plugins de AutoJs6 que Compose UI se reconoce y esta habilitado
4. Use `compose` o `$compose` en scripts; monte actividades con `compose.mount`, o conceda al host permiso de superposición y use `compose.floaty`

### Compatibilidad

- Version minima de AutoJs6: 6.8.0 (5322) o posterior; los hosts anteriores marcan el plugin como incompatible en el centro de plugins
- Version de Android: 7.0 (API 24) o posterior
- Arquitectura del procesador: arm64-v8a / armeabi-v7a / x86_64 / x86 (las cuatro integradas en el unico APK, sin elegir por arquitectura)
- Version de Compose: incluida en el plugin (BOM 2026.09.00), independiente del runtime de Compose del host
- Version del contrato: 2; el host y el plugin negocian la version del contrato y rechazan la carga con un error claro cuando no coincide
- Las aplicaciones empaquetadas también requieren instalar por separado un complemento Compose UI compatible, con activación/autorización propias de la aplicación; se comprueba el runtime AutoJs6 integrado, no el versionCode de la aplicación
- Las fábricas de View se ejecutan en el hilo principal antes del renderizado. Un reemplazo inválido conserva el contenido actual; una View no puede pertenecer a dos nodos ni tomarse de otro padre. Conserva sus listeners y los recursos siguen bajo control del llamador
- cancelable=false desactiva el cierre al volver, pulsar fuera o deslizar; el cierre explícito y la salida del script liberan el diálogo conservando las páginas y otras sesiones
- Esta versión usa el contrato Compose UI V2 y requiere AutoJs6 6.8.0 / 5322; TSX requiere TypeScript Engine 0.6.7. Los anfitriones nuevos admiten los componentes existentes de renderizadores V1; los componentes ampliados requieren V2
- compose.memo lo proporciona AutoJs6 6.8.0 / 5323 para reutilizar fragmentos de render cuyas dependencias no cambian; este complemento no necesita actualizarse y TSX requiere TypeScript Engine 0.6.8

### Preguntas frecuentes

- Por que no aparece el icono del plugin tras instalarlo? El plugin no tiene interfaz propia ni entrada en el lanzador; busquelo en el centro de plugins de AutoJs6
- Por qué falta `compose`? El objeto global lo proporciona la compilación local compatible del host; instalar solo el APK del plugin no lo añade
- Hay que desinstalar otros plugins de interfaz? No, Compose UI no interfiere con el modulo `ui` existente ni con otros plugins
- Qué ocurre si cambia el complemento? Actualizarlo, desinstalarlo o desactivarlo cierra las sesiones activas e informa del error correspondiente; un complemento compatible y activado permite volver a montar
- Qué requieren las ventanas flotantes? Conceda permiso de superposición al host y llame a `window.requestFocus()` antes de escribir. Si HyperOS no muestra la ventana, vuelva al escritorio. La falta de permiso devuelve PERMISSION_REQUIRED sin abrir automáticamente una solicitud de autorización
- Se puede usar TSX o cualquier función Compose? TSX admite las fábricas Compose documentadas con el host y TypeScript Engine correspondientes. No admite funciones Kotlin Composable arbitrarias ni componentes TSX personalizados
- Se pierde el estado al girar? El host actual gestiona los cambios normales de orientación sin reemplazar el motor de scripts. La recreación o destrucción real de la Activity cierra el motor y sus sesiones; el estado de negocio no se restaura automáticamente
- Cómo encuentran los selectores los componentes? testTag se expone como ID original sin prefijo de paquete. id/testTag y desc/contentDescription son distintos; el texto de Button puede ser un hijo, por lo que debe seguir parent() hasta un ancestro pulsable cuando haga falta

### Permisos y seguridad

- Proteccion de componentes: tanto la Wake Activity como el servicio INFO estan protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo el host AutoJs6 puede acceder a ellos
- Sin actividad en segundo plano: el plugin no tiene servicios residentes, receptores de difusion ni tareas programadas, y no consume recursos mientras el host no lo carga
- Limite de datos: el plugin nunca lee ni escribe datos de scripts ni archivos del usuario; el estado de la interfaz solo existe en la memoria del proceso del host
- Politica de copias de seguridad: la copia de seguridad de la aplicacion y la transferencia entre dispositivos estan deshabilitadas, y el plugin no guarda datos que migrar

Mas informacion (inicio rapido, notas de compilacion, hoja de ruta) en la pagina del proyecto: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
