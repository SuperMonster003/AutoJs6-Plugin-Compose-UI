<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-block-compose-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Un plugin que lleva las interfaces de Jetpack Compose y Material 3 a los scripts de AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / Idiomas

******

Este documento esta disponible en los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ar.md)

******

### Introduccion

******

3-Block Compose es un plugin de renderizado de interfaces para AutoJs6. Los scripts declaran interfaces mediante `compose` / `$compose` del host y el plugin las renderiza dentro del proceso del host con Jetpack Compose y Material 3. La vista previa admite contenido de actividades `"ui";` y ventanas flotantes desde scripts no UI.

El complemento incluye un diseñador visual y una galería de componentes: al abrirlo desde el lanzador se llega al inicio del diseñador, donde se crean o importan proyectos de script y se editan sobre el renderizador real, mientras que la galería muestra vistas previas y scripts de ejemplo de cada componente; las interfaces de los scripts se siguen renderizando en el proceso del host. El host descubre el complemento mediante el servicio INFO, lee su versión y compatibilidad y crea la fábrica del renderizador en su propio proceso según el contrato (`org.autojs.plugin.compose.api`).

******

### Estado actual

******

1.7.0 es la versión pública actual (GitHub Release, incluida en el índice oficial de complementos). Requiere una compilación de AutoJs6 6.8.0 (5322) o posterior con la nueva identidad de 3-Block Compose (commit del host f12665ffde o posterior); AutoJs6 6.8.0 aún no se había publicado oficialmente cuando salió esta versión, por lo que se necesita una compilación de AutoJs6 que contenga ese commit. La entrada de menú del editor de AutoJs6 "Abrir en el diseñador de 3-Block Compose" necesita una compilación posterior al commit del host db7e6a1734, y los proyectos TSX del diseñador necesitan TypeScript Engine 0.7.0, y la vista previa de diseño de los proyectos UI (XML) necesita una compilación que contenga el servicio de vista previa (commit del host 306a8f7834 o posterior). El complemento ofrece páginas de UI, ventanas flotantes, el diseñador visual (proyectos Compose y UI), cinco ejemplos, la referencia de API, la página de guía del diseñador y las declaraciones de TypeScript. El alcance verificado de compatibilidad y rendimiento se registra en la hoja de ruta. Los iconos usan el arte final del mantenedor. Desde 1.7.0 la vista previa interactiva de los proyectos UI (XML) hace que AutoJs6 ejecute todo el script de verdad; requiere una compilación de AutoJs6 con el contrato de vista previa v2 (commit del host 4c332c2053 o posterior) y Android 13 o posterior.

******

### Caracteristicas

******

Funciones de la vista previa de desarrollo actual:

- Diseñador visual: el archivo de script es el proyecto, el renderizador real muestra el árbol de interfaz mientras lo editas, el esquema / la paleta de componentes / el panel de propiedades cubren los 55 componentes, con deshacer / rehacer y recuperación de borradores; el script exportado se puede copiar o enviar directamente a AutoJs6; lo que se suelta en el lienzo queda donde está el puntero, las ventanas emergentes abiertas se dibujan dentro del lienzo, Image muestra imágenes @drawable y de una carpeta de imágenes del proyecto, el panel Estado cambia temporalmente los valores de vista previa de los state, y la vista previa interactiva ejecuta el script de un proyecto JavaScript en un entorno aislado restringido (Android 8.0 o posterior), de modo que los toques y lo que se escribe actualizan el lienzo como decide el script, con una consola para su salida y sus errores; los proyectos pueden ser JavaScript o TSX, y los proyectos TSX se analizan con TypeScript Engine 0.7.0 o posterior y se guardan como TSX canónico
- Integración con AutoJs6: un proyecto se puede enviar a ejecutar, abrir en el editor de AutoJs6 (sincronización bidireccional), importar al directorio de scripts de AutoJs6, copiar o exportar; el diseñador también abre archivos .js / .tsx directamente desde el editor de AutoJs6 o un gestor de archivos y guarda en el mismo archivo; un proyecto importado se puede, a petición en el menú del editor, escribir de vuelta en su origen o reimportar desde él
- Proyectos UI (XML): el diseñador también abre scripts clásicos en modo "ui" (diseños XML E4X) y los edita en el sitio, conservando cada byte no tocado (espacios, comentarios, orden de atributos, comillas, {expresiones}); el lienzo lo dibuja el servicio de vista previa de AutoJs6 con el inflador de diseños del modo ui (vistas reales incrustadas desde Android 11, instantánea de mapa de bits en versiones anteriores), el cajón de propiedades sigue el catálogo de etiquetas y atributos del sitio de documentación, la paleta lista 48 etiquetas, un elemento puede recibir un esqueleto de controlador `ui.<id>.on`, y el panel de enlaces sustituye temporalmente los valores `{{ }}`
- Vista previa interactiva de los proyectos UI (XML) (1.7.0): con el interruptor "Interactuar" sobre el lienzo activado, AutoJs6 ejecuta todo el script como una ejecución normal en modo ui, el lienzo muestra el ui.layout del script y recibe los toques directamente, los controladores de eventos, los enlaces `{{ }}` y los demás módulos se ejecutan con la lógica real del script, y la pestaña de consola muestra la salida, los errores y el motivo de finalización; el script vuelve a ejecutarse unos 0.4 s después de editar, se detiene al apagar el interruptor o salir del diseñador, y el documento y el archivo no cambian
- Interfaz declarativa: `compose.state` + `compose.mount(render)` vuelven a dibujar automaticamente ante cada cambio de estado, mientras que los manejadores de nodo de larga vida (`compose.Text({...})` y similares) permiten modificar directamente propiedades e hijos
- Núcleo Material 3: 29 fábricas de nodos para diseños, texto, iconos, imágenes, botones, entradas, selección, listas diferidas, diálogos e indicadores; Snackbar es un comando de sesión, no una fábrica compose.Snackbar
- Modifiers encadenados: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` conserva el orden de las operaciones, y las operaciones con ambito se validan en el lado del host
- Dos superficies: `compose.mount` o `compose` / `$compose` invocables para contenido de actividades `"ui";`, y `compose.floaty` para ventanas flotantes raw o redimensionables, también desde scripts no UI
- Renderizado dentro del proceso anfitrión: las actualizaciones se aplican en el host y los eventos se encolan en el hilo propietario del script; los trabajadores solicitan cambios mediante compose.post
- Un APK incluye arm64-v8a / armeabi-v7a / x86_64 / x86, sin código nativo propio del complemento; incorpora la biblioteca auxiliar AndroidX graphics-path y conserva los requisitos de compatibilidad de Android, host y complemento
- La edición nativa conserva la selección y la composición IME, admite foco y cambios explícitos, y rechaza cambios tardíos que sobrescribirían entradas más recientes; interruptores y deslizadores siguen controlados por el script
- Protecciones de integración: las comprobaciones indican no disponible si falta el plugin o es incompatible, los errores usan `ComposeError`, y cerrar la sesión o detener el script libera sus ventanas y callbacks
- Cinco ejemplos ejecutables de contador, validación de formularios, lista de 1000 elementos con claves estables, HUD flotante sin modo UI y temas, con requisitos e índice, sincronizados con la categoría 3-Block Compose del host correspondiente
- TSX admite `<compose.Column>`, `<compose:Text>`, referencias a fábricas de nodos, fragmentos, slots y callbacks reactivos; un mismo árbol no puede mezclar Compose y nodos XML existentes
- Los contenedores XML `<compose>` y compose.attach integran sesiones Compose independientes en páginas UI o ventanas flotantes existentes; compose.AndroidView muestra una View Android existente o devuelta por una fábrica síncrona
- compose.dialog devuelve una sesión que se puede actualizar y cerrar para diálogos y paneles inferiores modales en scripts UI o normales
- Componentes Material 3 ampliados: navegación y paneles laterales, pestañas, paneles inferiores y menús, selectores de fecha y hora, paginación y cuadrículas, chips, insignias, botones segmentados y flotantes, búsqueda, ayudas y actualización al deslizar
- Galería de componentes: la entrada del lanzador muestra vistas previas Material 3 y scripts de ejemplo ejecutables de los 55 componentes, que se pueden copiar o enviar a AutoJs6; la página de ajustes sigue por defecto el idioma, el modo oscuro y el color de tema de AutoJs6 y ofrece cuatro iconos de lanzador

******

### Uso

******

1. Instale una compilación local compatible de AutoJs6 que incluya la entrada compose (mínimo 6.8.0 / 5322)
2. Instale el APK del complemento; abra 3-Block Compose desde el lanzador para llegar al inicio del diseñador, crear un proyecto de script o abrir la galería de componentes y los ajustes desde la barra superior
3. Confirme en el centro de plugins de AutoJs6 que 3-Block Compose se reconoce y esta habilitado
4. Use `compose` o `$compose` en scripts; monte actividades con `compose.mount`, o conceda al host permiso de superposición y use `compose.floaty`
5. El menú del editor ofrece ejecutar, abrir en el editor de AutoJs6, importar a los scripts de AutoJs6, copiar y exportar; elegir 3-Block Compose desde el menú del editor de AutoJs6 (requiere una compilación de AutoJs6 con la actualización correspondiente) o desde un gestor de archivos edita el archivo de script directamente

******

### Inicio rapido

******

El contador y el HUD flotante siguientes pueden ejecutarse con el host local compatible de la vista previa. Antes de ejecutar el HUD, permita al host mostrarse sobre otras aplicaciones:

```js
"ui";

// Contador (capa render declarativa)
let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `${count.value} clics`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, 'Sumar uno'),
]));
```

```js
// HUD flotante (capa de manejadores de nodo)
let worker = null;
let status = compose.Text({ text: 'Preparando...', color: '#FFFFFF' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ contentColor: '#FFFFFF', onClick: () => win.close() }, 'Cerrar'),
]), { x: 50, y: 300, raw: true });
win.on('close', () => { if (worker) worker.interrupt(); });

worker = threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => {
            if (!win.isClosed()) status.text = `Progreso ${i}%`;
        });
    }
});
```

Los cinco scripts ejecutables se enumeran en assets/examples/index.json y se sincronizan con la categoría 3-Block Compose del host correspondiente. Cada cabecera explica el modo y los permisos. Consulte la referencia API local y las declaraciones TypeScript/editor asociadas para nodos, modificadores, temas, sesiones y ventanas; el sitio en línea puede no incluir aún estos cambios locales.

******

### Compatibilidad

******

Requisitos de ejecucion y limites del plugin:

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
- Ejecutar el script en la vista previa interactiva del diseñador requiere Android 8.0 (API 26) o posterior, porque el Date de JavaScript del entorno aislado se basa en java.time; con Android anterior, un proyecto JavaScript se previsualiza como un proyecto TSX
- La vista previa de diseño de los proyectos UI (XML) necesita una compilación de AutoJs6 6.8.0 que contenga el servicio de vista previa (commit del host 306a8f7834 o posterior; el complemento comprueba la presencia del servicio, no un número de versión); desde Android 11 (API 30) el lienzo incrusta las vistas reales que dibuja AutoJs6, las versiones anteriores muestran una instantánea de mapa de bits; sin el servicio un proyecto UI sigue siendo editable y el lienzo indica el motivo
- La vista previa interactiva de los proyectos UI (XML) necesita una compilación de AutoJs6 6.8.0 con el contrato de vista previa v2 (commit del host 4c332c2053 o posterior) y Android 13 (API 33) o posterior; en Android 13 - 14 los campos de entrada del lienzo no reciben teclado, desde Android 15 sí; si no se cumplen las condiciones el interruptor se oculta, la fila sobre el lienzo explica el motivo y queda "Ejecutar en AutoJs6"; una compilación de AutoJs6 con solo el contrato v1 sigue ofreciendo la vista previa de diseño

******

### Preguntas frecuentes

******

- Como se ejecutan los ejemplos de la galería? "Ejecutar en AutoJs6" entrega el script al AutoJs6 instalado; la galería solo muestra vistas previas y código y nunca ejecuta scripts
- Puede el diseñador abrir cualquier script? Solo interpreta las declaraciones `compose.state` de nivel superior y la única llamada `compose.mount` / `floaty` / `dialog`; el resto de sentencias se conserva tal cual. Las formas que no puede asignar (una raíz que no es una fábrica compose, varias llamadas de montaje, una función de renderizado sin return) se rechazan con línea y columna. En los proyectos TSX, las etiquetas `compose.X` / `compose:X` son nodos editables, las demás etiquetas y las expresiones `{}` se conservan tal cual, y las etiquetas XML y los atributos de propagación se rechazan
- El diseñador ejecuta scripts? Solo en la vista previa interactiva. Al editar solo los analiza (JavaScript con el analizador Rhino, TSX con el servicio de análisis de TypeScript Engine) y los previsualiza con el renderizador propio del complemento; los controladores de eventos no se ejecutan y el state muestra su valor inicial o un valor de vista previa fijado en el panel Estado. Con Interactuar activado, el script de un proyecto JavaScript se ejecuta dentro del complemento en un entorno aislado restringido (Android 8.0 o posterior): solo dispone de compose, console, toast y temporizadores, no puede acceder a clases de Java / Android, archivos, la red ni otros módulos de AutoJs6, se detiene cuando una ejecución dura más de unos 3 segundos y solo cambia el lienzo, nunca el proyecto. Un proyecto TSX (o cualquier proyecto con Android anterior a 8.0) no ejecuta su script; los controles vinculados a un state solo cambian su valor de vista previa. La ejecución real corresponde a AutoJs6. El lienzo de un proyecto UI (XML) solo lo compone el servicio de vista previa de AutoJs6 (crea y dibuja las vistas) sin ejecutar el script del proyecto; los valores `{{ }}` provienen de las declaraciones literales de nivel superior del script (evaluadas en el entorno aislado) o de las sustituciones del panel Enlaces. Desde 1.7.0 la vista previa interactiva de un proyecto UI (XML) es distinta: con el interruptor activado, AutoJs6 ejecuta todo el script de verdad, igual que "Ejecutar en AutoJs6" (sin los límites del sandbox, los efectos secundarios ocurren de verdad, cada proyecto pregunta una vez antes de su primera ejecución), y solo su contenido de ui.layout se muestra en el lienzo; el resultado sigue sin escribirse en el proyecto
- Por qué falta `compose`? El objeto global lo proporciona la compilación local compatible del host; instalar solo el APK del plugin no lo añade
- Hay que desinstalar otros plugins de interfaz? No, 3-Block Compose no interfiere con el modulo `ui` existente ni con otros plugins
- Qué ocurre si cambia el complemento? Actualizarlo, desinstalarlo o desactivarlo cierra las sesiones activas e informa del error correspondiente; un complemento compatible y activado permite volver a montar
- Qué requieren las ventanas flotantes? Conceda permiso de superposición al host y llame a `window.requestFocus()` antes de escribir. Si HyperOS no muestra la ventana, vuelva al escritorio. La falta de permiso devuelve PERMISSION_REQUIRED sin abrir automáticamente una solicitud de autorización
- Se puede usar TSX o cualquier función Compose? TSX admite las fábricas Compose documentadas con el host y TypeScript Engine correspondientes. No admite funciones Kotlin Composable arbitrarias ni componentes TSX personalizados
- Se pierde el estado al girar? El host actual gestiona los cambios normales de orientación sin reemplazar el motor de scripts. La recreación o destrucción real de la Activity cierra el motor y sus sesiones; el estado de negocio no se restaura automáticamente
- Cómo encuentran los selectores los componentes? testTag se expone como ID original sin prefijo de paquete. id/testTag y desc/contentDescription son distintos; el texto de Button puede ser un hijo, por lo que debe seguir parent() hasta un ancestro pulsable cuando haga falta

******

### Permisos y seguridad

******

El plugin no solicita ningun permiso de tiempo de ejecucion de Android y nunca accede a la red, al almacenamiento ni a los sensores.

- Protección de componentes: la Wake Activity y el servicio INFO están protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo el host AutoJs6 puede acceder a ellos; el editor del diseñador es un componente exportado sin permiso que solo acepta el Uri de un documento de script entregado por el usuario, y la lectura y escritura se limitan a ese Uri
- Comportamiento en segundo plano: el complemento no tiene servicios residentes ni tareas programadas y solo recibe una difusión del sistema al actualizarse, para normalizar sus componentes de icono del lanzador; no consume nada mientras el host no lo carga y ni el diseñador ni la galería están abiertos
- Límite de datos: el renderizador nunca lee ni escribe datos de scripts ni archivos del usuario, y el estado de la interfaz solo existe en la memoria del proceso del host; los proyectos y borradores del diseñador viven en el directorio privado del complemento, se importan y exportan solo mediante el selector de documentos del sistema, o leen y escriben un único archivo de script mediante un Uri entregado por el usuario; el directorio de proyectos se comparte solo a través del proveedor de contenido propio del complemento, por solicitud, al entregar un proyecto a AutoJs6; una importación conserva el acceso al archivo elegido cuando su proveedor lo permite, pero ese archivo solo se lee o escribe cuando el usuario elige "Escribir de vuelta en el origen" o "Volver a importar desde el origen"
- Entorno aislado de scripts: la vista previa interactiva del diseñador ejecuta el script de un proyecto JavaScript dentro del complemento con el intérprete de Rhino. Solo ofrece compose, console, toast y temporizadores; las clases de Java / Android, los archivos, la red y los demás módulos de AutoJs6 quedan fuera de su alcance, cada ejecución tiene límites de instrucciones y de tiempo, y lo que hace se queda en el lienzo: el proyecto nunca se escribe
- Politica de copias de seguridad: la copia de seguridad de la aplicacion y la transferencia entre dispositivos estan deshabilitadas, y el plugin no guarda datos que migrar
- Vista previa de diseño: el lienzo de un proyecto UI (XML) lo dibuja el AutoJs6 instalado: el complemento solo le entrega el fragmento XML y los valores de enlace a través del servicio de vista previa de AutoJs6 (protegido por el permiso de firma PLUGIN), AutoJs6 crea las vistas con su inflador de diseños del modo ui y las dibuja de vuelta, no ejecuta ningún script del proyecto y no puede acceder a los archivos de proyecto del complemento; ese tiempo de ejecución de vista previa aparece en la lista de ejecución de AutoJs6 y puede detenerse en cualquier momento
- Vista previa interactiva (proyectos UI): con el interruptor activado, el complemento entrega el texto del script al servicio de vista previa de AutoJs6 y AutoJs6 lo ejecuta como un script normal en modo ui (el mismo motor y los mismos permisos de script que "Ejecutar en AutoJs6", los efectos secundarios ocurren de verdad), muestra solo su contenido de ui.layout en el lienzo y devuelve la salida de consola; cada proyecto pregunta una vez antes de su primera ejecución, la ejecución aparece en la lista de ejecución de AutoJs6 y termina al apagar el interruptor, salir del diseñador o detenerla desde AutoJs6; el script no se ejecuta en el proceso del complemento y el complemento no amplía sus permisos

Al cargar el renderizador, el host conserva su propio modelo de permisos de script; el plugin no amplia las capacidades del sistema a las que pueden acceder los scripts.

******

### Interfaz del plugin

******

Identificadores expuestos al host:

```text
application id: io.github.supermonster003.autojs6.plugin.three.block.compose
plugin id: three-block-compose
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: three-block-compose
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 2)
minimum host build: 5322 (6.8.0)
```

El host descubre el plugin mediante `org.autojs.plugin.INFO` y lee datos de capacidad como `requiresHostVersion`; la clase de fabrica del renderizador se declara con el metadato `org.autojs.plugin.compose.RENDERER_FACTORY`, y el host crea un cargador de clases a partir de la ruta del APK del plugin (con el host como padre) y lo instancia dentro de su propio proceso.

******

### Hoja de ruta

******

Los hitos, las decisiones de diseño y los criterios de aceptacion se registran en una unica hoja de ruta:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.7.0

_2026/10/15_

- `Aviso` La vista previa interactiva de los proyectos UI (XML) ahora hace que AutoJs6 ejecute todo el script de verdad (requiere una compilación de AutoJs6 6.8.0 con el contrato de vista previa v2, commit del host 4c332c2053 o posterior, y Android 13 o posterior): los efectos secundarios del script son los mismos que al ejecutarlo en AutoJs6, y cada proyecto pregunta una vez antes de activar el interruptor; si no se cumplen las condiciones, la fila sobre el lienzo explica el motivo y queda "Ejecutar en AutoJs6"
- `Novedad` Vista previa interactiva de los proyectos UI (XML): con el interruptor "Interactuar" sobre el lienzo activado, AutoJs6 ejecuta todo el script como una ejecución en modo ui, el lienzo muestra el ui.layout del script y recibe los toques directamente, y los controladores de eventos, los enlaces {{ }}, los temporizadores y los demás módulos se ejecutan con la lógica real del script; el script vuelve a ejecutarse unos 0.4 s después de editar, se detiene al apagar el interruptor, al pasar el diseñador a segundo plano o al cerrar el editor, y el lienzo vuelve a la vista previa de diseño; el documento, el archivo del proyecto y el borrador nunca cambian. En Android 13 - 14 los campos de entrada del lienzo no reciben teclado, desde Android 15 sí
- `Novedad` Durante la vista previa interactiva, la pestaña de consola de un proyecto UI muestra la salida del script que ejecuta AutoJs6 (coloreada por nivel), el estado de la ejecución y su motivo de finalización; si el script falla, el error (con su línea) aparece sobre el lienzo y un toque abre la consola; la ejecución aparece con el nombre del archivo del proyecto en la lista de ejecución de AutoJs6 y puede detenerse desde allí
- `Correccion` El diseñador mostraba a veces "El servicio de vista previa de AutoJs6 no está disponible ahora (at most 2 sessions per client)" al abrir un proyecto UI: cuando el tamaño del lienzo cambiaba dos veces antes de asentarse el diseño, el complemento creaba dos veces la sesión de vista previa en AutoJs6 y alcanzaba el límite de sesiones por cliente; ahora se crea una sesión a la vez y el tamaño o la apariencia que cambió mientras tanto se aplica después
- `Dependencia` Actualiza el artefacto de contrato ui-preview-api.aar al contrato de vista previa ui v2 (añade, después de v1, las interfaces para ejecutar y detener un script y las devoluciones de llamada de consola / error / fin; las compilaciones de AutoJs6 con solo v1 siguen ofreciendo la vista previa de diseño)

#### v1.6.0

_2026/10/15_

- `Aviso` El diseñador incorpora proyectos UI (XML): los scripts clásicos en modo "ui" (diseños XML E4X) pueden abrirse y editarse, con el lienzo dibujado por AutoJs6. La vista previa de diseño necesita una compilación de AutoJs6 6.8.0 que contenga el servicio de vista previa (commit del host 306a8f7834 o posterior); desde Android 11 (API 30) el lienzo incrusta las vistas reales que dibuja AutoJs6, las versiones anteriores muestran una instantánea de mapa de bits; sin el servicio el proyecto sigue siendo editable y el lienzo indica el motivo
- `Novedad` El diseñador admite proyectos UI (XML): elige "UI (XML)" al crear un proyecto, o importa un script en modo "ui" o ábrelo desde el editor de AutoJs6 / un gestor de archivos y se lee como proyecto UI; la lista de inicio marca cada proyecto como JS / TSX / UI. Los diseños XML del script se conservan como un árbol sintáctico con posiciones: las partes no tocadas (espacios, comentarios, orden de atributos, comillas, {expresiones}) se preservan byte a byte y solo se reescriben las etiquetas de apertura editadas, el texto y los elementos recién insertados; los demás literales XML del script también pueden llevarse al lienzo
- `Novedad` El editor de proyectos UI: esquema (contraer, subir / bajar, eliminar, arrastrar con pulsación larga, selector cuando hay varios diseños), cajón de propiedades (etiqueta, id, contenido de texto, atributos ya definidos y atributos del catálogo agrupados por clase View, estados texto / enlace `{{ }}` / {expresión}, atributos sin valor, cualquier nombre de atributo), paleta de componentes (las 48 etiquetas de la documentación por categoría con atributos predeterminados), panel de enlaces (valores evaluados de los nombres `{{ }}` y sustituciones que solo afectan al lienzo), lista de problemas (comprobada con el catálogo de etiquetas y atributos del sitio de documentación) y consola (lo que AutoJs6 informó al dibujar el diseño); guardar, recuperación de borradores, sincronización con el origen, ejecutar, abrir en el editor de AutoJs6, importar al directorio de scripts de AutoJs6, copiar y exportar funcionan también con los proyectos UI
- `Novedad` El lienzo de un proyecto UI lo dibuja AutoJs6 (vista previa de diseño): el complemento entrega el fragmento XML y los valores de enlace al servicio de vista previa de AutoJs6, que crea vistas reales con su inflador de diseños del modo ui y las dibuja en el lienzo sin ejecutar el script del proyecto; la selección y el arrastre funcionan en el lienzo como siempre, y un fotograma fallido señala el elemento culpable; el tiempo de ejecución de vista previa aparece como "3-Block Compose preview" en la lista de ejecución de AutoJs6 y puede detenerse allí
- `Novedad` El cajón de propiedades de un proyecto UI añade eventos al elemento seleccionado: se anexa un esqueleto de controlador `ui.<id>.on('evento', ...)` después de la llamada a ui.layout (un elemento sin id recibe uno) y su cuerpo se escribe en el editor de scripts; los eventos que el script ya registra aparecen como "Gestionado en el script"
- `Dependencia` Añade el artefacto de contrato ui-preview-api.aar (contrato de vista previa de diseños ui de AutoJs6 v1), con el que el diseñador pide a AutoJs6 que dibuje el lienzo de los proyectos UI

#### v1.5.1

_2026/10/15_

- `Correccion` El complemento podía cerrarse de forma inesperada cuando aparecía un nuevo campo de texto en el lienzo del diseñador, por ejemplo al insertar un campo de texto desde la paleta de componentes, cuando deshacer / rehacer volvía a mostrar un campo de texto, o cuando un script de la vista previa interactiva mostraba uno (desde 1.4.0); ahora estos campos de texto aparecen con normalidad
- `Correccion` Completar las traducciones del encabezado de clave en el inspector del diseñador

##### Para ver mas historial de versiones, consulte

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilacion

******

Tras clonar el repositorio, compile directamente con el Gradle Wrapper; las versiones del plugin de Android Gradle y de Kotlin las selecciona automaticamente el plugin de versiones de plataforma segun el entorno IDE actual.

Compilar el APK de depuracion:

```powershell
.\gradlew.bat :app:assembleDebug
```

Ejecutar las pruebas unitarias de JVM y empaquetar las pruebas de contrato en dispositivo:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compilar el APK de lanzamiento (requiere `sign.properties` y la clave de firma):

```powershell
.\gradlew.bat :app:assembleRelease
```

Verificar la firma y generar el archivo de lanzamiento con sufijo de resumen:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verificar que los documentos localizados coinciden con sus fuentes:

```powershell
py .python\generate_markdown.py --check
```

La compilacion requiere JDK 21 o posterior. Tras editar las fuentes de `.readme` o `.changelog`, ejecute `py .python\generate_markdown.py` para regenerar todos los documentos.

******

### Organizacion de la documentacion

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

El README, la instruccion del centro de plugins y el registro de cambios se generan a partir de las fuentes JSON de `.readme` y `.changelog`; no edite directamente los archivos Markdown generados.

******

### Licencia

******

Este proyecto se publica bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE). La informacion de licencia de los componentes de terceros figura en [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md).

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentacion de AutoJs6: https://docs.autojs6.com
- Documentacion del modulo compose: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md
