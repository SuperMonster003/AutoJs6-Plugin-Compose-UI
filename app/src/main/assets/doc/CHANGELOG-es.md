******

### Historial de versiones

******

# v1.1.0

###### 2026/10/08

* `Aviso` Vista previa local de desarrollo 1.1.0: requiere una compilación AutoJs6 correspondiente y el complemento instalado y activado. Se proporcionan páginas UI, ventanas flotantes, cinco ejemplos, referencia de API y declaraciones TypeScript para esta integración local. La hoja de ruta registra el alcance verificado de compatibilidad y rendimiento. El complemento no figura en el índice oficial ni tiene una publicación oficial. El icono sigue siendo provisional hasta recibir las imágenes definitivas del mantenedor
* `Aviso` Esta versión usa el contrato Compose UI V2 y requiere AutoJs6 6.8.0 / 5322; TSX requiere TypeScript Engine 0.6.7. Los anfitriones nuevos admiten los componentes existentes de renderizadores V1; los componentes ampliados requieren V2
* `Novedad` TSX admite `<compose.Column>`, `<compose:Text>`, referencias a fábricas de nodos, fragmentos, slots y callbacks reactivos; un mismo árbol no puede mezclar Compose y nodos XML existentes
* `Novedad` Los contenedores XML `<compose>` y compose.attach integran sesiones Compose independientes en páginas UI o ventanas flotantes existentes; compose.AndroidView muestra una View Android existente o devuelta por una fábrica síncrona
* `Novedad` compose.dialog devuelve una sesión que se puede actualizar y cerrar para diálogos y paneles inferiores modales en scripts UI o normales
* `Novedad` Componentes Material 3 ampliados: navegación y paneles laterales, pestañas, paneles inferiores y menús, selectores de fecha y hora, paginación y cuadrículas, chips, insignias, botones segmentados y flotantes, búsqueda, ayudas y actualización al deslizar
* `Mejora` Los iconos de información de la aplicación de Android comparten las imágenes y los fondos claros y oscuros de Icon Studio, conservando las imágenes transparentes del centro de plugins y las opciones del lanzador
* `Mejora` Las fábricas de View se ejecutan en el hilo principal antes del renderizado. Un reemplazo inválido conserva el contenido actual; una View no puede pertenecer a dos nodos ni tomarse de otro padre. Conserva sus listeners y los recursos siguen bajo control del llamador
* `Mejora` cancelable=false desactiva el cierre al volver, pulsar fuera o deslizar; el cierre explícito y la salida del script liberan el diálogo conservando las páginas y otras sesiones
* `Dependencia` Actualizar compose-ui-api.aar con la extensión opcional AndroidView conservando V1
* `Dependencia` Añadir la capacidad opcional de diálogo a compose-ui-api.aar conservando los contratos V1 y AndroidView existentes
* `Dependencia` Añadir el catálogo V2 de compose-ui-api.aar conservando los modelos de nodos y la semántica de componentes V1

# v1.0.0

###### 2026/10/03

* `Aviso` Vista previa local de desarrollo 1.0.0: requiere una compilación AutoJs6 correspondiente y el complemento instalado y activado. Se proporcionan páginas UI, ventanas flotantes, cinco ejemplos, referencia de API y declaraciones TypeScript para esta integración local. La hoja de ruta registra el alcance verificado de compatibilidad y rendimiento. El complemento no figura en el índice oficial ni tiene una publicación oficial. El icono sigue siendo provisional hasta recibir las imágenes definitivas del mantenedor
* `Aviso` Requiere AutoJs6 6.8.0 (5316) o posterior
* `Aviso` Las aplicaciones empaquetadas también requieren instalar por separado un complemento Compose UI compatible, con activación/autorización propias de la aplicación; se comprueba el runtime AutoJs6 integrado, no el versionCode de la aplicación
* `Aviso` Se mantienen los dos mipmap claro/oscuro; el dibujo actual es provisional hasta que el mantenedor proporcione las imágenes definitivas en blanco y negro
* `Novedad` compose / $compose invocables, manejadores de nodos persistentes, state/render/ref reactivos, cambios agrupados, tareas encoladas y control del tema
* `Novedad` Núcleo Material 3: 29 fábricas de nodos para diseños, texto, iconos, imágenes, botones, entradas, selección, listas diferidas, diálogos e indicadores; Snackbar es un comando de sesión, no una fábrica compose.Snackbar
* `Novedad` Los scripts UI montan contenido Activity; compose.floaty también ofrece a scripts sin UI ventanas raw o redimensionables, geometría en píxeles, controles táctiles/de foco y limpieza de sus recursos
* `Novedad` Las 20 operaciones Modifier conservan el orden declarado y admiten validación del ámbito de diseño, desplazamiento y etiquetas de accesibilidad
* `Novedad` Los temas Material 3 admiten colores semilla, modos claro y oscuro, colores dinámicos del sistema en Android 12+, familias tipográficas y escala de texto
* `Novedad` La edición nativa conserva la selección y la composición IME, admite foco y cambios explícitos, y rechaza cambios tardíos que sobrescribirían entradas más recientes; interruptores y deslizadores siguen controlados por el script
* `Novedad` Iconos core e imágenes ImageWrapper/Bitmap, archivos locales y drawable del host; el renderizador no recicla automáticamente las imágenes propiedad del llamante
* `Novedad` Cinco ejemplos ejecutables de contador, validación de formularios, lista de 1000 elementos con claves estables, HUD flotante sin modo UI y temas, con requisitos e índice, sincronizados con la categoría Compose UI del host correspondiente
* `Novedad` Referencia API y declaraciones TypeScript asociadas, además de README, instrucciones del centro de complementos y registro de cambios en 10 idiomas
* `Novedad` Descubrimiento en el centro de complementos con comprobaciones de versión del host, contrato y autorización, sin pantalla independiente ni entrada de lanzador
* `Correccion` Compose UI rechaza volver a montar páginas o ventanas flotantes dentro del callback de renderizado y conserva la página actual; permite volver a montar páginas después de actualizar el plugin
* `Mejora` Las comprobaciones y ComposeError informan de plugins ausentes, desactivados, no autorizados o incompatibles, permisos insuficientes y sesiones cerradas; la limpieza también cubre ventanas canceladas antes de su conexión nativa; Actualizar, desinstalar o desactivar el plugin cierra sus sesiones activas y comunica el error correspondiente
* `Dependencia` Se agrega common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, bloqueado por hash)
* `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependencia` Añadir compose-ui-api.aar V1 alineado con AutoJs6 6.8.0 (5316) (MPL 2.0, hash fijado), con las dependencias compartidas alineadas con el host
* `Dependencia` Añadir Compose UI Test gestionado por BOM 2026.09.00 (Apache 2.0, solo para pruebas)
* `Dependencia` Añadir JaCoCo versión 0.8.14 (solo cobertura opcional de pruebas, excluido de los paquetes de publicación)
