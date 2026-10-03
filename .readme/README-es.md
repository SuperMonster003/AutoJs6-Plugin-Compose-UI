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

Compose UI es un plugin de renderizado de interfaces para AutoJs6. Los scripts declaran interfaces mediante la entrada `compose` / `$compose` del host, y el plugin las renderiza dentro del proceso del host con Jetpack Compose y Material 3. La vista previa actual admite contenido de actividades `"ui";`; las ventanas flotantes están previstas para P3.4.

El plugin no incluye pantallas independientes ni agrega una entrada en el lanzador. El host lo descubre mediante el servicio INFO, lee su version y sus datos de compatibilidad y luego carga el renderizador dentro del proceso del host segun el contrato (`org.autojs.plugin.compose.api`). El arbol de interfaz, el estado y los eventos viven en el lado del script; el renderizador solo aplica parches a la composicion de Compose y devuelve los eventos del usuario al script.

******

### Estado actual

******

Vista previa de desarrollo P3: la entrada invocable compose / $compose, 29 fábricas de nodos, manejadores persistentes, state/render/ref reactivos, batch/post/theme y el montaje en scripts UI funcionan con una compilación local compatible del host AutoJs6. Las ventanas flotantes siguen previstas para P3.4. Los ejemplos incluidos, la documentación completa de la API, las declaraciones de tipos y la matriz ampliada de verificación siguen pendientes. Es una vista previa local sin publicación oficial.

******

### Caracteristicas

******

Funciones de la vista previa actual y soporte de alojamiento previsto:

- Interfaz declarativa: `compose.state` + `compose.mount(render)` vuelven a dibujar automaticamente ante cada cambio de estado, mientras que los manejadores de nodo de larga vida (`compose.Text({...})` y similares) permiten modificar directamente propiedades e hijos
- Conjunto basico de componentes Material 3: diseños (Column / Row / Box / LazyColumn, etc.), texto, botones, campos de texto, interruptores, deslizadores, indicadores de progreso, tarjetas, dialogos
- Modifiers encadenados: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` conserva el orden de las operaciones, y las operaciones con ambito se validan en el lado del host
- Alojamiento: el contenido de actividades de scripts `"ui";` se monta con `compose.mount` o con `compose` / `$compose` invocables; las ventanas flotantes (`compose.floaty`) están previstas para P3.4
- Renderizado dentro del proceso: el renderizador se ejecuta en el proceso del host sin ningun puente de interfaz entre procesos, con eventos y actualizaciones de estado de baja latencia
- Paquete unico: sin variantes de ABI ni codigo nativo propio (solo el auxiliar AndroidX graphics-path incluido con Compose, integrado para las cuatro ABI), un solo APK para todos los dispositivos
- API de scripts: `compose` / `$compose`, 29 fábricas de nodos y manejadores persistentes, además de `compose.ref`, `compose.batch`, `compose.post` y `compose.theme`

******

### Uso

******

1. Instale una compilación local compatible de AutoJs6 que incluya la entrada compose (mínimo 6.8.0 / 5316)
2. Instale el APK de este plugin (no hay nada que abrir, el plugin no tiene entrada en el lanzador)
3. Confirme en el centro de plugins de AutoJs6 que Compose UI se reconoce y esta habilitado
4. Use `compose` o `$compose` en los scripts; monte el contenido de la actividad desde un script `"ui";`

******

### Inicio rapido

******

El contador siguiente puede ejecutarse con el host local de vista previa compatible. El HUD flotante muestra la API objetivo de P3.4 y todavía no se puede ejecutar:

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
let status = compose.Text({ text: 'Preparando...' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ onClick: () => win.close() }, 'Cerrar'),
]), { x: 50, y: 300, raw: true });

threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => status.set({ text: `Progreso ${i}%` }));
    }
});
```

Los ejemplos incluidos, la referencia completa de la API y las declaraciones TypeScript siguen pendientes. El apéndice A de la hoja de ruta define la forma actual de la API.

******

### Compatibilidad

******

Requisitos de ejecucion y limites del plugin:

- Version minima de AutoJs6: 6.8.0 (5316) o posterior; los hosts anteriores marcan el plugin como incompatible en el centro de plugins
- Version de Android: 7.0 (API 24) o posterior
- Arquitectura del procesador: arm64-v8a / armeabi-v7a / x86_64 / x86 (las cuatro integradas en el unico APK, sin elegir por arquitectura)
- Version de Compose: incluida en el plugin (BOM 2026.09.00), independiente del runtime de Compose del host
- Version del contrato: 1; el host y el plugin negocian la version del contrato y rechazan la carga con un error claro cuando no coincide

******

### Preguntas frecuentes

******

- Por que no aparece el icono del plugin tras instalarlo? El plugin no tiene interfaz propia ni entrada en el lanzador; busquelo en el centro de plugins de AutoJs6
- Por qué falta `compose`? El objeto global lo proporciona la compilación local compatible del host; instalar solo el APK del plugin no lo añade
- Hay que desinstalar otros plugins de interfaz? No, Compose UI no interfiere con el modulo `ui` existente ni con otros plugins
- Hay que modificar los scripts tras actualizar el plugin? No mientras la version del contrato se mantenga; las actualizaciones del contrato se indican explicitamente en el registro de cambios

******

### Permisos y seguridad

******

El plugin no solicita ningun permiso de tiempo de ejecucion de Android y nunca accede a la red, al almacenamiento ni a los sensores.

- Proteccion de componentes: tanto la Wake Activity como el servicio INFO estan protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo el host AutoJs6 puede acceder a ellos
- Sin actividad en segundo plano: el plugin no tiene servicios residentes, receptores de difusion ni tareas programadas, y no consume recursos mientras el host no lo carga
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
contract package: org.autojs.plugin.compose.api (version 1)
minimum host build: 5316 (6.8.0)
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

#### v1.0.0

_2026/10/03_

- `Aviso` Vista previa de desarrollo P3: la entrada invocable compose / $compose, 29 fábricas de nodos, manejadores persistentes, state/render/ref reactivos, batch/post/theme y el montaje en scripts UI funcionan con una compilación local compatible del host AutoJs6. Las ventanas flotantes siguen previstas para P3.4. Los ejemplos incluidos, la documentación completa de la API, las declaraciones de tipos y la matriz ampliada de verificación siguen pendientes. Es una vista previa local sin publicación oficial
- `Aviso` Requiere AutoJs6 6.8.0 (5316) o posterior
- `Novedad` Esqueleto del repositorio del plugin: cadena de compilacion del plugin de versiones de plataforma, dependencias de Jetpack Compose BOM 2026.09.00, protocolo de activacion Wake Activity y servicio INFO (categoria compose-ui)
- `Novedad` README, instruccion del centro de plugins y registro de cambios en 10 idiomas, generados a partir de fuentes JSON
- `Novedad` La vista previa admite diseños, texto, iconos, imágenes, botones, controles de selección y deslizadores; los mapas de bits proporcionados siguen siendo propiedad del llamador, que gestiona su liberación
- `Novedad` Las 20 operaciones Modifier conservan el orden declarado y admiten validación del ámbito de diseño, desplazamiento y etiquetas de accesibilidad
- `Novedad` Los temas Material 3 admiten colores semilla, modos claro y oscuro, colores dinámicos del sistema en Android 12+, familias tipográficas y escala de texto
- `Novedad` Las actualizaciones de interfaz son atómicas y, si se rechazan, conservan la última vista válida; las entradas controladas notifican cambios mediante callbacks en cola, que se liberan al cerrar
- `Novedad` Los campos de texto de la vista previa conservan la selección y la composición del IME, admiten el foco y la edición explícita, y rechazan ediciones retrasadas que sobrescribirían entradas más recientes
- `Novedad` La vista previa incorpora listas de carga diferida con claves de elemento estables y desplazamiento por índice, ranuras de Scaffold y barra superior, diálogos controlados, indicadores de progreso y callbacks en cola de acción o cierre de Snackbar
- `Novedad` La vista previa de scripts ofrece compose / $compose invocables, 29 fábricas de nodos, manejadores persistentes, state/render/ref reactivos, lotes, programación y control de temas
- `Novedad` Los scripts UI pueden montar contenido Compose; sustituir el montaje o detener el script libera la sesión anterior y sus callbacks. El alojamiento flotante sigue previsto para P3.4
- `Dependencia` Se agrega common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, bloqueado por hash)
- `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `Dependencia` Añadir compose-ui-api.aar V1 alineado con AutoJs6 6.8.0 (5316) (MPL 2.0, hash fijado), con las dependencias compartidas alineadas con el host
- `Dependencia` Añadir Compose UI Test gestionado por BOM 2026.09.00 (Apache 2.0, solo para pruebas)

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
