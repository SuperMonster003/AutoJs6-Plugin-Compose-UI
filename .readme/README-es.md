<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Un plugin que lleva las interfaces de Jetpack Compose y Material 3 a los scripts de AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Compose-UI?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / Idiomas

******

Este documento esta disponible en los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ar.md)

******

### Introduccion

******

Compose UI es un plugin de renderizado de interfaces para AutoJs6. Los scripts declaran interfaces mediante `compose` / `$compose` del host y el plugin las renderiza dentro del proceso del host con Jetpack Compose y Material 3. La vista previa admite contenido de actividades `"ui";` y ventanas flotantes desde scripts no UI.

El complemento incluye una galería de componentes que se abre desde el lanzador y muestra vistas previas y scripts de ejemplo de cada componente; las interfaces de los scripts se siguen renderizando en el proceso del host. El host descubre el complemento mediante el servicio INFO, lee su versión y compatibilidad y crea la fábrica del renderizador en su propio proceso según el contrato (`org.autojs.plugin.compose.api`).

******

### Estado actual

******

Vista previa local de desarrollo 1.1.0: requiere una compilación AutoJs6 correspondiente y el complemento instalado y activado. Se proporcionan páginas UI, ventanas flotantes, cinco ejemplos, referencia de API y declaraciones TypeScript para esta integración local. La hoja de ruta registra el alcance verificado de compatibilidad y rendimiento. El complemento no figura en el índice oficial ni tiene una publicación oficial. El icono sigue siendo provisional hasta recibir las imágenes definitivas del mantenedor.

******

### Caracteristicas

******

Funciones de la vista previa de desarrollo actual:

- Interfaz declarativa: `compose.state` + `compose.mount(render)` vuelven a dibujar automaticamente ante cada cambio de estado, mientras que los manejadores de nodo de larga vida (`compose.Text({...})` y similares) permiten modificar directamente propiedades e hijos
- Núcleo Material 3: 29 fábricas de nodos para diseños, texto, iconos, imágenes, botones, entradas, selección, listas diferidas, diálogos e indicadores; Snackbar es un comando de sesión, no una fábrica compose.Snackbar
- Modifiers encadenados: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` conserva el orden de las operaciones, y las operaciones con ambito se validan en el lado del host
- Dos superficies: `compose.mount` o `compose` / `$compose` invocables para contenido de actividades `"ui";`, y `compose.floaty` para ventanas flotantes raw o redimensionables, también desde scripts no UI
- Renderizado dentro del proceso anfitrión: las actualizaciones se aplican en el host y los eventos se encolan en el hilo propietario del script; los trabajadores solicitan cambios mediante compose.post
- Un APK incluye arm64-v8a / armeabi-v7a / x86_64 / x86, sin código nativo propio del complemento; incorpora la biblioteca auxiliar AndroidX graphics-path y conserva los requisitos de compatibilidad de Android, host y complemento
- La edición nativa conserva la selección y la composición IME, admite foco y cambios explícitos, y rechaza cambios tardíos que sobrescribirían entradas más recientes; interruptores y deslizadores siguen controlados por el script
- Protecciones de integración: las comprobaciones indican no disponible si falta el plugin o es incompatible, los errores usan `ComposeError`, y cerrar la sesión o detener el script libera sus ventanas y callbacks
- Cinco ejemplos ejecutables de contador, validación de formularios, lista de 1000 elementos con claves estables, HUD flotante sin modo UI y temas, con requisitos e índice, sincronizados con la categoría Compose UI del host correspondiente
- TSX admite `<compose.Column>`, `<compose:Text>`, referencias a fábricas de nodos, fragmentos, slots y callbacks reactivos; un mismo árbol no puede mezclar Compose y nodos XML existentes
- Los contenedores XML `<compose>` y compose.attach integran sesiones Compose independientes en páginas UI o ventanas flotantes existentes; compose.AndroidView muestra una View Android existente o devuelta por una fábrica síncrona
- compose.dialog devuelve una sesión que se puede actualizar y cerrar para diálogos y paneles inferiores modales en scripts UI o normales
- Componentes Material 3 ampliados: navegación y paneles laterales, pestañas, paneles inferiores y menús, selectores de fecha y hora, paginación y cuadrículas, chips, insignias, botones segmentados y flotantes, búsqueda, ayudas y actualización al deslizar
- Galería de componentes: la entrada del lanzador muestra vistas previas Material 3 y scripts de ejemplo ejecutables de los 55 componentes, que se pueden copiar o enviar a AutoJs6; la página de ajustes sigue por defecto el idioma, el modo oscuro y el color de tema de AutoJs6 y ofrece cuatro iconos de lanzador

******

### Uso

******

1. Instale una compilación local compatible de AutoJs6 que incluya la entrada compose (mínimo 6.8.0 / 5322)
2. Instale el APK del complemento; abra Compose UI desde el lanzador para ver la galería de componentes y los ajustes
3. Confirme en el centro de plugins de AutoJs6 que Compose UI se reconoce y esta habilitado
4. Use `compose` o `$compose` en scripts; monte actividades con `compose.mount`, o conceda al host permiso de superposición y use `compose.floaty`

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

Los cinco scripts ejecutables se enumeran en assets/examples/index.json y se sincronizan con la categoría Compose UI del host correspondiente. Cada cabecera explica el modo y los permisos. Consulte la referencia API local y las declaraciones TypeScript/editor asociadas para nodos, modificadores, temas, sesiones y ventanas; el sitio en línea puede no incluir aún estos cambios locales.

******

### Compatibilidad

******

Requisitos de ejecucion y limites del plugin:

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

******

### Preguntas frecuentes

******

- Como se ejecutan los ejemplos de la galería? "Ejecutar en AutoJs6" entrega el script al AutoJs6 instalado; la galería solo muestra vistas previas y código y nunca ejecuta scripts
- Por qué falta `compose`? El objeto global lo proporciona la compilación local compatible del host; instalar solo el APK del plugin no lo añade
- Hay que desinstalar otros plugins de interfaz? No, Compose UI no interfiere con el modulo `ui` existente ni con otros plugins
- Qué ocurre si cambia el complemento? Actualizarlo, desinstalarlo o desactivarlo cierra las sesiones activas e informa del error correspondiente; un complemento compatible y activado permite volver a montar
- Qué requieren las ventanas flotantes? Conceda permiso de superposición al host y llame a `window.requestFocus()` antes de escribir. Si HyperOS no muestra la ventana, vuelva al escritorio. La falta de permiso devuelve PERMISSION_REQUIRED sin abrir automáticamente una solicitud de autorización
- Se puede usar TSX o cualquier función Compose? TSX admite las fábricas Compose documentadas con el host y TypeScript Engine correspondientes. No admite funciones Kotlin Composable arbitrarias ni componentes TSX personalizados
- Se pierde el estado al girar? El host actual gestiona los cambios normales de orientación sin reemplazar el motor de scripts. La recreación o destrucción real de la Activity cierra el motor y sus sesiones; el estado de negocio no se restaura automáticamente
- Cómo encuentran los selectores los componentes? testTag se expone como ID original sin prefijo de paquete. id/testTag y desc/contentDescription son distintos; el texto de Button puede ser un hijo, por lo que debe seguir parent() hasta un ancestro pulsable cuando haga falta

******

### Permisos y seguridad

******

El plugin no solicita ningun permiso de tiempo de ejecucion de Android y nunca accede a la red, al almacenamiento ni a los sensores.

- Proteccion de componentes: tanto la Wake Activity como el servicio INFO estan protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo el host AutoJs6 puede acceder a ellos
- Comportamiento en segundo plano: sin servicio residente ni tareas programadas; el complemento recibe una difusión del sistema al actualizarse para normalizar los componentes del icono del lanzador y no consume recursos mientras el host no lo carga ni está abierto
- Limite de datos: el plugin nunca lee ni escribe datos de scripts ni archivos del usuario; el estado de la interfaz solo existe en la memoria del proceso del host
- Politica de copias de seguridad: la copia de seguridad de la aplicacion y la transferencia entre dispositivos estan deshabilitadas, y el plugin no guarda datos que migrar

Al cargar el renderizador, el host conserva su propio modelo de permisos de script; el plugin no amplia las capacidades del sistema a las que pueden acceder los scripts.

******

### Interfaz del plugin

******

Identificadores expuestos al host:

```text
application id: io.github.supermonster003.autojs6.plugin.compose.ui
plugin id: compose-ui
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: compose-ui
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 2)
minimum host build: 5322 (6.8.0)
```

El host descubre el plugin mediante `org.autojs.plugin.INFO` y lee datos de capacidad como `requiresHostVersion`; la clase de fabrica del renderizador se declara con el metadato `org.autojs.plugin.compose.RENDERER_FACTORY`, y el host crea un cargador de clases a partir de la ruta del APK del plugin (con el host como padre) y lo instancia dentro de su propio proceso.

******

### Hoja de ruta

******

Los hitos, las decisiones de diseño y los criterios de aceptacion se registran en una unica hoja de ruta:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.1.0

_2026/10/08_

- `Aviso` Vista previa local de desarrollo 1.1.0: requiere una compilación AutoJs6 correspondiente y el complemento instalado y activado. Se proporcionan páginas UI, ventanas flotantes, cinco ejemplos, referencia de API y declaraciones TypeScript para esta integración local. La hoja de ruta registra el alcance verificado de compatibilidad y rendimiento. El complemento no figura en el índice oficial ni tiene una publicación oficial. El icono sigue siendo provisional hasta recibir las imágenes definitivas del mantenedor
- `Aviso` Esta versión usa el contrato Compose UI V2 y requiere AutoJs6 6.8.0 / 5322; TSX requiere TypeScript Engine 0.6.7. Los anfitriones nuevos admiten los componentes existentes de renderizadores V1; los componentes ampliados requieren V2
- `Aviso` compose.memo lo proporciona AutoJs6 6.8.0 / 5323 para reutilizar fragmentos de render cuyas dependencias no cambian; este complemento no necesita actualizarse y TSX requiere TypeScript Engine 0.6.8
- `Aviso` La galería y los ajustes se ejecutan en el proceso del complemento y no cambian el renderizado dentro del host ni la versión mínima del host; ejecutar los ejemplos requiere AutoJs6 con este complemento instalado y habilitado
- `Novedad` TSX admite `<compose.Column>`, `<compose:Text>`, referencias a fábricas de nodos, fragmentos, slots y callbacks reactivos; un mismo árbol no puede mezclar Compose y nodos XML existentes
- `Novedad` Los contenedores XML `<compose>` y compose.attach integran sesiones Compose independientes en páginas UI o ventanas flotantes existentes; compose.AndroidView muestra una View Android existente o devuelta por una fábrica síncrona
- `Novedad` compose.dialog devuelve una sesión que se puede actualizar y cerrar para diálogos y paneles inferiores modales en scripts UI o normales
- `Novedad` Componentes Material 3 ampliados: navegación y paneles laterales, pestañas, paneles inferiores y menús, selectores de fecha y hora, paginación y cuadrículas, chips, insignias, botones segmentados y flotantes, búsqueda, ayudas y actualización al deslizar
- `Novedad` Galería de componentes: ábrala desde el lanzador para ver vistas previas Material 3 y scripts de ejemplo de los 55 componentes, copiarlos al portapapeles o enviarlos al AutoJs6 instalado
- `Novedad` Página de ajustes: el idioma, el modo oscuro y el color de tema siguen AutoJs6 por defecto y pueden fijarse por separado; el icono del lanzador ofrece opciones adaptativas (claro / oscuro / automático) y de fondo transparente
- `Mejora` Los iconos de información de la aplicación de Android comparten las imágenes y los fondos claros y oscuros de Icon Studio, conservando las imágenes transparentes del centro de plugins y las opciones del lanzador
- `Mejora` Las fábricas de View se ejecutan en el hilo principal antes del renderizado. Un reemplazo inválido conserva el contenido actual; una View no puede pertenecer a dos nodos ni tomarse de otro padre. Conserva sus listeners y los recursos siguen bajo control del llamador
- `Mejora` cancelable=false desactiva el cierre al volver, pulsar fuera o deslizar; el cierre explícito y la salida del script liberan el diálogo conservando las páginas y otras sesiones
- `Mejora` El icono del centro de complementos y el icono de información de la aplicación usan los fondos #FAFAFA claro / #212121 oscuro compartidos con los demás complementos independientes
- `Dependencia` Actualizar compose-ui-api.aar con la extensión opcional AndroidView conservando V1
- `Dependencia` Añadir la capacidad opcional de diálogo a compose-ui-api.aar conservando los contratos V1 y AndroidView existentes
- `Dependencia` Añadir el catálogo V2 de compose-ui-api.aar conservando los modelos de nodos y la semántica de componentes V1
- `Dependencia` Añadido material-color-utilities 4.1.1 (MIT) para la derivación del color de tema compartida con los demás complementos independientes
- `Dependencia` Añadidas copias de AndroidX activity, core, lifecycle, savedstate y kotlinx-coroutines en las versiones fijadas por el host para la galería en el proceso del complemento; dentro del host siguen prevaleciendo las copias del host

#### v1.0.0

_2026/10/03_

- `Aviso` Vista previa local de desarrollo 1.0.0: requiere una compilación AutoJs6 correspondiente y el complemento instalado y activado. Se proporcionan páginas UI, ventanas flotantes, cinco ejemplos, referencia de API y declaraciones TypeScript para esta integración local. La hoja de ruta registra el alcance verificado de compatibilidad y rendimiento. El complemento no figura en el índice oficial ni tiene una publicación oficial. El icono sigue siendo provisional hasta recibir las imágenes definitivas del mantenedor
- `Aviso` Requiere AutoJs6 6.8.0 (5316) o posterior
- `Aviso` Las aplicaciones empaquetadas también requieren instalar por separado un complemento Compose UI compatible, con activación/autorización propias de la aplicación; se comprueba el runtime AutoJs6 integrado, no el versionCode de la aplicación
- `Aviso` Se mantienen los dos mipmap claro/oscuro; el dibujo actual es provisional hasta que el mantenedor proporcione las imágenes definitivas en blanco y negro
- `Novedad` compose / $compose invocables, manejadores de nodos persistentes, state/render/ref reactivos, cambios agrupados, tareas encoladas y control del tema
- `Novedad` Núcleo Material 3: 29 fábricas de nodos para diseños, texto, iconos, imágenes, botones, entradas, selección, listas diferidas, diálogos e indicadores; Snackbar es un comando de sesión, no una fábrica compose.Snackbar
- `Novedad` Los scripts UI montan contenido Activity; compose.floaty también ofrece a scripts sin UI ventanas raw o redimensionables, geometría en píxeles, controles táctiles/de foco y limpieza de sus recursos
- `Novedad` Las 20 operaciones Modifier conservan el orden declarado y admiten validación del ámbito de diseño, desplazamiento y etiquetas de accesibilidad
- `Novedad` Los temas Material 3 admiten colores semilla, modos claro y oscuro, colores dinámicos del sistema en Android 12+, familias tipográficas y escala de texto
- `Novedad` La edición nativa conserva la selección y la composición IME, admite foco y cambios explícitos, y rechaza cambios tardíos que sobrescribirían entradas más recientes; interruptores y deslizadores siguen controlados por el script
- `Novedad` Iconos core e imágenes ImageWrapper/Bitmap, archivos locales y drawable del host; el renderizador no recicla automáticamente las imágenes propiedad del llamante
- `Novedad` Cinco ejemplos ejecutables de contador, validación de formularios, lista de 1000 elementos con claves estables, HUD flotante sin modo UI y temas, con requisitos e índice, sincronizados con la categoría Compose UI del host correspondiente
- `Novedad` Referencia API y declaraciones TypeScript asociadas, además de README, instrucciones del centro de complementos y registro de cambios en 10 idiomas
- `Novedad` Descubrimiento en el centro de complementos con comprobaciones de versión del host, contrato y autorización, sin pantalla independiente ni entrada de lanzador
- `Correccion` Compose UI rechaza volver a montar páginas o ventanas flotantes dentro del callback de renderizado y conserva la página actual; permite volver a montar páginas después de actualizar el plugin
- `Mejora` Las comprobaciones y ComposeError informan de plugins ausentes, desactivados, no autorizados o incompatibles, permisos insuficientes y sesiones cerradas; la limpieza también cubre ventanas canceladas antes de su conexión nativa; Actualizar, desinstalar o desactivar el plugin cierra sus sesiones activas y comunica el error correspondiente
- `Dependencia` Se agrega common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, bloqueado por hash)
- `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `Dependencia` Añadir compose-ui-api.aar V1 alineado con AutoJs6 6.8.0 (5316) (MPL 2.0, hash fijado), con las dependencias compartidas alineadas con el host
- `Dependencia` Añadir Compose UI Test gestionado por BOM 2026.09.00 (Apache 2.0, solo para pruebas)
- `Dependencia` Añadir JaCoCo versión 0.8.14 (solo cobertura opcional de pruebas, excluido de los paquetes de publicación)

##### Para ver mas historial de versiones, consulte

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

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

Este proyecto se publica bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE). La informacion de licencia de los componentes de terceros figura en [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md).

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentacion de AutoJs6: https://docs.autojs6.com
- Documentacion del modulo compose: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
