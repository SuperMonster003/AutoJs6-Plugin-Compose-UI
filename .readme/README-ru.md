<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Плагин, который приносит интерфейсы Jetpack Compose и Material 3 в скрипты AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Compose-UI?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / Языки

******

Этот документ доступен на следующих языках:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ar.md)

******

### Введение

******

Compose UI - плагин отрисовки интерфейса для AutoJs6. Скрипты объявляют интерфейс через встроенный в хост глобальный объект `compose`, а плагин отрисовывает его внутри процесса хоста средствами Jetpack Compose и Material 3, предоставляя единое декларативное решение для содержимого активностей в режиме `"ui";` и для плавающих окон.

Плагин не содержит отдельных экранов и не добавляет ярлык в лаунчер. Хост обнаруживает его через сервис INFO, читает версию и сведения о совместимости, затем загружает рендерер внутри своего процесса согласно контракту (`org.autojs.plugin.compose.api`). Дерево интерфейса, состояние и события описываются на стороне скрипта; рендерер лишь применяет патчи к композиции Compose и возвращает события пользователя скрипту.

******

### Текущее состояние

******

Предварительная версия P0: отдельный тестовый хост может отрисовать счетчик Column / Text / Button. API сценариев compose пока недоступен.

******

### Возможности

******

Ключевые возможности, которые плагин должен предоставить:

- Декларативный интерфейс: `compose.state` + `compose.mount(render)` автоматически перерисовывают интерфейс при изменении состояния, а долгоживущие дескрипторы узлов (`compose.Text({...})` и подобные) позволяют напрямую менять свойства и дочерние узлы
- Базовый набор компонентов Material 3: контейнеры (Column / Row / Box / LazyColumn и другие), текст, кнопки, поля ввода, переключатели, ползунки, индикаторы прогресса, карточки, диалоги
- Цепочки Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` сохраняет порядок операций, а операции с ограниченной областью проверяются на стороне хоста
- Две поверхности отображения: содержимое активности скриптов `"ui";` (`compose.mount`) и плавающие окна любого скрипта (`compose.floaty`)
- Отрисовка внутри процесса: рендерер работает в процессе хоста без межпроцессного моста интерфейса, поэтому события и обновления состояния доставляются с малой задержкой
- Единый пакет: без вариантов ABI и без собственного нативного кода (только вспомогательная библиотека AndroidX graphics-path, поставляемая с Compose, встроена для всех четырёх ABI), один APK для всех устройств

******

### Использование

******

1. Установите AutoJs6 6.8.0 (5308) или новее
2. Установите APK этого плагина (открывать нечего, у плагина нет ярлыка в лаунчере)
3. Убедитесь в центре плагинов AutoJs6, что Compose UI распознан и включён
4. Используйте глобальный объект `compose` прямо в скриптах (отрисовка появится в версии 1.0.0)

******

### Быстрый старт

******

Примеры ниже показывают целевую форму API (определена в приложении A дорожной карты; до поставки отрисовки не выполняются):

```js
"ui";

// Счётчик (декларативный слой render)
let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `Нажато ${count.value} раз`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, 'Прибавить один'),
]));
```

```js
// Плавающий HUD (слой дескрипторов узлов)
let status = compose.Text({ text: 'Подготовка...' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ onClick: () => win.close() }, 'Закрыть'),
]), { x: 50, y: 300, raw: true });

threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => status.set({ text: `Прогресс ${i}%` }));
    }
});
```

Полный справочник API (каталог компонентов, операции Modifier, объекты сессии, коды ошибок) находится в главе о модуле compose документации AutoJs6.

******

### Совместимость

******

Требования к среде выполнения и ограничения плагина:

- Версия AutoJs6: 6.8.0 (5308) или новее; более старые хосты помечают плагин как несовместимый в центре плагинов
- Версия Android: 7.0 (API 24) или новее
- Архитектура процессора: arm64-v8a / armeabi-v7a / x86_64 / x86 (все четыре встроены в единый APK, выбирать сборку по архитектуре не нужно)
- Версия Compose: поставляется вместе с плагином (BOM 2026.09.00), не зависит от среды Compose хоста
- Версия контракта: 1; хост и плагин согласовывают версию контракта и при несовпадении отказывают в загрузке с понятной ошибкой

******

### Частые вопросы

******

- Почему после установки нет значка плагина? У плагина нет собственного интерфейса и ярлыка в лаунчере; найдите его в центре плагинов AutoJs6
- Почему `compose` в скриптах ещё не работает? Это предварительная сборка этапа P0; рендерер и скриптовый API появятся на следующих этапах
- Нужно ли удалять другие плагины интерфейса? Нет, Compose UI не мешает существующему модулю `ui` и другим плагинам
- Нужно ли менять скрипты после обновления плагина? Нет, пока версия контракта не меняется; обновления контракта явно отмечаются в журнале изменений

******

### Разрешения и безопасность

******

Плагин не запрашивает разрешений времени выполнения Android и не обращается к сети, хранилищу или датчикам.

- Защита компонентов: Wake Activity и сервис INFO защищены разрешением с проверкой подписи `org.autojs.permission.PLUGIN`, поэтому доступ к ним имеет только хост AutoJs6
- Нет фоновой активности: у плагина нет постоянных сервисов, приёмников широковещательных сообщений и запланированных задач, и он не потребляет ресурсы, пока хост его не загружает
- Граница данных: плагин не читает и не записывает данные скриптов и файлы пользователя; состояние интерфейса существует только в памяти процесса хоста
- Политика резервного копирования: резервное копирование приложения и перенос между устройствами отключены, и плагин не хранит данных, которые требовалось бы переносить

При загрузке рендерера хост сохраняет собственную модель разрешений для скриптов; плагин не расширяет системные возможности, доступные скриптам.

******

### Интерфейс плагина

******

Идентификаторы, доступные хосту:

```text
application id: io.github.supermonster003.autojs6.plugin.compose.ui
plugin id: compose-ui
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: compose-ui
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 1)
minimum host build: 5308 (6.8.0)
```

Хост обнаруживает плагин через `org.autojs.plugin.INFO` и читает сведения о возможностях, такие как `requiresHostVersion`; класс фабрики рендерера объявляется метаданными `org.autojs.plugin.compose.RENDERER_FACTORY`, а хост создаёт загрузчик классов из пути к APK плагина (с хостом в качестве родителя) и создаёт экземпляр внутри своего процесса.

******

### Дорожная карта

******

Этапы, проектные решения и критерии приёмки ведутся в единой дорожной карте:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### История версий

******

#### v1.0.0

_2026/10/02_

- `Подсказка` Предварительная версия P0: отдельный тестовый хост может отрисовать счетчик Column / Text / Button. API сценариев compose пока недоступен.
- `Подсказка` Требуется AutoJs6 6.8.0 (5308) или новее (точный минимальный номер сборки будет уточнён после внесения изменений на стороне хоста)
- `Новое` Каркас репозитория плагина: сборочная цепочка плагина версий платформы, зависимости Jetpack Compose BOM 2026.09.00, протокол активации Wake Activity и сервис INFO (категория compose-ui)
- `Новое` README, описание для центра плагинов и журнал изменений на 10 языках, генерируемые из JSON-исходников
- `Новое` Предварительная версия P0: отдельный тестовый хост может отрисовать счетчик Column / Text / Button. API сценариев compose пока недоступен
- `Зависимость` Добавлен common-plugin-api.aar версии 6.8.0 (5307) (MPL 2.0, зафиксирован по хешу)
- `Зависимость` Добавлен Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `Зависимость` Добавлен черновик P0 compose-ui-api.aar (MPL 2.0, фиксированная хеш-сумма), общие зависимости согласованы с хостом

##### Подробную историю версий смотрите в

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка

******

После клонирования собирайте напрямую через Gradle Wrapper; версии Android Gradle Plugin и Kotlin автоматически выбирает плагин версий платформы в соответствии с текущим окружением IDE.

Собрать отладочный APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Запустить модульные тесты JVM и упаковать контрактные тесты для устройства:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Собрать релизный APK (требуются `sign.properties` и ключ подписи):

```powershell
.\gradlew.bat :app:assembleRelease
```

Проверить подпись и получить релизный файл с суффиксом контрольной суммы:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Проверить, что локализованные документы соответствуют исходникам:

```powershell
py .python\generate_markdown.py --check
```

Для сборки требуется JDK 21 или новее. После правки исходников в `.readme` или `.changelog` выполните `py .python\generate_markdown.py`, чтобы заново сгенерировать все документы.

******

### Структура документации

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

README, описание для центра плагинов и журнал изменений генерируются из JSON-исходников в `.readme` и `.changelog`; не редактируйте сгенерированные файлы Markdown напрямую.

******

### Лицензия

******

Проект распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE). Сведения о лицензиях сторонних компонентов приведены в [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md).

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Документация AutoJs6: https://docs.autojs6.com
- Документация модуля compose: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- Уведомления о сторонних компонентах: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
