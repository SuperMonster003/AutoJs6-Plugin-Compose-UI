******

### Historial de versiones

******

# v1.7.0

###### 2026/10/15

* `Aviso` La vista previa interactiva de los proyectos UI (XML) ahora hace que AutoJs6 ejecute todo el script de verdad (requiere una compilación de AutoJs6 6.8.0 con el contrato de vista previa v2, commit del host 4c332c2053 o posterior, y Android 13 o posterior): los efectos secundarios del script son los mismos que al ejecutarlo en AutoJs6, y cada proyecto pregunta una vez antes de activar el interruptor; si no se cumplen las condiciones, la fila sobre el lienzo explica el motivo y queda "Ejecutar en AutoJs6"
* `Novedad` Vista previa interactiva de los proyectos UI (XML): con el interruptor "Interactuar" sobre el lienzo activado, AutoJs6 ejecuta todo el script como una ejecución en modo ui, el lienzo muestra el ui.layout del script y recibe los toques directamente, y los controladores de eventos, los enlaces {{ }}, los temporizadores y los demás módulos se ejecutan con la lógica real del script; el script vuelve a ejecutarse unos 0.4 s después de editar, se detiene al apagar el interruptor, al pasar el diseñador a segundo plano o al cerrar el editor, y el lienzo vuelve a la vista previa de diseño; el documento, el archivo del proyecto y el borrador nunca cambian. En Android 13 - 14 los campos de entrada del lienzo no reciben teclado, desde Android 15 sí
* `Novedad` Durante la vista previa interactiva, la pestaña de consola de un proyecto UI muestra la salida del script que ejecuta AutoJs6 (coloreada por nivel), el estado de la ejecución y su motivo de finalización; si el script falla, el error (con su línea) aparece sobre el lienzo y un toque abre la consola; la ejecución aparece con el nombre del archivo del proyecto en la lista de ejecución de AutoJs6 y puede detenerse desde allí
* `Correccion` El diseñador mostraba a veces "El servicio de vista previa de AutoJs6 no está disponible ahora (at most 2 sessions per client)" al abrir un proyecto UI: cuando el tamaño del lienzo cambiaba dos veces antes de asentarse el diseño, el complemento creaba dos veces la sesión de vista previa en AutoJs6 y alcanzaba el límite de sesiones por cliente; ahora se crea una sesión a la vez y el tamaño o la apariencia que cambió mientras tanto se aplica después
* `Dependencia` Actualiza el artefacto de contrato ui-preview-api.aar al contrato de vista previa ui v2 (añade, después de v1, las interfaces para ejecutar y detener un script y las devoluciones de llamada de consola / error / fin; las compilaciones de AutoJs6 con solo v1 siguen ofreciendo la vista previa de diseño)

# v1.6.0

###### 2026/10/15

* `Aviso` El diseñador incorpora proyectos UI (XML): los scripts clásicos en modo "ui" (diseños XML E4X) pueden abrirse y editarse, con el lienzo dibujado por AutoJs6. La vista previa de diseño necesita una compilación de AutoJs6 6.8.0 que contenga el servicio de vista previa (commit del host 306a8f7834 o posterior); desde Android 11 (API 30) el lienzo incrusta las vistas reales que dibuja AutoJs6, las versiones anteriores muestran una instantánea de mapa de bits; sin el servicio el proyecto sigue siendo editable y el lienzo indica el motivo
* `Novedad` El diseñador admite proyectos UI (XML): elige "UI (XML)" al crear un proyecto, o importa un script en modo "ui" o ábrelo desde el editor de AutoJs6 / un gestor de archivos y se lee como proyecto UI; la lista de inicio marca cada proyecto como JS / TSX / UI. Los diseños XML del script se conservan como un árbol sintáctico con posiciones: las partes no tocadas (espacios, comentarios, orden de atributos, comillas, {expresiones}) se preservan byte a byte y solo se reescriben las etiquetas de apertura editadas, el texto y los elementos recién insertados; los demás literales XML del script también pueden llevarse al lienzo
* `Novedad` El editor de proyectos UI: esquema (contraer, subir / bajar, eliminar, arrastrar con pulsación larga, selector cuando hay varios diseños), cajón de propiedades (etiqueta, id, contenido de texto, atributos ya definidos y atributos del catálogo agrupados por clase View, estados texto / enlace `{{ }}` / {expresión}, atributos sin valor, cualquier nombre de atributo), paleta de componentes (las 48 etiquetas de la documentación por categoría con atributos predeterminados), panel de enlaces (valores evaluados de los nombres `{{ }}` y sustituciones que solo afectan al lienzo), lista de problemas (comprobada con el catálogo de etiquetas y atributos del sitio de documentación) y consola (lo que AutoJs6 informó al dibujar el diseño); guardar, recuperación de borradores, sincronización con el origen, ejecutar, abrir en el editor de AutoJs6, importar al directorio de scripts de AutoJs6, copiar y exportar funcionan también con los proyectos UI
* `Novedad` El lienzo de un proyecto UI lo dibuja AutoJs6 (vista previa de diseño): el complemento entrega el fragmento XML y los valores de enlace al servicio de vista previa de AutoJs6, que crea vistas reales con su inflador de diseños del modo ui y las dibuja en el lienzo sin ejecutar el script del proyecto; la selección y el arrastre funcionan en el lienzo como siempre, y un fotograma fallido señala el elemento culpable; el tiempo de ejecución de vista previa aparece como "3-Block Compose preview" en la lista de ejecución de AutoJs6 y puede detenerse allí
* `Novedad` El cajón de propiedades de un proyecto UI añade eventos al elemento seleccionado: se anexa un esqueleto de controlador `ui.<id>.on('evento', ...)` después de la llamada a ui.layout (un elemento sin id recibe uno) y su cuerpo se escribe en el editor de scripts; los eventos que el script ya registra aparecen como "Gestionado en el script"
* `Dependencia` Añade el artefacto de contrato ui-preview-api.aar (contrato de vista previa de diseños ui de AutoJs6 v1), con el que el diseñador pide a AutoJs6 que dibuje el lienzo de los proyectos UI

# v1.5.1

###### 2026/10/15

* `Correccion` El complemento podía cerrarse de forma inesperada cuando aparecía un nuevo campo de texto en el lienzo del diseñador, por ejemplo al insertar un campo de texto desde la paleta de componentes, cuando deshacer / rehacer volvía a mostrar un campo de texto, o cuando un script de la vista previa interactiva mostraba uno (desde 1.4.0); ahora estos campos de texto aparecen con normalidad
* `Correccion` Completar las traducciones del encabezado de clave en el inspector del diseñador

# v1.5.0

###### 2026/10/14

* `Aviso` La vista previa interactiva del diseñador ahora ejecuta el script de un proyecto JavaScript (Android 8.0 o posterior). El script se ejecuta dentro del complemento en un entorno aislado restringido: solo puede usar compose, console, toast y temporizadores, no puede acceder a Java / Android ni a otros módulos de AutoJs6, y solo cambia el lienzo, nunca el proyecto. Los proyectos TSX y los dispositivos con Android anterior a 8.0 mantienen la vista previa interactiva anterior
* `Novedad` La vista previa interactiva ejecuta el script: con Interactuar activado, la función de renderizado y los controladores de eventos de un proyecto JavaScript se ejecutan de verdad, así que tocar botones, escribir y accionar interruptores actualiza el lienzo como decide el script; los valores de vista previa del panel Estado pasan a ser los valores iniciales de los state, y el script vuelve a empezar cuando cambia el proyecto o un valor de vista previa
* `Novedad` Nueva pestaña Consola: muestra lo que el script escribe con console / log, sus avisos y sus errores en la vista previa; un error del script aparece en la parte inferior del lienzo y abre la consola al tocarlo; una ejecución de más de unos 3 segundos (un bucle infinito, por ejemplo) detiene el script e indica el motivo, y el diseñador sigue respondiendo
* `Novedad` El interruptor Interactuar muestra un icono de ejecución mientras el script se ejecuta
* `Correccion` En el modo de edición, un campo de texto del lienzo podía tomar el foco por sí solo y abrir el teclado sobre los paneles (sobre todo en Android 7.x); ahora los componentes del lienzo no toman el foco en el modo de edición, y al salir de la vista previa interactiva se oculta el teclado

# v1.4.0

###### 2026/10/13

* `Aviso` Los proyectos TSX del diseñador necesitan TypeScript Engine 0.7.0 o posterior; sin él, o con una versión anterior, un proyecto TSX solo se puede ver y copiar como código fuente de solo lectura
* `Novedad` El diseñador admite proyectos TSX: un proyecto nuevo puede ser JavaScript o TSX, y los archivos .tsx se pueden importar o abrir directamente desde el editor de AutoJs6 y los gestores de archivos; el código TSX se analiza con TypeScript Engine y se edita en el lienzo, y se guarda como TSX con un formato uniforme (sangría de 4 espacios, atributos entre comillas simples, un elemento hijo por línea); el código que el diseñador no puede editar (como etiquetas alias o hijos generados por condiciones o bucles) se conserva tal cual, y el que no puede representar (como etiquetas XML y atributos de propagación) se rechaza indicando línea y columna
* `Novedad` "Ejecutar" en un proyecto TSX del diseñador entrega el archivo del proyecto a AutoJs6, que lo compila con TypeScript Engine y lo ejecuta; los cambios sin guardar se guardan antes
* `Novedad` Cuando TypeScript Engine no está instalado, es demasiado antiguo o su servicio de análisis no está disponible por el momento, el diseñador muestra el código del proyecto TSX en solo lectura con el motivo, y se puede copiar el código o reintentar; al volver al diseñador después de instalar o actualizar TypeScript Engine se reintenta automáticamente
* `Correccion` Guardar un script externo que no se podía sobrescribir cerraba el diseñador en lugar de guardarlo como proyecto cuando el nombre del archivo tenía espacios, paréntesis u otros caracteres no permitidos en un nombre de proyecto; ahora esos caracteres se sustituyen por "-", y los scripts importados se nombran igual
* `Mejora` Los controladores de eventos editados en un proyecto TSX los comprueba TypeScript Engine, por lo que se puede usar sintaxis de TypeScript como las anotaciones de tipo
* `Dependencia` Se añade el artefacto de contrato typescript-parser-api.aar (contrato de análisis de árbol sintáctico de TypeScript v1), que el diseñador usa para leer proyectos TSX

# v1.3.0

###### 2026/10/13

* `Novedad` Los proyectos del diseñador creados con "Importar script" pueden usar "Escribir de vuelta en el origen" y "Volver a importar desde el origen" en el menú del editor: la importación conserva el acceso al archivo de origen cuando su proveedor lo permite, cada acción pregunta antes si el otro lado cambió desde la última sincronización, y un origen que ya no se abre se puede elegir de nuevo para restablecer el vínculo; guardar un proyecto nunca escribe el origen por sí solo
* `Novedad` El lienzo del diseñador previsualiza las imágenes de Image: '@drawable/name' proviene del AutoJs6 instalado y las rutas relativas se buscan en la carpeta elegida con "Carpeta de imágenes" en el menú del editor (solo se conserva acceso de lectura); las URL y rutas absolutas siguen como marcadores, con el motivo en la lista de problemas
* `Novedad` El editor del diseñador tiene un nuevo panel Estado: enumera los compose.state de nivel superior del script con sus valores iniciales y permite establecer valores de vista previa que solo cambian el lienzo, nunca el script, y no añaden pasos de deshacer. Con Interactuar activado (arriba a la derecha del lienzo) se pueden manejar los componentes del lienzo, y los interruptores, controles deslizantes, campos de texto, selecciones, paginadores y otros controles vinculados a un estado cambian su valor de vista previa (los controladores de eventos del script no se ejecutan); al desactivarlo vuelven la selección y el arrastre
* `Correccion` Cuando se insertaba, eliminaba o movía a otro lugar un hijo situado al principio de un contenedor, los hijos posteriores se reconstruían por completo y perdían su propio estado (por ejemplo, la posición de desplazamiento de una LazyColumn); ahora conservan su estado y ya no se reconstruyen
* `Correccion` Tras cambiar el texto de un campo de texto (TextField / OutlinedTextField) en el panel de propiedades del diseñador, el campo del lienzo seguía mostrando el texto anterior
* `Mejora` Arrastrar un nodo con pulsación larga en el lienzo del diseñador muestra una línea de inserción según la disposición del contenedor de destino (vertical, horizontal o en cuadrícula) y suelta el nodo donde está el dedo, en lugar de añadirlo siempre al final del contenedor
* `Mejora` En el lienzo del diseñador, AndroidView se muestra ahora como un área sombreada que conserva sus modificadores de tamaño (como h y fillMaxWidth) y muestra el código de su expresión view
* `Mejora` La cadena de modificadores del panel de propiedades del diseñador se reordena arrastrando el asa a la izquierda de cada operación, con una línea de inserción hasta soltarla; los botones subir / bajar se mantienen
* `Mejora` Los componentes emergentes abiertos (open es true) ahora se dibujan con un aspecto cercano al real dentro de la pantalla lógica del lienzo del diseñador: AlertDialog y ModalBottomSheet cubren la página con un velo, DropdownMenu y Tooltip aparecen debajo o encima de su ancla, y los nodos que contienen se pueden tocar para editarlos; ya no se insertan en la página como tarjetas y no se abre ninguna ventana real
* `Mejora` Tras cada edición, el lienzo del diseñador solo recompone los nodos que cambiaron y registra las posiciones de los nodos con menos coste, por lo que un documento más grande (unos 250 nodos) se redibuja claramente más rápido tras deshacer / rehacer

# v1.2.0

###### 2026/10/13

* `Aviso` El complemento pasa de llamarse Compose UI a 3-Block Compose: el ID de aplicación cambia de io.github.supermonster003.autojs6.plugin.compose.ui a io.github.supermonster003.autojs6.plugin.three.block.compose. Android lo trata como una aplicación nueva, el paquete anterior puede seguir instalado, los ajustes y autorizaciones no se migran, y el nuevo paquete debe activarse y autorizarse de nuevo en el centro de complementos
* `Aviso` Requiere una compilación de AutoJs6 6.8.0 que contenga la nueva identidad del complemento (commit del host f12665ffde o posterior); los hosts anteriores buscan el complemento por el nombre de paquete antiguo y no reconocen esta versión
* `Aviso` El diseñador se documenta en la página "Diseñador de 3-Block Compose" del sitio de documentación; la entrada de menú del editor de AutoJs6 "Abrir en el diseñador de 3-Block Compose" necesita una compilación de AutoJs6 6.8.0 posterior al commit del host db7e6a1734, mientras que los demás destinos solo necesitan AutoJs6 instalado
* `Novedad` Diseñador visual: el lanzador abre el inicio del diseñador, donde se crean, importan o abren proyectos de script del almacenamiento privado; el editor ofrece un lienzo de vista previa con el renderizador real, un árbol de esquema, una paleta de componentes y un panel de propiedades, con selección por toque, arrastre con pulsación larga para cambiar de padre, reordenación en el esquema, deshacer / rehacer, guardado y recuperación automáticos de borradores, edición de las opciones de presentación, informe de validación según el catálogo del contrato, y copia del script o envío a AutoJs6; en horizontal y en tabletas, el lienzo y los paneles aparecen uno al lado del otro; la galería de componentes y los ajustes se abren ahora desde el inicio del diseñador
* `Novedad` Integración del diseñador con AutoJs6: el menú del editor abre el script actual en el editor de AutoJs6 (el diseñador se recarga cuando AutoJs6 guarda), lo importa al directorio de scripts de AutoJs6 o lo exporta a cualquier lugar; el diseñador también abre archivos .js directamente desde el editor de AutoJs6 o un gestor de archivos y guarda en el mismo archivo, recurriendo a un proyecto nuevo si no tiene permiso de escritura; los archivos de proyecto se comparten mediante el proveedor de contenido del propio complemento con permisos por solicitud, sin exponer ningún otro directorio
* `Mejora` El título de la aplicación, el ID del complemento (three-block-compose), los nombres de paquete y componentes, los artefactos de publicación, la documentación y el repositorio de GitHub (AutoJs6-Plugin-Three-Block-Compose) se actualizan con el cambio de nombre; la API de script compose, la versión del contrato y el comportamiento de renderizado no cambian
* `Mejora` Los iconos del lanzador, del centro de complementos y de la información de la aplicación del sistema usan ahora las imágenes definitivas del mantenedor, con los glifos claro y oscuro compartiendo una misma forma
* `Mejora` Los ejemplos incluidos y la categoría de ejemplos del host pasan a llamarse 3-Block Compose
* `Mejora` El AAR del contrato compose-ui-api ahora se empaqueta con el complemento y se conserva íntegro (antes solo en compilación) para el proceso propio del complemento; dentro de AutoJs6 las clases de contrato idénticas del host siguen teniendo prioridad, por lo que el comportamiento de renderizado y la versión del contrato no cambian
* `Dependencia` Se añade el jar reducido del analizador Rhino (org.mozilla.javascript 2.0.0-SNAPSHOT, MPL 2.0, la misma compilación que el host AutoJs6), usado solo por el diseñador visual para analizar scripts, nunca para ejecutarlos; las compilaciones release conservan solo las clases del analizador referenciadas

# v1.1.0

###### 2026/10/08

* `Aviso` Vista previa local de desarrollo 1.1.0: requiere una compilación AutoJs6 correspondiente y el complemento instalado y activado. Se proporcionan páginas UI, ventanas flotantes, cinco ejemplos, referencia de API y declaraciones TypeScript para esta integración local. La hoja de ruta registra el alcance verificado de compatibilidad y rendimiento. El complemento no figura en el índice oficial ni tiene una publicación oficial. El icono sigue siendo provisional hasta recibir las imágenes definitivas del mantenedor
* `Aviso` Esta versión usa el contrato Compose UI V2 y requiere AutoJs6 6.8.0 / 5322; TSX requiere TypeScript Engine 0.6.7. Los anfitriones nuevos admiten los componentes existentes de renderizadores V1; los componentes ampliados requieren V2
* `Aviso` compose.memo lo proporciona AutoJs6 6.8.0 / 5323 para reutilizar fragmentos de render cuyas dependencias no cambian; este complemento no necesita actualizarse y TSX requiere TypeScript Engine 0.6.8
* `Aviso` La galería y los ajustes se ejecutan en el proceso del complemento y no cambian el renderizado dentro del host ni la versión mínima del host; ejecutar los ejemplos requiere AutoJs6 con este complemento instalado y habilitado
* `Novedad` TSX admite `<compose.Column>`, `<compose:Text>`, referencias a fábricas de nodos, fragmentos, slots y callbacks reactivos; un mismo árbol no puede mezclar Compose y nodos XML existentes
* `Novedad` Los contenedores XML `<compose>` y compose.attach integran sesiones Compose independientes en páginas UI o ventanas flotantes existentes; compose.AndroidView muestra una View Android existente o devuelta por una fábrica síncrona
* `Novedad` compose.dialog devuelve una sesión que se puede actualizar y cerrar para diálogos y paneles inferiores modales en scripts UI o normales
* `Novedad` Componentes Material 3 ampliados: navegación y paneles laterales, pestañas, paneles inferiores y menús, selectores de fecha y hora, paginación y cuadrículas, chips, insignias, botones segmentados y flotantes, búsqueda, ayudas y actualización al deslizar
* `Novedad` Galería de componentes: ábrala desde el lanzador para ver vistas previas Material 3 y scripts de ejemplo de los 55 componentes, copiarlos al portapapeles o enviarlos al AutoJs6 instalado
* `Novedad` Página de ajustes: el idioma, el modo oscuro y el color de tema siguen AutoJs6 por defecto y pueden fijarse por separado; el icono del lanzador ofrece opciones adaptativas (claro / oscuro / automático) y de fondo transparente
* `Correccion` Corregido el fallo de compilación tras ajustar el fondo de los iconos del lanzador en Icon Studio
* `Mejora` Los iconos de información de la aplicación de Android comparten las imágenes y los fondos claros y oscuros de Icon Studio, conservando las imágenes transparentes del centro de plugins y las opciones del lanzador
* `Mejora` Las fábricas de View se ejecutan en el hilo principal antes del renderizado. Un reemplazo inválido conserva el contenido actual; una View no puede pertenecer a dos nodos ni tomarse de otro padre. Conserva sus listeners y los recursos siguen bajo control del llamador
* `Mejora` cancelable=false desactiva el cierre al volver, pulsar fuera o deslizar; el cierre explícito y la salida del script liberan el diálogo conservando las páginas y otras sesiones
* `Mejora` El icono del centro de complementos y el icono de información de la aplicación usan los fondos #FAFAFA claro / #212121 oscuro compartidos con los demás complementos independientes
* `Dependencia` Actualizar compose-ui-api.aar con la extensión opcional AndroidView conservando V1
* `Dependencia` Añadir la capacidad opcional de diálogo a compose-ui-api.aar conservando los contratos V1 y AndroidView existentes
* `Dependencia` Añadir el catálogo V2 de compose-ui-api.aar conservando los modelos de nodos y la semántica de componentes V1
* `Dependencia` Añadido material-color-utilities 4.1.1 (MIT) para la derivación del color de tema compartida con los demás complementos independientes
* `Dependencia` Añadidas copias de AndroidX activity, core, lifecycle, savedstate, emoji2, window, kotlinx-coroutines y sus complementos en las versiones fijadas por el host para la galería en el proceso del complemento; dentro del host siguen prevaleciendo las copias del host

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
