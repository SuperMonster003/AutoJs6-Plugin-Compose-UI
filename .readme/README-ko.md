<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-block-compose-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6 스크립트에 Jetpack Compose와 Material 3 UI 렌더링을 제공하는 플러그인</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / 언어

******

이 문서는 다음 언어로 제공됩니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ar.md)

******

### 소개

******

3-Block Compose는 AutoJs6의 UI 렌더링 플러그인입니다. 스크립트는 호스트가 제공하는 `compose` / `$compose` API로 UI를 선언하고, 플러그인은 호스트 프로세스 안에서 Jetpack Compose와 Material 3로 렌더링합니다. 현재 미리보기는 `"ui";` 모드의 Activity 콘텐츠와 비 UI 스크립트의 플로팅 창을 모두 지원합니다.

플러그인에는 비주얼 디자이너와 구성요소 갤러리가 내장되어 있습니다. 런처에서 열면 디자이너 홈으로 들어가 스크립트 프로젝트를 만들거나 가져와 실제 렌더러 위에서 화면을 편집할 수 있고, 갤러리에서는 모든 구성요소의 미리보기와 예제 스크립트를 볼 수 있습니다. 스크립트 화면은 여전히 호스트 프로세스에서 렌더링됩니다. 스크립트 화면은 여전히 호스트 프로세스에서 렌더링됩니다. 호스트는 INFO 서비스로 플러그인을 찾아 버전과 호환성 정보를 읽고, 계약 (`org.autojs.plugin.compose.api`) 에 따라 자체 프로세스에서 렌더러 팩토리를 생성합니다.

******

### 현재 상태

******

1.4.0 은 첫 공개 릴리스입니다 (GitHub Release, 공식 플러그인 인덱스에 등록됨). 3-Block Compose 의 새 식별 정보를 포함한 AutoJs6 6.8.0 (5322) 이상 빌드 (호스트 커밋 f12665ffde 이후) 가 필요합니다. 공개 시점에 AutoJs6 6.8.0 은 아직 정식 출시 전이므로 이 커밋을 포함한 AutoJs6 빌드를 사용해야 합니다. AutoJs6 편집기 메뉴의 "3-Block Compose 디자이너에서 열기" 는 호스트 커밋 db7e6a1734 이후 빌드가 필요하고, 디자이너의 TSX 프로젝트에는 TypeScript Engine 0.7.0 이 필요합니다. 이 플러그인은 UI 페이지, 플로팅 창, 비주얼 디자이너, 예제 5개, API 참조, 디자이너 안내 페이지, TypeScript 선언을 제공합니다. 검증된 호환성과 성능 범위는 로드맵에 기록되어 있습니다. 아이콘은 관리자가 제공한 정식 도안을 사용합니다.

******

### 기능

******

현재 개발 미리보기의 핵심 기능:

- 비주얼 디자이너: 스크립트 파일이 곧 프로젝트이고 실제 렌더러가 편집 중인 UI 트리를 그대로 미리 보여 주며, 개요 / 구성요소 팔레트 / 속성 서랍으로 55개 구성요소를 모두 편집하고 실행 취소 / 다시 실행과 초안 복원을 지원합니다. 내보낸 스크립트는 복사하거나 AutoJs6로 바로 보내 실행할 수 있습니다. 캔버스에 놓으면 포인터 위치에 들어가고, 열린 팝업은 캔버스 안에 그려지며, Image 는 @drawable 과 프로젝트 이미지 폴더의 이미지를 보여 주고, 상태 패널과 조작 미리보기로 state 의 미리보기 값을 잠시 바꿀 수 있습니다. 프로젝트는 JavaScript 또는 TSX 로 만들 수 있으며, TSX 프로젝트는 TypeScript Engine 0.7.0 이상으로 분석하고 정규화된 TSX 로 저장합니다
- AutoJs6 연동: 프로젝트를 실행하도록 보내거나, AutoJs6 편집기에서 열거나 (양방향 동기화), AutoJs6 스크립트 폴더로 가져오거나, 복사하거나 내보낼 수 있고, 디자이너는 AutoJs6 편집기나 파일 관리자에서 .js / .tsx 파일을 바로 열어 같은 파일에 저장합니다. 가져온 프로젝트는 편집 화면 메뉴에서 원본에 다시 쓰거나 원본에서 다시 가져올 수 있습니다
- 선언형 UI: `compose.state` + `compose.mount(render)`가 상태 변화에 따라 자동으로 다시 그리며, 오래 보유할 수 있는 노드 핸들 (`compose.Text({...})` 등)로 속성과 자식 노드를 직접 수정할 수 있습니다
- Material 3 핵심 구성: 노드 팩터리 29개가 레이아웃, 텍스트, 아이콘, 이미지, 버튼, 입력, 선택, 지연 목록, 대화 상자와 진행 표시를 제공; Snackbar는 세션 명령이며 compose.Snackbar 팩터리가 아닙니다
- 체인형 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')`는 연산 순서를 유지하며, 범위 한정 연산은 호스트 쪽에서 검증됩니다
- 두 가지 표시 위치: `"ui";` 스크립트의 Activity 콘텐츠는 `compose.mount` 또는 호출 가능한 `compose` / `$compose`, 비 UI 스크립트를 포함한 플로팅 창은 `compose.floaty`의 raw 또는 크기 조절 가능한 창 사용
- 호스트 프로세스 안에서 UI 업데이트를 적용하고 이벤트를 소유 스크립트 스레드의 큐로 전달; 작업 스레드는 compose.post로 업데이트를 요청합니다
- 하나의 APK에 arm64-v8a / armeabi-v7a / x86_64 / x86이 포함되며 플러그인 자체 네이티브 코드는 없습니다; AndroidX graphics-path 보조 라이브러리가 포함되고 Android, 호스트 및 플러그인 호환 조건이 적용됩니다
- 네이티브 텍스트 편집은 선택 범위와 IME 조합 상태를 유지하고 포커스 및 명시적 편집을 지원하며 새 입력을 덮어쓰는 지연 편집을 거부; 스위치와 슬라이더 상태는 스크립트가 제어합니다
- 통합 보호: 플러그인이 없거나 호환되지 않으면 가용성 확인이 사용 불가를 반환하고 `ComposeError`로 오류를 보고. 세션 종료나 스크립트 중지 시 소유 창과 콜백 해제
- 카운터, 폼 검증, 안정적인 키를 사용하는 1000개 항목 목록, 비 UI 플로팅 HUD, 테마 예제 5개를 요구 사항과 목록과 함께 제공하며 호환 호스트의 3-Block Compose 예제 분류에도 동기화
- TSX는 `<compose.Column>`, `<compose:Text>`, 노드 팩토리 참조, Fragment, 슬롯 및 반응형 콜백을 지원합니다. 하나의 트리에서 Compose와 기존 XML 노드를 혼합할 수 없습니다
- XML `<compose>` 컨테이너와 compose.attach로 UI 페이지나 기존 플로팅 창에 독립적인 Compose 세션을 삽입합니다. compose.AndroidView는 기존 Android View 또는 동기 팩토리가 반환하는 View를 표시합니다
- compose.dialog는 UI 또는 일반 스크립트에서 대화상자와 모달 하단 시트를 표시하고 업데이트 및 종료 가능한 세션을 반환합니다
- Material 3 확장 구성요소: 탐색 및 서랍, 탭, 하단 시트 및 메뉴, 날짜와 시간 선택, 페이징 및 그리드, 칩, 배지, 분할 버튼, 플로팅 버튼, 검색창, 도움말 및 당겨서 새로고침
- 구성요소 갤러리: 런처 항목에서 55개 구성요소 전체의 Material 3 미리보기와 실행 가능한 예제 스크립트를 보여주며 복사하거나 AutoJs6로 보낼 수 있습니다. 설정 페이지의 언어, 다크 모드, 테마 색상은 기본적으로 AutoJs6을 따르며 네 가지 런처 아이콘을 제공합니다

******

### 사용 방법

******

1. compose 스크립트 API가 포함된 호환 로컬 AutoJs6 빌드를 설치합니다 (최소 6.8.0 / 5322)
2. 플러그인 APK를 설치하고 런처에서 3-Block Compose를 열면 디자이너 홈으로 들어갑니다. 여기서 스크립트 프로젝트를 만들거나 상단 바에서 구성요소 갤러리와 설정을 열 수 있습니다
3. AutoJs6 플러그인 센터에서 3-Block Compose가 인식되고 활성화되었는지 확인합니다
4. 스크립트에서 `compose` 또는 `$compose`를 사용합니다. Activity 콘텐츠는 `compose.mount`, 플로팅 창은 호스트에 오버레이 권한을 부여한 뒤 `compose.floaty`를 사용합니다
5. 편집 화면 메뉴에 실행, AutoJs6 편집기에서 열기, AutoJs6 스크립트 폴더로 가져오기, 복사, 내보내기가 있습니다. AutoJs6 편집기 메뉴 (해당 업데이트가 포함된 AutoJs6 필요) 나 파일 관리자에서 3-Block Compose 를 선택하면 스크립트 파일을 바로 편집할 수 있습니다

******

### 빠른 시작

******

아래 카운터와 플로팅 HUD는 호환 로컬 미리보기 호스트에서 실행할 수 있습니다. HUD 실행 전에 호스트에 다른 앱 위에 표시할 권한을 부여하세요:

```js
"ui";

// 카운터 (선언형 render 계층)
let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `${count.value}번 클릭했습니다`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, '하나 더하기'),
]));
```

```js
// 플로팅 HUD (노드 핸들 계층)
let worker = null;
let status = compose.Text({ text: '준비 중...', color: '#FFFFFF' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ contentColor: '#FFFFFF', onClick: () => win.close() }, '닫기'),
]), { x: 50, y: 300, raw: true });
win.on('close', () => { if (worker) worker.interrupt(); });

worker = threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => {
            if (!win.isClosed()) status.text = `진행률 ${i}%`;
        });
    }
});
```

실행 가능한 스크립트 5개를 assets/examples/index.json에 나열하고 호환 호스트의 3-Block Compose 예제 분류에도 동기화합니다. 각 머리말은 실행 모드와 권한을 설명합니다. 노드, 수정자 체인, 테마, 세션 및 창의 자세한 내용은 함께 제공되는 로컬 API 문서와 TypeScript/편집기 선언을 참조하세요; 온라인 사이트에는 아직 로컬 변경이 반영되지 않을 수 있습니다.

******

### 호환성

******

플러그인의 실행 요구 사항과 제한:

- 최소 AutoJs6 버전: 6.8.0 (5322) 이상. 더 낮은 버전의 호스트는 플러그인 센터에서 비호환으로 표시합니다
- Android 버전: 7.0 (API 24) 이상
- 프로세서 아키텍처: arm64-v8a / armeabi-v7a / x86_64 / x86 (네 가지 모두 단일 APK에 내장, 아키텍처별 선택 불필요)
- Compose 버전: 플러그인에 포함 (BOM 2026.09.00), 호스트의 Compose 런타임에 의존하지 않습니다
- 계약 버전: 2. 호스트와 플러그인은 계약 버전을 협상하며, 일치하지 않으면 명확한 오류와 함께 로드를 거부합니다
- 패키징된 앱에도 호환 3-Block Compose 플러그인을 별도로 설치해야 하며 활성화/승인 기록은 해당 앱에 속합니다; 호환성은 내장 AutoJs6 런타임을 검사하며 앱 자체의 versionCode를 사용하지 않습니다
- View 팩토리는 렌더링 전에 메인 스레드에서 실행됩니다. 잘못된 교체는 현재 내용을 보존합니다. 하나의 View를 두 노드가 소유하거나 다른 부모에서 가져올 수 없습니다. 빌린 View의 리스너와 호출자의 리소스 소유권은 유지됩니다
- cancelable=false는 뒤로 가기, 바깥쪽 클릭, 아래로 밀기를 통한 닫기를 막습니다. 명시적 닫기와 스크립트 종료는 대화상자를 정리하고 기존 페이지와 다른 세션을 유지합니다
- 이 빌드는 3-Block Compose 계약 V2를 사용하며 AutoJs6 6.8.0 / 5322가 필요합니다. TSX에는 TypeScript Engine 0.6.7이 필요합니다. 새 호스트는 기존 V1 렌더러의 구성요소도 지원합니다. 확장 구성요소에는 V2가 필요합니다
- compose.memo는 AutoJs6 6.8.0 / 5323에서 제공되며 의존 값이 바뀌지 않은 render 조각을 재사용합니다. 이 플러그인은 업데이트가 필요 없으며 TSX에는 TypeScript Engine 0.6.8이 필요합니다
- 1.2.0 부터 계약 클래스 (org.autojs.plugin.compose.api) 는 독립 프로세스용으로 플러그인에 함께 패키징됩니다. AutoJs6 안에서는 호스트가 같은 클래스를 제공하며 플러그인 사본은 사용되지 않습니다
- 디자이너는 플러그인 자체 프로세스에서 실행되며 AutoJs6 에 의존하지 않습니다. 실행, AutoJs6 편집기에서 열기, AutoJs6 스크립트 폴더로 가져오기에는 설치된 AutoJs6 가 필요하고, 편집기 메뉴의 "3-Block Compose 디자이너에서 열기" 에는 해당 진입점을 포함한 AutoJs6 빌드가 필요합니다
- 디자이너의 TSX 프로젝트에는 TypeScript Engine 0.7.0 이상 (구문 트리 분석 서비스 제공) 이 필요하며, 실행하면 AutoJs6 가 TypeScript Engine 으로 컴파일합니다. 설치되어 있지 않거나 버전이 낮으면 TSX 프로젝트는 읽기 전용 소스로 열리며 보기와 복사만 할 수 있습니다

******

### 자주 묻는 질문

******

- 갤러리 예제는 어떻게 실행하나요? "AutoJs6에서 실행"을 누르면 설치된 AutoJs6에 스크립트를 전달합니다. 갤러리 자체는 미리보기와 코드만 보여주며 스크립트를 실행하지 않습니다
- 디자이너가 아무 스크립트나 열 수 있나요? 최상위 `compose.state` 선언과 유일한 `compose.mount` / `floaty` / `dialog` 호출만 해석하고 나머지 문장은 원본 그대로 유지합니다. 루트가 compose 팩토리가 아니거나, 마운트 호출이 여러 개이거나, 렌더 함수에 return 이 없는 등 대응할 수 없는 구조는 행과 열을 알려 주며 열기를 거부합니다. TSX 프로젝트에서는 `compose.X` / `compose:X` 태그가 편집 가능한 노드가 되고, 다른 태그와 `{}` 식은 원문 그대로 유지하며, XML 태그와 스프레드 속성은 거부합니다
- 디자이너가 스크립트를 실행하나요? 아니요. JavaScript 는 Rhino 파서로, TSX 는 TypeScript Engine 분석 서비스로 해석하고 플러그인 자체 렌더러로 미리 볼 뿐이며, 이벤트 핸들러는 실행되지 않고 state 는 초기값이나 상태 패널에서 설정한 미리보기 값을 보여 줍니다. 조작 미리보기에서는 state 에 바인딩된 컨트롤 (스위치, 슬라이더, 텍스트 필드 등) 이 미리보기 값을 직접 바꾸지만 핸들러와 렌더 함수는 여전히 실행되지 않습니다. 실행은 AutoJs6 에 맡깁니다
- `compose`를 찾을 수 없는 이유는 무엇인가요? 전역 객체는 호환되는 로컬 호스트 빌드에서 제공됩니다. 플러그인 APK만 설치하면 추가되지 않습니다
- 다른 UI 플러그인을 제거해야 하나요? 아니요. 3-Block Compose는 기존 `ui` 모듈이나 다른 플러그인에 영향을 주지 않습니다
- 플러그인이 변경되면 어떻게 되나요? 업데이트, 제거 또는 비활성화는 활성 세션을 닫고 해당 오류를 보고합니다; 호환되고 활성화된 플러그인으로 다시 마운트할 수 있습니다
- 플로팅 창에 필요한 조건은 무엇인가요? 호스트에 오버레이 권한을 부여하고 문자 입력 전에 `window.requestFocus()`를 호출하세요. HyperOS에서 창이 보이지 않으면 바탕 화면으로 돌아가세요. 권한이 없으면 PERMISSION_REQUIRED를 반환하며 권한 화면을 자동으로 열지 않습니다
- TSX나 임의의 Compose 함수를 사용할 수 있나요? 일치하는 호스트 및 TypeScript Engine으로 문서의 Compose 노드 팩토리를 TSX에서 사용할 수 있습니다. 임의의 Kotlin Composable 함수나 사용자 정의 TSX 컴포넌트는 지원하지 않습니다
- 회전하면 상태를 잃나요? 현재 호스트는 일반적인 방향 변경을 처리하면서 스크립트 엔진을 유지합니다. 실제 Activity 재생성 또는 제거는 엔진과 세션을 닫으며 업무 상태를 자동 복원하지 않습니다
- 선택기로 구성 요소를 어떻게 찾나요? testTag는 패키지 접두사가 없는 원래 ID로 노출됩니다. id/testTag와 desc/contentDescription은 별개의 정보입니다; Button 텍스트가 자식 노드이면 parent()를 따라 클릭 가능한 상위 노드를 찾으세요

******

### 권한과 보안

******

플러그인은 Android 런타임 권한을 요청하지 않으며 네트워크, 저장소, 센서에 접근하지 않습니다.

- 구성 요소 보호: Wake Activity 와 INFO 서비스는 서명 권한 `org.autojs.permission.PLUGIN` 으로 보호되어 AutoJs6 호스트만 접근할 수 있습니다. 디자이너 편집 화면은 권한 없는 내보낸 구성 요소지만 사용자가 넘긴 스크립트 문서 Uri 만 받으며 읽기와 쓰기도 그 Uri 로 제한됩니다
- 백그라운드 동작: 상주 서비스나 예약 작업이 없고, 자신이 업데이트될 때 한 번만 시스템 브로드캐스트를 받아 런처 아이콘 구성 요소를 정리합니다. 호스트에 로드되지 않고 디자이너나 갤러리도 열려 있지 않으면 아무것도 소모하지 않습니다
- 데이터 경계: 렌더러는 스크립트 데이터나 사용자 파일을 읽거나 쓰지 않으며 UI 상태는 호스트 프로세스 메모리에만 존재합니다. 디자이너의 프로젝트와 초안은 플러그인 비공개 폴더에 있고, 시스템 문서 선택기로만 가져오거나 내보내며, 사용자가 넘긴 Uri 를 통해 단일 스크립트 파일만 읽고 씁니다. 프로젝트 폴더는 AutoJs6 에 프로젝트를 넘길 때만 플러그인 자체 콘텐츠 제공자를 통해 요청마다 공유됩니다. 가져올 때 선택한 파일의 접근 권한을 유지하지만 (제공자가 허용하는 경우), 사용자가 "원본에 다시 쓰기" 또는 "원본에서 다시 가져오기" 를 선택할 때만 그 파일을 읽고 씁니다
- 백업 정책: 앱 백업과 기기 이전이 비활성화되어 있으며, 플러그인 자체는 이전할 데이터를 보유하지 않습니다

호스트는 렌더러를 로드할 때도 자체 스크립트 권한 모델을 유지하며, 플러그인은 스크립트가 접근할 수 있는 시스템 기능을 넓히지 않습니다.

******

### 플러그인 인터페이스

******

호스트에 노출되는 식별자:

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

호스트는 `org.autojs.plugin.INFO`으로 플러그인을 발견하고 `requiresHostVersion` 등의 기능 정보를 읽습니다. 렌더러 팩토리 클래스 이름은 `org.autojs.plugin.compose.RENDERER_FACTORY` 메타데이터로 선언되며, 호스트는 플러그인 APK 경로로 클래스 로더 (부모는 호스트)를 만들어 호스트 프로세스 안에서 인스턴스화합니다.

******

### 로드맵

******

마일스톤, 설계 결정, 수락 기준은 하나의 로드맵에 기록됩니다:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.4.0

_2026/10/13_

- `안내` 디자이너의 TSX 프로젝트에는 TypeScript Engine 0.7.0 이상이 필요합니다. 설치되어 있지 않거나 버전이 낮으면 TSX 프로젝트는 읽기 전용 소스로 보기와 복사만 할 수 있습니다
- `새 기능` 디자이너가 TSX 프로젝트를 지원합니다. 새 프로젝트를 JavaScript 또는 TSX 로 만들 수 있고, .tsx 파일을 가져오거나 AutoJs6 편집기와 파일 관리자에서 바로 열 수 있습니다. TSX 소스는 TypeScript Engine 으로 분석한 뒤 캔버스에서 편집하며, 일관된 형식의 TSX (4칸 들여쓰기, 작은따옴표 속성, 자식 요소마다 한 줄) 로 저장합니다. 디자이너가 편집할 수 없는 작성 방식 (별칭 태그, 조건이나 반복으로 만든 자식 요소 등) 은 원문 그대로 유지하고, XML 태그와 스프레드 속성처럼 표현할 수 없는 작성 방식은 행과 열을 알려 주며 열기를 거부합니다
- `새 기능` 디자이너에서 TSX 프로젝트를 "실행" 하면 프로젝트 파일을 AutoJs6 에 넘기고, TypeScript Engine 으로 컴파일한 뒤 실행합니다. 저장하지 않은 변경은 먼저 저장합니다
- `새 기능` TypeScript Engine 이 설치되어 있지 않거나, 버전이 낮거나, 분석 서비스를 잠시 사용할 수 없으면 디자이너는 TSX 프로젝트의 소스를 이유와 함께 읽기 전용으로 보여 주며, 소스를 복사하거나 다시 시도할 수 있습니다. TypeScript Engine 을 설치하거나 업데이트한 뒤 디자이너로 돌아오면 자동으로 다시 시도합니다
- `수정` 다시 쓸 수 없는 외부 스크립트를 저장할 때 파일 이름에 공백이나 괄호 등 프로젝트 이름에 쓸 수 없는 문자가 있으면 프로젝트로 저장되지 않고 디자이너가 강제 종료되던 문제. 이제 이런 문자는 "-" 로 바꿔 저장하며, 가져온 스크립트도 같은 규칙으로 이름을 정합니다
- `개선` TSX 프로젝트에서 편집한 이벤트 핸들러는 TypeScript Engine 으로 구문을 검사하므로 타입 주석 등 TypeScript 작성 방식을 사용할 수 있습니다
- `의존성` 계약 아티팩트 typescript-parser-api.aar (TypeScript 구문 트리 분석 계약 v1) 를 추가했습니다. 디자이너가 TSX 프로젝트를 읽는 데 사용합니다

#### v1.3.0

_2026/10/13_

- `새 기능` 디자이너에서 "스크립트 가져오기" 로 만든 프로젝트는 편집 화면 메뉴의 "원본에 다시 쓰기" 와 "원본에서 다시 가져오기" 를 사용할 수 있습니다. 가져올 때 원본 파일 접근 권한을 유지하고 (제공자가 허용하는 경우), 마지막 동기화 이후 다른 쪽이 변경되었으면 먼저 확인하며, 원본 파일을 열 수 없게 되면 다시 선택해 연결을 복원할 수 있습니다. 프로젝트를 저장해도 원본에 자동으로 쓰지 않습니다
- `새 기능` 디자이너 캔버스에서 Image 의 그림을 미리 볼 수 있습니다. '@drawable/name' 은 설치된 AutoJs6 에서 가져오고, 상대 경로는 편집 화면 메뉴의 "이미지 폴더" 로 고른 폴더에서 찾습니다 (읽기 전용 접근 권한만 유지). URL 과 절대 경로는 계속 자리 표시로 남으며 이유는 문제 목록에 표시됩니다
- `새 기능` 디자이너 편집 화면에 "상태" 패널이 추가되었습니다. 스크립트 최상위의 compose.state 와 초기값을 나열하고, 캔버스에만 반영되는 미리보기 값을 임시로 설정할 수 있습니다 (스크립트에 기록되지 않고 실행 취소 단계도 생기지 않습니다). 캔버스 오른쪽 위의 "조작 미리보기" 를 켜면 캔버스의 컴포넌트를 직접 조작할 수 있으며, 상태에 바인딩된 스위치, 슬라이더, 텍스트 필드, 선택 항목, 페이저 등의 조작이 해당 미리보기 값을 바꿉니다 (스크립트의 이벤트 핸들러는 실행되지 않습니다). 끄면 선택과 끌기로 돌아갑니다
- `수정` 컨테이너 앞부분의 자식 노드가 삽입, 삭제되거나 다른 곳으로 이동하면 그 뒤의 자식 노드가 모두 새로 만들어져 자신의 상태 (예: LazyColumn 의 스크롤 위치) 를 잃던 문제를 수정했습니다. 이제 해당 자식 노드는 상태를 유지하며 다시 만들어지지 않습니다
- `수정` 디자이너 속성 서랍에서 텍스트 필드 (TextField / OutlinedTextField) 의 text 를 바꿔도 캔버스의 필드에 이전 텍스트가 계속 표시되던 문제를 수정했습니다
- `개선` 디자이너 캔버스에서 노드를 길게 눌러 끌면 대상 컨테이너의 배치 방향에 맞춰 삽입선을 표시하고 (세로, 가로, 그리드), 손을 뗀 위치에 노드를 놓습니다. 이전에는 항상 컨테이너 끝에 추가되었습니다
- `개선` 디자이너 캔버스의 AndroidView 는 크기 수식자 (h, fillMaxWidth 등) 를 유지한 옅은 색 자리 표시 영역으로 표시되며, 그 안에 view 식의 소스를 보여 줍니다
- `개선` 디자이너 속성 서랍의 수식자 체인은 각 항목 왼쪽의 핸들을 끌어 순서를 바꿀 수 있으며, 손을 떼기 전까지 삽입선이 표시됩니다. 위로 / 아래로 이동 버튼도 그대로 남아 있습니다
- `개선` 디자이너 캔버스에서 열린 상태 (open 이 true) 인 팝업 컴포넌트는 이제 실제와 가까운 모습으로 캔버스의 논리 화면 안에 그려집니다. AlertDialog 와 ModalBottomSheet 는 스크림과 함께 페이지 위를 덮고, DropdownMenu 와 Tooltip 은 앵커 아래 또는 위에 표시되며, 안의 노드를 탭하여 편집할 수 있습니다. 더 이상 카드로 페이지에 끼워 넣지 않고 실제 창도 열리지 않습니다
- `개선` 디자이너 캔버스는 편집할 때마다 변경된 노드만 다시 구성하고 노드 위치 기록 비용도 줄여, 큰 문서 (약 250 개 노드) 에서 실행 취소 / 다시 실행 후 화면이 눈에 띄게 빨리 갱신됩니다

#### v1.2.0

_2026/10/13_

- `안내` 플러그인 이름이 Compose UI 에서 3-Block Compose 로 바뀌었습니다: 애플리케이션 ID 가 io.github.supermonster003.autojs6.plugin.compose.ui 에서 io.github.supermonster003.autojs6.plugin.three.block.compose 로 변경됩니다. Android 는 이를 새 앱으로 취급하며 이전 패키지는 함께 남겨 둘 수 있습니다. 설정과 권한은 이전되지 않으므로 새 패키지를 플러그인 센터에서 다시 활성화하고 권한을 부여해야 합니다
- `안내` 새 플러그인 식별 정보를 포함한 AutoJs6 6.8.0 호스트 빌드 (호스트 커밋 f12665ffde 이후) 가 필요합니다. 그 이전 호스트는 이전 패키지 이름으로 플러그인을 찾으므로 이 버전을 인식하지 못합니다
- `안내` 디자이너 사용법은 문서 사이트의 "3-Block Compose 디자이너" 페이지를 참고하세요. AutoJs6 편집기 메뉴의 "3-Block Compose 디자이너에서 열기" 에는 호스트 커밋 db7e6a1734 이후의 AutoJs6 6.8.0 빌드가 필요하고, 다른 대상은 설치된 AutoJs6 만 있으면 됩니다
- `새 기능` 비주얼 디자이너: 런처에서 디자이너 홈을 열어 비공개 저장소의 스크립트 프로젝트를 만들거나 가져오거나 열 수 있습니다. 편집 화면은 실제 렌더러 미리보기 캔버스, 개요 트리, 구성요소 팔레트, 속성 서랍을 제공하며 탭으로 선택, 길게 눌러 끌어 부모 변경, 개요 안에서 순서 변경, 실행 취소 / 다시 실행, 초안 자동 저장과 복원, 표시 옵션 편집, 계약 카탈로그 기반 검증 보고서, 스크립트 복사 또는 AutoJs6로 보내기를 지원합니다. 가로 화면과 태블릿에서는 캔버스와 패널이 좌우로 배치됩니다. 구성요소 갤러리와 설정은 이제 디자이너 홈에서 엽니다
- `새 기능` 디자이너와 AutoJs6 연동: 편집 화면 메뉴에서 현재 스크립트를 AutoJs6 편집기에서 열고 (AutoJs6 가 저장하면 디자이너가 다시 불러옵니다), AutoJs6 스크립트 폴더로 가져오거나, 원하는 위치로 내보낼 수 있습니다. 디자이너는 AutoJs6 편집기나 파일 관리자에서 .js 파일을 바로 열어 같은 파일에 저장하며, 쓰기 권한이 없으면 새 프로젝트로 저장합니다. 프로젝트 파일은 플러그인 자체 콘텐츠 제공자를 통해 요청마다 권한을 부여해 공유하고 다른 폴더는 공개하지 않습니다
- `개선` 앱 제목, 플러그인 ID (three-block-compose), 패키지 및 구성 요소 이름, 배포 산출물, 문서, GitHub 저장소 (AutoJs6-Plugin-Three-Block-Compose) 가 이름 변경에 맞춰 갱신되었습니다. 스크립트 API compose, 계약 버전, 렌더링 동작은 그대로입니다
- `개선` 런처, 플러그인 센터, 시스템 앱 정보 아이콘이 관리자가 제공한 정식 그림으로 바뀌었으며 밝은 그림과 어두운 그림은 같은 모양을 공유합니다
- `개선` 번들 예제와 호스트 예제 분류가 3-Block Compose 로 바뀌었습니다
- `개선` 계약 AAR compose-ui-api 를 플러그인에 함께 패키징하고 그대로 유지하도록 바꿨습니다 (이전에는 컴파일 시에만 참조). 플러그인 자체 프로세스를 위한 것이며 AutoJs6 안에서는 호스트의 동일한 계약 클래스가 계속 우선되므로 렌더링 동작과 계약 버전은 그대로입니다
- `의존성` Rhino 파서 경량 jar (org.mozilla.javascript 2.0.0-SNAPSHOT, MPL 2.0, AutoJs6 호스트와 같은 빌드) 를 추가했습니다. 비주얼 디자이너가 스크립트를 파싱하는 데만 쓰이며 실행하지 않습니다. release 빌드에는 참조되는 파서 클래스만 남습니다

##### 더 많은 릴리스 기록은 다음을 참고하세요

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드

******

저장소를 복제한 뒤 Gradle Wrapper로 바로 빌드할 수 있습니다. 필요한 Android Gradle Plugin과 Kotlin 버전은 플랫폼 버전 플러그인이 현재 IDE 환경에 맞춰 자동으로 선택합니다.

디버그 APK 빌드:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM 단위 테스트 실행 및 기기 계약 테스트 패키징:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

릴리스 APK 빌드 (`sign.properties`와 서명 키 필요):

```powershell
.\gradlew.bat :app:assembleRelease
```

서명을 검증하고 다이제스트 접미사가 붙은 릴리스 파일 생성:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

다국어 문서가 원본과 일치하는지 검증:

```powershell
py .python\generate_markdown.py --check
```

빌드에는 JDK 21 이상이 필요합니다. `.readme` 또는 `.changelog` 아래의 원본을 수정한 뒤에는 `py .python\generate_markdown.py`를 실행하여 모든 문서를 다시 생성하세요.

******

### 문서 구성

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

README, 플러그인 센터 설명, 변경 로그는 모두 `.readme`와 `.changelog` 아래의 JSON 원본에서 생성됩니다. 생성된 Markdown 파일을 직접 편집하지 마세요.

******

### 라이선스

******

이 프로젝트는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE)에 따라 배포됩니다. 서드파티 구성 요소의 라이선스 정보는 [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md)를 참고하세요.

******

### 관련 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 문서: https://docs.autojs6.com
- compose 모듈 문서: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- 서드파티 고지: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md
