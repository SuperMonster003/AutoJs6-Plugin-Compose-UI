3-Block Compose es un plugin de renderizado de interfaces para AutoJs6. Los scripts declaran interfaces mediante `compose` / `$compose` del host y el plugin las renderiza dentro del proceso del host con Jetpack Compose y Material 3. La vista previa admite contenido de actividades `"ui";` y ventanas flotantes desde scripts no UI.

1.4.0 es la primera versión pública (GitHub Release, incluida en el índice oficial de complementos). Requiere una compilación de AutoJs6 6.8.0 (5322) o posterior con la nueva identidad de 3-Block Compose (commit del host f12665ffde o posterior); AutoJs6 6.8.0 aún no se había publicado oficialmente cuando salió esta versión, por lo que se necesita una compilación de AutoJs6 que contenga ese commit. La entrada de menú del editor de AutoJs6 "Abrir en el diseñador de 3-Block Compose" necesita una compilación posterior al commit del host db7e6a1734, y los proyectos TSX del diseñador necesitan TypeScript Engine 0.7.0. El complemento ofrece páginas de UI, ventanas flotantes, el diseñador visual, cinco ejemplos, la referencia de API, la página de guía del diseñador y las declaraciones de TypeScript. El alcance verificado de compatibilidad y rendimiento se registra en la hoja de ruta. Los iconos usan el arte final del mantenedor.

### Uso

1. Instale una compilación local compatible de AutoJs6 que incluya la entrada compose (mínimo 6.8.0 / 5322)
2. Instale el APK del complemento; abra 3-Block Compose desde el lanzador para llegar al inicio del diseñador, crear un proyecto de script o abrir la galería de componentes y los ajustes desde la barra superior
3. Confirme en el centro de plugins de AutoJs6 que 3-Block Compose se reconoce y esta habilitado
4. Use `compose` o `$compose` en scripts; monte actividades con `compose.mount`, o conceda al host permiso de superposición y use `compose.floaty`
5. El menú del editor ofrece ejecutar, abrir en el editor de AutoJs6, importar a los scripts de AutoJs6, copiar y exportar; elegir 3-Block Compose desde el menú del editor de AutoJs6 (requiere una compilación de AutoJs6 con la actualización correspondiente) o desde un gestor de archivos edita el archivo de script directamente

### Compatibilidad

- Version minima de AutoJs6: 6.8.0 (5322) o posterior; los hosts anteriores marcan el plugin como incompatible en el centro de plugins
- Version de Android: 7.0 (API 24) o posterior
- Arquitectura del procesador: arm64-v8a / armeabi-v7a / x86_64 / x86 (las cuatro integradas en el unico APK, sin elegir por arquitectura)
- Version de Compose: incluida en el plugin (BOM 2026.09.00), independiente del runtime de Compose del host
- Version del contrato: 2; el host y el plugin negocian la version del contrato y rechazan la carga con un error claro cuando no coincide
- Las aplicaciones empaquetadas también requieren instalar por separado un complemento 3-Block Compose compatible, con activación/autorización propias de la aplicación; se comprueba el runtime AutoJs6 integrado, no el versionCode de la aplicación
- Las fábricas de View se ejecutan en el hilo principal antes del renderizado. Un reemplazo inválido conserva el contenido actual; una View no puede pertenecer a dos nodos ni tomarse de otro padre. Conserva sus listeners y los recursos siguen bajo control del llamador
- cancelable=false desactiva el cierre al volver, pulsar fuera o deslizar; el cierre explícito y la salida del script liberan el diálogo conservando las páginas y otras sesiones
- Esta versión usa el contrato 3-Block Compose V2 y requiere AutoJs6 6.8.0 / 5322; TSX requiere TypeScript Engine 0.6.7. Los anfitriones nuevos admiten los componentes existentes de renderizadores V1; los componentes ampliados requieren V2
- compose.memo lo proporciona AutoJs6 6.8.0 / 5323 para reutilizar fragmentos de render cuyas dependencias no cambian; este complemento no necesita actualizarse y TSX requiere TypeScript Engine 0.6.8
- Desde 1.2.0 las clases del contrato (org.autojs.plugin.compose.api) se empaquetan con el complemento para su proceso independiente; dentro de AutoJs6 el host proporciona las mismas clases y la copia del complemento queda oculta
- El diseñador se ejecuta en el proceso propio del complemento y no depende de AutoJs6; ejecutar, abrir en el editor de AutoJs6 e importar al directorio de scripts de AutoJs6 requieren AutoJs6 instalado, y la entrada de menú del editor "Abrir en el diseñador de 3-Block Compose" requiere una compilación de AutoJs6 que la incluya
- Los proyectos TSX del diseñador necesitan TypeScript Engine 0.7.0 o posterior (que ofrece el servicio de análisis de árbol sintáctico), y al ejecutarlos AutoJs6 los compila con TypeScript Engine; sin él, o con una versión anterior, un proyecto TSX se abre como código fuente de solo lectura que se puede ver y copiar

### Preguntas frecuentes

- Como se ejecutan los ejemplos de la galería? "Ejecutar en AutoJs6" entrega el script al AutoJs6 instalado; la galería solo muestra vistas previas y código y nunca ejecuta scripts
- Puede el diseñador abrir cualquier script? Solo interpreta las declaraciones `compose.state` de nivel superior y la única llamada `compose.mount` / `floaty` / `dialog`; el resto de sentencias se conserva tal cual. Las formas que no puede asignar (una raíz que no es una fábrica compose, varias llamadas de montaje, una función de renderizado sin return) se rechazan con línea y columna. En los proyectos TSX, las etiquetas `compose.X` / `compose:X` son nodos editables, las demás etiquetas y las expresiones `{}` se conservan tal cual, y las etiquetas XML y los atributos de propagación se rechazan
- El diseñador ejecuta scripts? No. Solo los analiza (JavaScript con el analizador Rhino, TSX con el servicio de análisis de TypeScript Engine) y los previsualiza con el renderizador propio del complemento; los controladores de eventos no se ejecutan y el state muestra su valor inicial o un valor de vista previa fijado en el panel Estado. En la vista previa interactiva, los controles vinculados a un state (interruptores, controles deslizantes, campos de texto, etc.) cambian directamente su valor de vista previa, pero los controladores y las funciones de renderizado siguen sin ejecutarse. La ejecución corresponde a AutoJs6
- Por qué falta `compose`? El objeto global lo proporciona la compilación local compatible del host; instalar solo el APK del plugin no lo añade
- Hay que desinstalar otros plugins de interfaz? No, 3-Block Compose no interfiere con el modulo `ui` existente ni con otros plugins
- Qué ocurre si cambia el complemento? Actualizarlo, desinstalarlo o desactivarlo cierra las sesiones activas e informa del error correspondiente; un complemento compatible y activado permite volver a montar
- Qué requieren las ventanas flotantes? Conceda permiso de superposición al host y llame a `window.requestFocus()` antes de escribir. Si HyperOS no muestra la ventana, vuelva al escritorio. La falta de permiso devuelve PERMISSION_REQUIRED sin abrir automáticamente una solicitud de autorización
- Se puede usar TSX o cualquier función Compose? TSX admite las fábricas Compose documentadas con el host y TypeScript Engine correspondientes. No admite funciones Kotlin Composable arbitrarias ni componentes TSX personalizados
- Se pierde el estado al girar? El host actual gestiona los cambios normales de orientación sin reemplazar el motor de scripts. La recreación o destrucción real de la Activity cierra el motor y sus sesiones; el estado de negocio no se restaura automáticamente
- Cómo encuentran los selectores los componentes? testTag se expone como ID original sin prefijo de paquete. id/testTag y desc/contentDescription son distintos; el texto de Button puede ser un hijo, por lo que debe seguir parent() hasta un ancestro pulsable cuando haga falta

### Permisos y seguridad

- Protección de componentes: la Wake Activity y el servicio INFO están protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo el host AutoJs6 puede acceder a ellos; el editor del diseñador es un componente exportado sin permiso que solo acepta el Uri de un documento de script entregado por el usuario, y la lectura y escritura se limitan a ese Uri
- Comportamiento en segundo plano: el complemento no tiene servicios residentes ni tareas programadas y solo recibe una difusión del sistema al actualizarse, para normalizar sus componentes de icono del lanzador; no consume nada mientras el host no lo carga y ni el diseñador ni la galería están abiertos
- Límite de datos: el renderizador nunca lee ni escribe datos de scripts ni archivos del usuario, y el estado de la interfaz solo existe en la memoria del proceso del host; los proyectos y borradores del diseñador viven en el directorio privado del complemento, se importan y exportan solo mediante el selector de documentos del sistema, o leen y escriben un único archivo de script mediante un Uri entregado por el usuario; el directorio de proyectos se comparte solo a través del proveedor de contenido propio del complemento, por solicitud, al entregar un proyecto a AutoJs6; una importación conserva el acceso al archivo elegido cuando su proveedor lo permite, pero ese archivo solo se lee o escribe cuando el usuario elige "Escribir de vuelta en el origen" o "Volver a importar desde el origen"
- Politica de copias de seguridad: la copia de seguridad de la aplicacion y la transferencia entre dispositivos estan deshabilitadas, y el plugin no guarda datos que migrar

Mas informacion (inicio rapido, notas de compilacion, hoja de ruta) en la pagina del proyecto: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose
