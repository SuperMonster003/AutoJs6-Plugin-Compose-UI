<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6 스크립트에 Jetpack Compose와 Material 3 UI 렌더링을 제공하는 플러그인</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Compose-UI?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / 언어

******

이 문서는 다음 언어로 제공됩니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ar.md)

******

### 소개

******

Compose UI는 AutoJs6의 UI 렌더링 플러그인입니다. 스크립트는 호스트가 제공하는 `compose` / `$compose` API로 UI를 선언하고, 플러그인은 호스트 프로세스 안에서 Jetpack Compose와 Material 3로 렌더링합니다. 현재 미리보기는 `"ui";` 모드의 Activity 콘텐츠와 비 UI 스크립트의 플로팅 창을 모두 지원합니다.

플러그인에는 런처에서 열어 모든 구성요소의 미리보기와 예제 스크립트를 볼 수 있는 구성요소 갤러리가 내장되어 있습니다. 스크립트 화면은 여전히 호스트 프로세스에서 렌더링됩니다. 호스트는 INFO 서비스로 플러그인을 찾아 버전과 호환성 정보를 읽고, 계약 (`org.autojs.plugin.compose.api`) 에 따라 자체 프로세스에서 렌더러 팩토리를 생성합니다.

******

### 현재 상태

******

1.1.0 로컬 개발 미리 보기: 호환 AutoJs6 호스트 빌드와 설치 및 활성화된 플러그인이 필요합니다. UI 페이지, 플로팅 창, 예제 5개, API 참조 및 TypeScript 선언을 로컬 연동용으로 제공합니다. 검증된 호환성과 성능 범위는 로드맵에 기록되어 있습니다. 공식 인덱스 등록이나 정식 배포는 하지 않았습니다. 아이콘 그림은 임시이며 관리자의 최종 원본 이미지를 기다리고 있습니다.

******

### 기능

******

현재 개발 미리보기의 핵심 기능:

- 선언형 UI: `compose.state` + `compose.mount(render)`가 상태 변화에 따라 자동으로 다시 그리며, 오래 보유할 수 있는 노드 핸들 (`compose.Text({...})` 등)로 속성과 자식 노드를 직접 수정할 수 있습니다
- Material 3 핵심 구성: 노드 팩터리 29개가 레이아웃, 텍스트, 아이콘, 이미지, 버튼, 입력, 선택, 지연 목록, 대화 상자와 진행 표시를 제공; Snackbar는 세션 명령이며 compose.Snackbar 팩터리가 아닙니다
- 체인형 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')`는 연산 순서를 유지하며, 범위 한정 연산은 호스트 쪽에서 검증됩니다
- 두 가지 표시 위치: `"ui";` 스크립트의 Activity 콘텐츠는 `compose.mount` 또는 호출 가능한 `compose` / `$compose`, 비 UI 스크립트를 포함한 플로팅 창은 `compose.floaty`의 raw 또는 크기 조절 가능한 창 사용
- 호스트 프로세스 안에서 UI 업데이트를 적용하고 이벤트를 소유 스크립트 스레드의 큐로 전달; 작업 스레드는 compose.post로 업데이트를 요청합니다
- 하나의 APK에 arm64-v8a / armeabi-v7a / x86_64 / x86이 포함되며 플러그인 자체 네이티브 코드는 없습니다; AndroidX graphics-path 보조 라이브러리가 포함되고 Android, 호스트 및 플러그인 호환 조건이 적용됩니다
- 네이티브 텍스트 편집은 선택 범위와 IME 조합 상태를 유지하고 포커스 및 명시적 편집을 지원하며 새 입력을 덮어쓰는 지연 편집을 거부; 스위치와 슬라이더 상태는 스크립트가 제어합니다
- 통합 보호: 플러그인이 없거나 호환되지 않으면 가용성 확인이 사용 불가를 반환하고 `ComposeError`로 오류를 보고. 세션 종료나 스크립트 중지 시 소유 창과 콜백 해제
- 카운터, 폼 검증, 안정적인 키를 사용하는 1000개 항목 목록, 비 UI 플로팅 HUD, 테마 예제 5개를 요구 사항과 목록과 함께 제공하며 호환 호스트의 Compose UI 예제 분류에도 동기화
- TSX는 `<compose.Column>`, `<compose:Text>`, 노드 팩토리 참조, Fragment, 슬롯 및 반응형 콜백을 지원합니다. 하나의 트리에서 Compose와 기존 XML 노드를 혼합할 수 없습니다
- XML `<compose>` 컨테이너와 compose.attach로 UI 페이지나 기존 플로팅 창에 독립적인 Compose 세션을 삽입합니다. compose.AndroidView는 기존 Android View 또는 동기 팩토리가 반환하는 View를 표시합니다
- compose.dialog는 UI 또는 일반 스크립트에서 대화상자와 모달 하단 시트를 표시하고 업데이트 및 종료 가능한 세션을 반환합니다
- Material 3 확장 구성요소: 탐색 및 서랍, 탭, 하단 시트 및 메뉴, 날짜와 시간 선택, 페이징 및 그리드, 칩, 배지, 분할 버튼, 플로팅 버튼, 검색창, 도움말 및 당겨서 새로고침
- 구성요소 갤러리: 런처 항목에서 55개 구성요소 전체의 Material 3 미리보기와 실행 가능한 예제 스크립트를 보여주며 복사하거나 AutoJs6로 보낼 수 있습니다. 설정 페이지의 언어, 다크 모드, 테마 색상은 기본적으로 AutoJs6을 따르며 네 가지 런처 아이콘을 제공합니다

******

### 사용 방법

******

1. compose 스크립트 API가 포함된 호환 로컬 AutoJs6 빌드를 설치합니다 (최소 6.8.0 / 5322)
2. 플러그인 APK를 설치하고 런처에서 Compose UI를 열면 구성요소 갤러리와 설정을 볼 수 있습니다
3. AutoJs6 플러그인 센터에서 Compose UI가 인식되고 활성화되었는지 확인합니다
4. 스크립트에서 `compose` 또는 `$compose`를 사용합니다. Activity 콘텐츠는 `compose.mount`, 플로팅 창은 호스트에 오버레이 권한을 부여한 뒤 `compose.floaty`를 사용합니다

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

실행 가능한 스크립트 5개를 assets/examples/index.json에 나열하고 호환 호스트의 Compose UI 예제 분류에도 동기화합니다. 각 머리말은 실행 모드와 권한을 설명합니다. 노드, 수정자 체인, 테마, 세션 및 창의 자세한 내용은 함께 제공되는 로컬 API 문서와 TypeScript/편집기 선언을 참조하세요; 온라인 사이트에는 아직 로컬 변경이 반영되지 않을 수 있습니다.

******

### 호환성

******

플러그인의 실행 요구 사항과 제한:

- 최소 AutoJs6 버전: 6.8.0 (5322) 이상. 더 낮은 버전의 호스트는 플러그인 센터에서 비호환으로 표시합니다
- Android 버전: 7.0 (API 24) 이상
- 프로세서 아키텍처: arm64-v8a / armeabi-v7a / x86_64 / x86 (네 가지 모두 단일 APK에 내장, 아키텍처별 선택 불필요)
- Compose 버전: 플러그인에 포함 (BOM 2026.09.00), 호스트의 Compose 런타임에 의존하지 않습니다
- 계약 버전: 2. 호스트와 플러그인은 계약 버전을 협상하며, 일치하지 않으면 명확한 오류와 함께 로드를 거부합니다
- 패키징된 앱에도 호환 Compose UI 플러그인을 별도로 설치해야 하며 활성화/승인 기록은 해당 앱에 속합니다; 호환성은 내장 AutoJs6 런타임을 검사하며 앱 자체의 versionCode를 사용하지 않습니다
- View 팩토리는 렌더링 전에 메인 스레드에서 실행됩니다. 잘못된 교체는 현재 내용을 보존합니다. 하나의 View를 두 노드가 소유하거나 다른 부모에서 가져올 수 없습니다. 빌린 View의 리스너와 호출자의 리소스 소유권은 유지됩니다
- cancelable=false는 뒤로 가기, 바깥쪽 클릭, 아래로 밀기를 통한 닫기를 막습니다. 명시적 닫기와 스크립트 종료는 대화상자를 정리하고 기존 페이지와 다른 세션을 유지합니다
- 이 빌드는 Compose UI 계약 V2를 사용하며 AutoJs6 6.8.0 / 5322가 필요합니다. TSX에는 TypeScript Engine 0.6.7이 필요합니다. 새 호스트는 기존 V1 렌더러의 구성요소도 지원합니다. 확장 구성요소에는 V2가 필요합니다
- compose.memo는 AutoJs6 6.8.0 / 5323에서 제공되며 의존 값이 바뀌지 않은 render 조각을 재사용합니다. 이 플러그인은 업데이트가 필요 없으며 TSX에는 TypeScript Engine 0.6.8이 필요합니다

******

### 자주 묻는 질문

******

- 갤러리 예제는 어떻게 실행하나요? "AutoJs6에서 실행"을 누르면 설치된 AutoJs6에 스크립트를 전달합니다. 갤러리 자체는 미리보기와 코드만 보여주며 스크립트를 실행하지 않습니다
- `compose`를 찾을 수 없는 이유는 무엇인가요? 전역 객체는 호환되는 로컬 호스트 빌드에서 제공됩니다. 플러그인 APK만 설치하면 추가되지 않습니다
- 다른 UI 플러그인을 제거해야 하나요? 아니요. Compose UI는 기존 `ui` 모듈이나 다른 플러그인에 영향을 주지 않습니다
- 플러그인이 변경되면 어떻게 되나요? 업데이트, 제거 또는 비활성화는 활성 세션을 닫고 해당 오류를 보고합니다; 호환되고 활성화된 플러그인으로 다시 마운트할 수 있습니다
- 플로팅 창에 필요한 조건은 무엇인가요? 호스트에 오버레이 권한을 부여하고 문자 입력 전에 `window.requestFocus()`를 호출하세요. HyperOS에서 창이 보이지 않으면 바탕 화면으로 돌아가세요. 권한이 없으면 PERMISSION_REQUIRED를 반환하며 권한 화면을 자동으로 열지 않습니다
- TSX나 임의의 Compose 함수를 사용할 수 있나요? 일치하는 호스트 및 TypeScript Engine으로 문서의 Compose 노드 팩토리를 TSX에서 사용할 수 있습니다. 임의의 Kotlin Composable 함수나 사용자 정의 TSX 컴포넌트는 지원하지 않습니다
- 회전하면 상태를 잃나요? 현재 호스트는 일반적인 방향 변경을 처리하면서 스크립트 엔진을 유지합니다. 실제 Activity 재생성 또는 제거는 엔진과 세션을 닫으며 업무 상태를 자동 복원하지 않습니다
- 선택기로 구성 요소를 어떻게 찾나요? testTag는 패키지 접두사가 없는 원래 ID로 노출됩니다. id/testTag와 desc/contentDescription은 별개의 정보입니다; Button 텍스트가 자식 노드이면 parent()를 따라 클릭 가능한 상위 노드를 찾으세요

******

### 권한과 보안

******

플러그인은 Android 런타임 권한을 요청하지 않으며 네트워크, 저장소, 센서에 접근하지 않습니다.

- 컴포넌트 보호: Wake Activity와 INFO 서비스는 모두 `org.autojs.permission.PLUGIN` 서명 권한으로 보호되어 AutoJs6 호스트만 접근할 수 있습니다
- 백그라운드 동작: 상주 서비스나 예약 작업이 없습니다. 플러그인이 업데이트될 때 한 번 시스템 브로드캐스트를 받아 런처 아이콘 구성요소를 정리하며, 호스트에 로드되지 않고 갤러리를 열지 않은 동안에는 리소스를 사용하지 않습니다
- 데이터 경계: 스크립트 데이터나 사용자 파일을 읽거나 쓰지 않으며, UI 상태는 호스트 프로세스 메모리에만 존재합니다
- 백업 정책: 앱 백업과 기기 이전이 비활성화되어 있으며, 플러그인 자체는 이전할 데이터를 보유하지 않습니다

호스트는 렌더러를 로드할 때도 자체 스크립트 권한 모델을 유지하며, 플러그인은 스크립트가 접근할 수 있는 시스템 기능을 넓히지 않습니다.

******

### 플러그인 인터페이스

******

호스트에 노출되는 식별자:

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

호스트는 `org.autojs.plugin.INFO`으로 플러그인을 발견하고 `requiresHostVersion` 등의 기능 정보를 읽습니다. 렌더러 팩토리 클래스 이름은 `org.autojs.plugin.compose.RENDERER_FACTORY` 메타데이터로 선언되며, 호스트는 플러그인 APK 경로로 클래스 로더 (부모는 호스트)를 만들어 호스트 프로세스 안에서 인스턴스화합니다.

******

### 로드맵

******

마일스톤, 설계 결정, 수락 기준은 하나의 로드맵에 기록됩니다:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.1.0

_2026/10/08_

- `안내` 1.1.0 로컬 개발 미리 보기: 호환 AutoJs6 호스트 빌드와 설치 및 활성화된 플러그인이 필요합니다. UI 페이지, 플로팅 창, 예제 5개, API 참조 및 TypeScript 선언을 로컬 연동용으로 제공합니다. 검증된 호환성과 성능 범위는 로드맵에 기록되어 있습니다. 공식 인덱스 등록이나 정식 배포는 하지 않았습니다. 아이콘 그림은 임시이며 관리자의 최종 원본 이미지를 기다리고 있습니다
- `안내` 이 빌드는 Compose UI 계약 V2를 사용하며 AutoJs6 6.8.0 / 5322가 필요합니다. TSX에는 TypeScript Engine 0.6.7이 필요합니다. 새 호스트는 기존 V1 렌더러의 구성요소도 지원합니다. 확장 구성요소에는 V2가 필요합니다
- `안내` compose.memo는 AutoJs6 6.8.0 / 5323에서 제공되며 의존 값이 바뀌지 않은 render 조각을 재사용합니다. 이 플러그인은 업데이트가 필요 없으며 TSX에는 TypeScript Engine 0.6.8이 필요합니다
- `안내` 갤러리와 설정 페이지는 플러그인 자체 프로세스에서 실행되며 호스트 내 렌더링 방식과 최소 호스트 버전은 바뀌지 않습니다. 예제 스크립트를 실행하려면 이 플러그인을 설치하고 활성화한 AutoJs6이 필요합니다
- `새 기능` TSX는 `<compose.Column>`, `<compose:Text>`, 노드 팩토리 참조, Fragment, 슬롯 및 반응형 콜백을 지원합니다. 하나의 트리에서 Compose와 기존 XML 노드를 혼합할 수 없습니다
- `새 기능` XML `<compose>` 컨테이너와 compose.attach로 UI 페이지나 기존 플로팅 창에 독립적인 Compose 세션을 삽입합니다. compose.AndroidView는 기존 Android View 또는 동기 팩토리가 반환하는 View를 표시합니다
- `새 기능` compose.dialog는 UI 또는 일반 스크립트에서 대화상자와 모달 하단 시트를 표시하고 업데이트 및 종료 가능한 세션을 반환합니다
- `새 기능` Material 3 확장 구성요소: 탐색 및 서랍, 탭, 하단 시트 및 메뉴, 날짜와 시간 선택, 페이징 및 그리드, 칩, 배지, 분할 버튼, 플로팅 버튼, 검색창, 도움말 및 당겨서 새로고침
- `새 기능` 구성요소 갤러리: 런처에서 열어 55개 구성요소 전체의 Material 3 미리보기와 예제 스크립트를 보고, 클립보드에 복사하거나 설치된 AutoJs6로 보내 실행할 수 있습니다
- `새 기능` 설정 페이지: 언어, 다크 모드, 테마 색상은 기본적으로 AutoJs6을 따르며 따로 설정할 수 있습니다. 런처 아이콘은 적응형 (라이트 / 다크 / 자동) 과 투명 배경의 네 가지 선택을 제공합니다
- `개선` Android 앱 정보 아이콘에 Icon Studio의 그림과 밝은 배경 및 어두운 배경을 사용하고 플러그인 센터의 투명 그림과 기존 런처 옵션을 유지
- `개선` View 팩토리는 렌더링 전에 메인 스레드에서 실행됩니다. 잘못된 교체는 현재 내용을 보존합니다. 하나의 View를 두 노드가 소유하거나 다른 부모에서 가져올 수 없습니다. 빌린 View의 리스너와 호출자의 리소스 소유권은 유지됩니다
- `개선` cancelable=false는 뒤로 가기, 바깥쪽 클릭, 아래로 밀기를 통한 닫기를 막습니다. 명시적 닫기와 스크립트 종료는 대화상자를 정리하고 기존 페이지와 다른 세션을 유지합니다
- `개선` 플러그인 센터 아이콘과 시스템 앱 정보 아이콘은 다른 독립 플러그인과 같은 라이트 #FAFAFA / 다크 #212121 배경을 사용합니다
- `의존성` 동결된 V1을 유지하고 선택적 AndroidView 상호 운용 확장을 포함하도록 compose-ui-api.aar 업데이트
- `의존성` 기존 V1 및 AndroidView 계약을 유지하면서 compose-ui-api.aar에 선택적 대화상자 기능 추가
- `의존성` 기존 노드 모델 및 V1 구성요소 의미를 유지하며 compose-ui-api.aar V2 카탈로그 추가
- `의존성` material-color-utilities 4.1.1 (MIT) 추가. 다른 독립 플러그인과 같은 테마 색상 유도에 사용합니다
- `의존성` AndroidX activity, core, lifecycle, savedstate, emoji2, window, kotlinx-coroutines 등의 호스트 고정 버전 복사본을 추가. 플러그인 자체 프로세스의 갤러리가 사용하며 호스트 내에서는 여전히 호스트 복사본이 우선합니다

#### v1.0.0

_2026/10/03_

- `안내` 1.0.0 로컬 개발 미리 보기: 호환 AutoJs6 호스트 빌드와 설치 및 활성화된 플러그인이 필요합니다. UI 페이지, 플로팅 창, 예제 5개, API 참조 및 TypeScript 선언을 로컬 연동용으로 제공합니다. 검증된 호환성과 성능 범위는 로드맵에 기록되어 있습니다. 공식 인덱스 등록이나 정식 배포는 하지 않았습니다. 아이콘 그림은 임시이며 관리자의 최종 원본 이미지를 기다리고 있습니다
- `안내` AutoJs6 6.8.0 (5316) 이상이 필요합니다
- `안내` 패키징된 앱에도 호환 Compose UI 플러그인을 별도로 설치해야 하며 활성화/승인 기록은 해당 앱에 속합니다; 호환성은 내장 AutoJs6 런타임을 검사하며 앱 자체의 versionCode를 사용하지 않습니다
- `안내` 밝은/어두운 두 mipmap을 유지하며 현재 그림은 임시입니다; 관리자의 최종 흑백 원본 이미지로 교체할 예정입니다
- `새 기능` 호출 가능한 compose / $compose, 유지되는 노드 핸들, 반응형 state/render/ref, 일괄 변경, 큐 게시 및 테마 제어
- `새 기능` Material 3 핵심 구성: 노드 팩터리 29개가 레이아웃, 텍스트, 아이콘, 이미지, 버튼, 입력, 선택, 지연 목록, 대화 상자와 진행 표시를 제공; Snackbar는 세션 명령이며 compose.Snackbar 팩터리가 아닙니다
- `새 기능` UI 스크립트는 Activity 콘텐츠를 마운트하고 compose.floaty는 비 UI 스크립트에도 raw / 크기 조절 창, 픽셀 위치와 크기, 터치/포커스 제어 및 소유 리소스 정리를 제공합니다
- `새 기능` 20가지 Modifier 작업 모두 선언 순서를 유지하며 레이아웃 범위 검사, 스크롤 및 접근성 레이블을 지원
- `새 기능` Material 3 테마는 시드 색상, 밝은 모드와 어두운 모드, Android 12+ 시스템 동적 색상, 글꼴 및 글자 크기 배율을 지원
- `새 기능` 네이티브 텍스트 편집은 선택 범위와 IME 조합 상태를 유지하고 포커스 및 명시적 편집을 지원하며 새 입력을 덮어쓰는 지연 편집을 거부; 스위치와 슬라이더 상태는 스크립트가 제어합니다
- `새 기능` 핵심 아이콘과 ImageWrapper/Bitmap, 로컬 파일 및 호스트 drawable 이미지 지원; 렌더러는 호출자가 소유한 이미지를 자동으로 recycle하지 않습니다
- `새 기능` 카운터, 폼 검증, 안정적인 키를 사용하는 1000개 항목 목록, 비 UI 플로팅 HUD, 테마 예제 5개를 요구 사항과 목록과 함께 제공하며 호환 호스트의 Compose UI 예제 분류에도 동기화
- `새 기능` 함께 제공되는 API 참조 및 TypeScript 선언과 10개 언어의 README, 플러그인 센터 설명 및 변경 기록
- `새 기능` 플러그인 센터 검색과 호스트 버전, 계약 및 승인 검사; 독립 화면이나 런처 항목은 없습니다
- `수정` Compose UI는 렌더링 콜백에서 페이지나 플로팅 창을 다시 마운트하는 작업을 거부하고 현재 페이지를 유지하며, 플러그인 업데이트 후 페이지를 다시 마운트할 수 있도록 지원
- `개선` 가용성 확인과 ComposeError가 플러그인 누락, 비활성화, 미승인, 비호환, 권한 부족 및 닫힌 세션을 일관되게 보고. 네이티브 창 연결 전 취소도 수명 주기 정리에서 처리; 플러그인 업데이트, 제거 또는 비활성화 시 활성 세션을 닫고 해당 오류를 보고
- `의존성` common-plugin-api.aar 버전 6.8.0 (5307) 추가 (MPL 2.0, 해시 고정)
- `의존성` Jetpack Compose BOM 2026.09.00 추가 (Apache 2.0)
- `의존성` AutoJs6 6.8.0 (5316)에 맞춘 compose-ui-api.aar V1 추가 (MPL 2.0, 해시 고정), 공유 의존성을 호스트와 일치시킴
- `의존성` BOM 2026.09.00에서 관리하는 Compose UI Test 추가 (Apache 2.0, 테스트 전용)
- `의존성` JaCoCo 버전 0.8.14 추가 (선택적 테스트 커버리지 전용, 릴리스 패키지에는 포함되지 않음)

##### 더 많은 릴리스 기록은 다음을 참고하세요

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

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

이 프로젝트는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE)에 따라 배포됩니다. 서드파티 구성 요소의 라이선스 정보는 [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md)를 참고하세요.

******

### 관련 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 문서: https://docs.autojs6.com
- compose 모듈 문서: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- 서드파티 고지: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
