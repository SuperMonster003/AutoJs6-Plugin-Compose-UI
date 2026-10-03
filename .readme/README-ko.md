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

플러그인은 독립적인 화면이 없으며 런처에도 표시되지 않습니다. 호스트는 INFO 서비스로 플러그인을 발견하고 버전과 호환성 정보를 읽은 뒤, 계약 (`org.autojs.plugin.compose.api`)에 따라 호스트 프로세스 안에 렌더러를 로드합니다. UI 트리, 상태, 이벤트는 스크립트 쪽에서 기술되며, 렌더러는 Compose 컴포지션에 패치를 적용하고 사용자 이벤트를 스크립트로 되돌려 보내는 역할만 합니다.

******

### 현재 상태

******

P3 개발 미리보기: 호출 가능한 compose / $compose, 노드 팩터리 29개, 유지 가능한 핸들, 반응형 state/render/ref, batch/post/theme, UI 스크립트 마운트와 raw 또는 크기 조절 가능한 플로팅 창을 호환 로컬 AutoJs6 호스트 빌드에서 사용할 수 있습니다. 가용성 확인, 형식화된 오류 및 세션 정리도 포함됩니다. 예제 모음, 전체 API 문서, 타입 선언 및 더 넓은 검증 매트릭스는 아직 제공되지 않습니다. 현재는 공식 릴리스가 아닌 로컬 미리보기입니다.

******

### 기능

******

현재 개발 미리보기의 핵심 기능:

- 선언형 UI: `compose.state` + `compose.mount(render)`가 상태 변화에 따라 자동으로 다시 그리며, 오래 보유할 수 있는 노드 핸들 (`compose.Text({...})` 등)로 속성과 자식 노드를 직접 수정할 수 있습니다
- Material 3 컴포넌트 핵심 세트: 레이아웃 (Column / Row / Box / LazyColumn 등), 텍스트, 버튼, 텍스트 필드, 스위치, 슬라이더, 진행 표시기, 카드, 대화상자
- 체인형 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')`는 연산 순서를 유지하며, 범위 한정 연산은 호스트 쪽에서 검증됩니다
- 두 가지 표시 위치: `"ui";` 스크립트의 Activity 콘텐츠는 `compose.mount` 또는 호출 가능한 `compose` / `$compose`, 비 UI 스크립트를 포함한 플로팅 창은 `compose.floaty`의 raw 또는 크기 조절 가능한 창 사용
- 프로세스 내 렌더링: 렌더러가 호스트 프로세스 안에서 실행되고 프로세스 간 UI 브리지가 없어 이벤트와 상태 갱신의 지연이 낮습니다
- 단일 패키지: ABI 구분 없음, 플러그인 자체 네이티브 코드 없음 (Compose에 포함된 AndroidX graphics-path 보조 라이브러리만 네 가지 ABI로 내장), 하나의 APK로 모든 기기 지원
- 스크립트 API: `compose` / `$compose`, 노드 팩터리 29개와 유지 가능한 핸들, `compose.ref`, `compose.batch`, `compose.post`, `compose.theme`
- 통합 보호: 플러그인이 없거나 호환되지 않으면 가용성 확인이 사용 불가를 반환하고 `ComposeError`로 오류를 보고. 세션 종료나 스크립트 중지 시 소유 창과 콜백 해제

******

### 사용 방법

******

1. compose 스크립트 API가 포함된 호환 로컬 AutoJs6 빌드를 설치합니다 (최소 6.8.0 / 5316)
2. 이 플러그인 APK를 설치합니다 (열 필요가 없으며, 플러그인에는 런처 항목이 없습니다)
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
let status = compose.Text({ text: '준비 중...' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ onClick: () => win.close() }, '닫기'),
]), { x: 50, y: 300, raw: true });

threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => status.set({ text: `진행률 ${i}%` }));
    }
});
```

예제 모음, 전체 API 설명 및 TypeScript 타입 선언은 추후 제공됩니다. 현재 API 형태는 로드맵 부록 A를 따릅니다.

******

### 호환성

******

플러그인의 실행 요구 사항과 제한:

- 최소 AutoJs6 버전: 6.8.0 (5316) 이상. 더 낮은 버전의 호스트는 플러그인 센터에서 비호환으로 표시합니다
- Android 버전: 7.0 (API 24) 이상
- 프로세서 아키텍처: arm64-v8a / armeabi-v7a / x86_64 / x86 (네 가지 모두 단일 APK에 내장, 아키텍처별 선택 불필요)
- Compose 버전: 플러그인에 포함 (BOM 2026.09.00), 호스트의 Compose 런타임에 의존하지 않습니다
- 계약 버전: 1. 호스트와 플러그인은 계약 버전을 협상하며, 일치하지 않으면 명확한 오류와 함께 로드를 거부합니다

******

### 자주 묻는 질문

******

- 설치 후 플러그인 아이콘이 보이지 않는 이유는 무엇인가요? 플러그인에는 독립적인 UI와 런처 항목이 없습니다. AutoJs6 플러그인 센터에서 확인하세요
- `compose`를 찾을 수 없는 이유는 무엇인가요? 전역 객체는 호환되는 로컬 호스트 빌드에서 제공됩니다. 플러그인 APK만 설치하면 추가되지 않습니다
- 다른 UI 플러그인을 제거해야 하나요? 아니요. Compose UI는 기존 `ui` 모듈이나 다른 플러그인에 영향을 주지 않습니다
- 플러그인 업데이트 후 스크립트를 수정해야 하나요? 계약 버전이 그대로이면 필요 없습니다. 계약 업그레이드는 변경 로그에 명시됩니다
- 플로팅 창에 필요한 조건은 무엇인가요? 호스트에 오버레이 권한을 부여하고 문자 입력 전에 `window.requestFocus()`를 호출하세요. HyperOS에서 창이 보이지 않으면 바탕 화면으로 돌아가세요. 권한이 없으면 PERMISSION_REQUIRED를 반환하며 권한 화면을 자동으로 열지 않습니다

******

### 권한과 보안

******

플러그인은 Android 런타임 권한을 요청하지 않으며 네트워크, 저장소, 센서에 접근하지 않습니다.

- 컴포넌트 보호: Wake Activity와 INFO 서비스는 모두 `org.autojs.permission.PLUGIN` 서명 권한으로 보호되어 AutoJs6 호스트만 접근할 수 있습니다
- 백그라운드 동작 없음: 상주 서비스, 브로드캐스트 수신기, 예약 작업이 없으며 호스트가 로드하지 않는 동안 자원을 소모하지 않습니다
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
contract package: org.autojs.plugin.compose.api (version 1)
minimum host build: 5316 (6.8.0)
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

#### v1.0.0

_2026/10/03_

- `안내` P3 개발 미리보기: 호출 가능한 compose / $compose, 노드 팩터리 29개, 유지 가능한 핸들, 반응형 state/render/ref, batch/post/theme, UI 스크립트 마운트와 raw 또는 크기 조절 가능한 플로팅 창을 호환 로컬 AutoJs6 호스트 빌드에서 사용할 수 있습니다. 가용성 확인, 형식화된 오류 및 세션 정리도 포함됩니다. 예제 모음, 전체 API 문서, 타입 선언 및 더 넓은 검증 매트릭스는 아직 제공되지 않습니다. 현재는 공식 릴리스가 아닌 로컬 미리보기입니다
- `안내` AutoJs6 6.8.0 (5316) 이상이 필요합니다
- `새 기능` 플러그인 저장소 골격: 플랫폼 버전 플러그인 빌드 체인, Jetpack Compose BOM 2026.09.00 의존성, Wake Activity 활성화 프로토콜, INFO 서비스 (카테고리 compose-ui)
- `새 기능` JSON 원본에서 생성되는 10개 언어의 README, 플러그인 센터 설명, 변경 로그
- `새 기능` 미리보기 렌더링은 레이아웃, 텍스트, 아이콘, 이미지, 버튼, 선택 컨트롤 및 슬라이더를 지원하며, 전달한 비트맵의 소유권과 해제 책임은 호출자에게 유지
- `새 기능` 20가지 Modifier 작업 모두 선언 순서를 유지하며 레이아웃 범위 검사, 스크롤 및 접근성 레이블을 지원
- `새 기능` Material 3 테마는 시드 색상, 밝은 모드와 어두운 모드, Android 12+ 시스템 동적 색상, 글꼴 및 글자 크기 배율을 지원
- `새 기능` 화면 업데이트를 원자적으로 적용하고 거부 시 마지막으로 유효한 화면을 유지. 제어 입력은 큐에 넣은 콜백으로 변경을 알리며 종료 시 콜백을 해제
- `새 기능` 미리보기 입력란이 선택 범위와 IME 조합 텍스트를 유지하고 포커스와 명시적 편집을 지원하며, 최신 입력을 덮어쓰는 지연된 편집을 거부
- `새 기능` 안정적인 항목 key 및 인덱스 스크롤을 지원하는 지연 로딩 목록, Scaffold와 상단 앱 바 슬롯, 제어형 대화상자, 진행 표시기, 작업 또는 닫기 결과를 대기열 순서로 반환하는 Snackbar를 미리보기에 추가
- `새 기능` 스크립트 미리보기에 호출 가능한 compose / $compose, 노드 팩터리 29개, 유지 가능한 핸들, 반응형 state/render/ref, 일괄 처리, 예약 실행 및 테마 제어 제공
- `새 기능` UI 스크립트에서 Compose 콘텐츠를 마운트할 수 있으며 재마운트나 스크립트 중지 시 이전 세션과 콜백 해제
- `새 기능` 비 UI 스크립트에서 raw 또는 크기 조절 가능한 Compose 플로팅 창을 만들고 픽셀 위치와 크기, 터치 및 포커스를 변경 가능. 창 제어, floaty.closeAll 또는 스크립트 종료를 통한 닫기 지원
- `개선` 가용성 확인과 ComposeError가 플러그인 누락, 비활성화, 미승인, 비호환, 권한 부족 및 닫힌 세션을 일관되게 보고. 네이티브 창 연결 전 취소도 수명 주기 정리에서 처리
- `의존성` common-plugin-api.aar 버전 6.8.0 (5307) 추가 (MPL 2.0, 해시 고정)
- `의존성` Jetpack Compose BOM 2026.09.00 추가 (Apache 2.0)
- `의존성` AutoJs6 6.8.0 (5316)에 맞춘 compose-ui-api.aar V1 추가 (MPL 2.0, 해시 고정), 공유 의존성을 호스트와 일치시킴
- `의존성` BOM 2026.09.00에서 관리하는 Compose UI Test 추가 (Apache 2.0, 테스트 전용)

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
