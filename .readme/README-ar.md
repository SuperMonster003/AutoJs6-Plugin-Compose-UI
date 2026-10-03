<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>إضافة تجلب واجهات Jetpack Compose و Material 3 إلى سكربتات AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Compose-UI?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / اللغات

******

هذا المستند متاح باللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالية

******

### مقدمة

******

Compose UI هي إضافة لعرض الواجهات في AutoJs6. تعلن النصوص عن الواجهات عبر `compose` / `$compose` التي يوفرها المضيف, وتعرضها الإضافة داخل عمليته باستخدام Jetpack Compose وMaterial 3. تدعم المعاينة محتوى أنشطة `"ui";` والنوافذ العائمة للنصوص غير UI.

لا تحتوي الإضافة على أي شاشات مستقلة ولا تضيف مدخلا في المشغل. يكتشفها المضيف عبر خدمة INFO ويقرأ إصدارها وبيانات التوافق, ثم يحمل المعرض داخل عملية المضيف وفق العقد (`org.autojs.plugin.compose.api`). تبقى شجرة الواجهة والحالة والأحداث في جانب السكربت, ولا يقوم المعرض إلا بتطبيق الرقع على تركيب Compose وإعادة أحداث المستخدم إلى السكربت.

******

### الحالة الحالية

******

معاينة تطوير محلية 1.0.0: تتطلب بناء AutoJs6 مطابقا وإضافة مثبتة ومفعلة. تتوفر صفحات UI والنوافذ العائمة وخمسة أمثلة ومرجع API وتعريفات TypeScript لهذا التكامل المحلي. يسجل مخطط الطريق نطاق التوافق والأداء الذي تم التحقق منه. لم تدرج الإضافة في الفهرس الرسمي ولم تنشر كإصدار رسمي. الرسم الحالي للأيقونة مؤقت بانتظار صور المصدر النهائية من المشرف.

******

### الميزات

******

قدرات معاينة التطوير الحالية:

- واجهة تصريحية: يعيد `compose.state` + `compose.mount(render)` الرسم تلقائيا عند تغير الحالة, بينما تتيح مقابض العقد طويلة العمر (`compose.Text({...})` وأمثالها) تعديل الخصائص والعقد الفرعية مباشرة
- نواة Material 3: توفر 29 دالة لإنشاء عقد التخطيط والنصوص والأيقونات والصور والأزرار والإدخال والاختيار والقوائم الكسولة والحوارات والتقدم; Snackbar أمر للجلسة وليس دالة إنشاء compose.Snackbar
- سلاسل Modifier: يحافظ `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` على ترتيب العمليات, ويتحقق المضيف من العمليات المقيدة بالنطاق
- طريقتان للعرض: `compose.mount` أو استدعاء `compose` / `$compose` لمحتوى أنشطة `"ui";`, و`compose.floaty` للنوافذ العائمة raw أو القابلة لتغيير الحجم, بما في ذلك النصوص غير UI
- العرض داخل عملية المضيف: تطبق تحديثات الواجهة داخل المضيف وتوضع الأحداث في طابور خيط البرنامج النصي المالك; تستخدم خيوط العمل compose.post لطلب التحديث
- يتضمن APK واحد arm64-v8a / armeabi-v7a / x86_64 / x86 دون شيفرة أصلية خاصة بالإضافة; تتضمن الحزمة مكتبة AndroidX graphics-path المساعدة وتظل شروط توافق Android والمضيف والإضافة مطلوبة
- يحافظ تحرير النص الأصلي على التحديد وتركيب IME ويدعم التركيز والتعديلات الصريحة ويرفض التعديلات المتأخرة التي ستستبدل إدخالا أحدث; يتحكم البرنامج النصي بحالة المفاتيح وشرائط التمرير
- حماية التكامل: تشير الفحوص إلى عدم التوفر عند غياب الإضافة أو عدم توافقها, وتستخدم الأخطاء `ComposeError`, ويحرر إغلاق الجلسة أو إيقاف النص نوافذها واستدعاءاتها الراجعة
- خمسة أمثلة قابلة للتشغيل للعداد والتحقق من النموذج وقائمة من 1000 عنصر بمفاتيح ثابتة وHUD عائم خارج وضع UI والسمات, مع المتطلبات والفهرس ومزامنتها مع فئة أمثلة Compose UI في المضيف المطابق

******

### طريقة الاستخدام

******

1. ثبت بناء محليا مطابقا من AutoJs6 يتضمن نقطة دخول نصوص compose (الحد الأدنى 6.8.0 / 5316)
2. ثبت حزمة APK لهذه الإضافة (لا شيء لفتحه, فالإضافة بلا مدخل في المشغل)
3. تأكد في مركز الإضافات في AutoJs6 من أن Compose UI معروفة ومفعلة
4. استخدم `compose` أو `$compose` في النصوص; ركب الأنشطة عبر `compose.mount`, أو امنح المضيف إذن العرض فوق التطبيقات واستخدم `compose.floaty`

******

### بداية سريعة

******

يمكن تشغيل العداد وHUD العائم أدناه مع مضيف المعاينة المحلي المطابق. امنح المضيف إذن العرض فوق التطبيقات الأخرى قبل تشغيل HUD:

```js
"ui";

// عداد (طبقة render التصريحية)
let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `تم النقر ${count.value} مرة`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, 'أضف واحدا'),
]));
```

```js
// HUD عائم (طبقة مقابض العقد)
let worker = null;
let status = compose.Text({ text: 'جار التحضير...', color: '#FFFFFF' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ contentColor: '#FFFFFF', onClick: () => win.close() }, 'إغلاق'),
]), { x: 50, y: 300, raw: true });
win.on('close', () => { if (worker) worker.interrupt(); });

worker = threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => {
            if (!win.isClosed()) status.text = `التقدم ${i}%`;
        });
    }
});
```

تسرد assets/examples/index.json خمسة برامج نصية قابلة للتشغيل وتزامنها مع فئة Compose UI في المضيف المطابق. تشرح مقدمة كل مثال الوضع والأذونات. استخدم مرجع API المحلي وتعريفات TypeScript/المحرر المرافقة لتفاصيل العقد والمعدلات والسمات والجلسات والنوافذ; قد لا يتضمن الموقع هذه التغييرات المحلية بعد.

******

### التوافق

******

متطلبات التشغيل وحدود الإضافة:

- الحد الأدنى لإصدار AutoJs6: 6.8.0 (5316) أو أحدث; تعرض المضيفات الأقدم الإضافة على أنها غير متوافقة في مركز الإضافات
- إصدار Android: 7.0 (API 24) أو أحدث
- معمارية المعالج: arm64-v8a / armeabi-v7a / x86_64 / x86 (الأربع جميعها مدمجة في حزمة APK الواحدة, دون اختيار حسب المعمارية)
- إصدار Compose: مضمن مع الإضافة (BOM 2026.09.00), مستقل عن بيئة Compose في المضيف
- إصدار العقد: 1; يتفاوض المضيف والإضافة على إصدار العقد ويرفضان التحميل مع خطأ واضح عند عدم التطابق
- تتطلب التطبيقات المجمعة أيضا تثبيت إضافة Compose UI متوافقة بشكل منفصل مع إعدادات تفعيل/تفويض تخص التطبيق; يفحص التوافق وقت تشغيل AutoJs6 المضمن وليس versionCode الخاص بالتطبيق

******

### الأسئلة الشائعة

******

- لماذا لا تظهر أيقونة الإضافة بعد التثبيت? ليس للإضافة واجهة مستقلة ولا مدخل في المشغل; ابحث عنها في مركز الإضافات في AutoJs6
- لماذا لا يوجد `compose`? يوفر البناء المحلي المطابق للمضيف هذا الكائن العام; تثبيت APK الإضافة وحده لا يضيفه
- هل يلزم إلغاء تثبيت إضافات الواجهة الأخرى? لا, فإضافة Compose UI لا تتعارض مع وحدة `ui` الحالية ولا مع الإضافات الأخرى
- ماذا يحدث عند تغيير الإضافة? يؤدي تحديثها أو إزالتها أو تعطيلها إلى إغلاق الجلسات النشطة والإبلاغ عن الخطأ المقابل; تسمح الإضافة المتوافقة والمفعلة بتركيب جديد
- ماذا تتطلب النوافذ العائمة? امنح المضيف إذن العرض فوق التطبيقات واستدع `window.requestFocus()` قبل إدخال النص. إذا لم يعرض HyperOS النافذة, عد إلى سطح المكتب. يعيد غياب الإذن PERMISSION_REQUIRED دون فتح طلب ترخيص تلقائي
- هل يمكن استدعاء أي دالة Compose أو استخدام JSX/TSX? لا. استخدم دوال إنشاء العقد والأوامر الموثقة; لا تجمع الإضافة Kotlin ولا تكشف دوال Composable عشوائية
- هل يفقد التدوير الحالة? يعالج المضيف الحالي تغييرات الاتجاه العادية مع الاحتفاظ بمحرك البرنامج النصي. إعادة إنشاء Activity فعليا أو إتلافها يغلق المحرك وجلساته; لا تستعاد حالة العمل تلقائيا
- كيف تعثر المحددات على المكونات? يعرض testTag كمعرف ID أصلي دون بادئة حزمة. تختلف id/testTag عن desc/contentDescription; قد يكون نص Button عقدة فرعية لذا اتبع parent() إلى سلف قابل للنقر عند الحاجة

******

### الأذونات والأمان

******

لا تطلب الإضافة أي أذونات وقت تشغيل من Android ولا تصل أبدا إلى الشبكة أو التخزين أو المستشعرات.

- حماية المكونات: يحمي إذن التوقيع `org.autojs.permission.PLUGIN` كلا من Wake Activity وخدمة INFO, فلا يصل إليهما إلا مضيف AutoJs6
- لا نشاط في الخلفية: ليس للإضافة خدمات مقيمة ولا مستقبلات بث ولا مهام مجدولة, ولا تستهلك موارد ما لم يحملها المضيف
- حدود البيانات: لا تقرأ الإضافة بيانات السكربتات أو ملفات المستخدم ولا تكتبها; توجد حالة الواجهة في ذاكرة عملية المضيف فقط
- سياسة النسخ الاحتياطي: النسخ الاحتياطي للتطبيق والنقل بين الأجهزة معطلان, ولا تحتفظ الإضافة بأي بيانات تستحق الترحيل

عند تحميل المعرض يحتفظ المضيف بنموذج أذونات السكربتات الخاص به; لا توسع الإضافة قدرات النظام التي يمكن للسكربتات الوصول إليها.

******

### واجهة الإضافة

******

المعرفات المكشوفة للمضيف:

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

يكتشف المضيف الإضافة عبر `org.autojs.plugin.INFO` ويقرأ بيانات القدرات مثل `requiresHostVersion`; يعلن عن صنف مصنع المعرض عبر البيانات الوصفية `org.autojs.plugin.compose.RENDERER_FACTORY`, وينشئ المضيف محمل أصناف من مسار حزمة APK للإضافة (والمضيف أبا له) ثم ينشئ النسخة داخل عمليته.

******

### خارطة الطريق

******

تسجل المراحل وقرارات التصميم ومعايير القبول في خارطة طريق واحدة:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/10/03_

- `تلميح` معاينة تطوير محلية 1.0.0: تتطلب بناء AutoJs6 مطابقا وإضافة مثبتة ومفعلة. تتوفر صفحات UI والنوافذ العائمة وخمسة أمثلة ومرجع API وتعريفات TypeScript لهذا التكامل المحلي. يسجل مخطط الطريق نطاق التوافق والأداء الذي تم التحقق منه. لم تدرج الإضافة في الفهرس الرسمي ولم تنشر كإصدار رسمي. الرسم الحالي للأيقونة مؤقت بانتظار صور المصدر النهائية من المشرف
- `تلميح` يتطلب AutoJs6 6.8.0 (5316) أو أحدث
- `تلميح` تتطلب التطبيقات المجمعة أيضا تثبيت إضافة Compose UI متوافقة بشكل منفصل مع إعدادات تفعيل/تفويض تخص التطبيق; يفحص التوافق وقت تشغيل AutoJs6 المضمن وليس versionCode الخاص بالتطبيق
- `تلميح` يبقى مدخلا mipmap الفاتح/الداكن; الرسم الحالي مؤقت حتى يقدم المشرف صور المصدر النهائية بالأبيض والأسود
- `ميزة` compose / $compose القابلة للاستدعاء مع مقابض عقد دائمة وstate/render/ref تفاعلية وتغييرات مجمعة ومهام في الطابور والتحكم بالسمات
- `ميزة` نواة Material 3: توفر 29 دالة لإنشاء عقد التخطيط والنصوص والأيقونات والصور والأزرار والإدخال والاختيار والقوائم الكسولة والحوارات والتقدم; Snackbar أمر للجلسة وليس دالة إنشاء compose.Snackbar
- `ميزة` تركب برامج UI محتوى Activity; وتدعم compose.floaty أيضا البرامج خارج UI بنوافذ raw أو قابلة لتغيير الحجم وهندسة بالبكسل وتحكم باللمس/التركيز وتنظيف الموارد المملوكة
- `ميزة` تحافظ عمليات Modifier العشرون كلها على ترتيب التعريف, وتدعم التحقق من نطاق التخطيط والتمرير وتسميات إمكانية الوصول
- `ميزة` تدعم سمات Material 3 ألوان البداية والوضعين الفاتح والداكن وألوان النظام الديناميكية في Android 12+ وعائلات الخطوط وتحجيم النص
- `ميزة` يحافظ تحرير النص الأصلي على التحديد وتركيب IME ويدعم التركيز والتعديلات الصريحة ويرفض التعديلات المتأخرة التي ستستبدل إدخالا أحدث; يتحكم البرنامج النصي بحالة المفاتيح وشرائط التمرير
- `ميزة` أيقونات core وصور ImageWrapper/Bitmap والملفات المحلية وdrawable المضيف; لا يعيد محرك العرض تدوير موارد الصور المملوكة للمتصل تلقائيا
- `ميزة` خمسة أمثلة قابلة للتشغيل للعداد والتحقق من النموذج وقائمة من 1000 عنصر بمفاتيح ثابتة وHUD عائم خارج وضع UI والسمات, مع المتطلبات والفهرس ومزامنتها مع فئة أمثلة Compose UI في المضيف المطابق
- `ميزة` مرجع API وتعريفات TypeScript المرافقة إضافة إلى README وتعليمات مركز الإضافات وسجل التغييرات بعشر لغات
- `ميزة` اكتشاف في مركز الإضافات مع التحقق من إصدار المضيف والعقد والتفويض دون شاشة مستقلة أو مدخل في المشغل
- `إصلاح` يرفض Compose UI تركيب صفحة أخرى أو نافذة عائمة داخل رد نداء العرض مع الاحتفاظ بالصفحة الحالية; يمكن تركيب الصفحات مجددا بعد تحديث الإضافة
- `تحسين` تبلغ فحوص التوفر وComposeError بشكل موحد عن الإضافات المفقودة أو المعطلة أو غير المصرح بها أو غير المتوافقة ونقص الأذونات والجلسات المغلقة; يشمل التنظيف النوافذ الملغاة قبل اتصالها الأصلي; يؤدي تحديث الإضافة أو إزالتها أو تعطيلها إلى إغلاق الجلسات النشطة والإبلاغ عن الخطأ المناسب
- `اعتمادية` إرفاق common-plugin-api.aar الإصدار 6.8.0 (5307) (MPL 2.0, مثبت بالتجزئة)
- `اعتمادية` إرفاق Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `اعتمادية` إضافة compose-ui-api.aar V1 المتوافق مع AutoJs6 6.8.0 (5316) (MPL 2.0, بصمة مقفلة), مع مواءمة الاعتماديات المشتركة مع المضيف
- `اعتمادية` إضافة Compose UI Test الذي يديره BOM 2026.09.00 (Apache 2.0, للاختبارات فقط)
- `اعتمادية` إضافة JaCoCo الإصدار 0.8.14 (لتغطية الاختبارات الاختيارية فقط, ولا يدرج في حزم الإصدار)

##### لمزيد من سجل الإصدارات, راجع

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء

******

بعد الاستنساخ, ابن مباشرة باستخدام Gradle Wrapper; تختار إضافة إصدارات المنصة إصداري Android Gradle Plugin و Kotlin تلقائيا وفق بيئة IDE الحالية.

بناء حزمة APK للتصحيح:

```powershell
.\gradlew.bat :app:assembleDebug
```

تشغيل اختبارات وحدة JVM وتعبئة اختبارات العقد على الجهاز:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

بناء حزمة APK للإصدار (يتطلب `sign.properties` ومفتاح التوقيع):

```powershell
.\gradlew.bat :app:assembleRelease
```

التحقق من التوقيع وإنتاج ملف الإصدار مع لاحقة الملخص:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

التحقق من مطابقة المستندات المترجمة لمصادرها:

```powershell
py .python\generate_markdown.py --check
```

يتطلب البناء JDK 21 أو أحدث. بعد تعديل المصادر تحت `.readme` أو `.changelog`, شغل `py .python\generate_markdown.py` لإعادة توليد جميع المستندات.

******

### تنظيم الوثائق

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

يولد README وتعليمات مركز الإضافات وسجل التغييرات جميعها من مصادر JSON تحت `.readme` و `.changelog`; لا تحرر ملفات Markdown المولدة مباشرة.

******

### الرخصة

******

يصدر هذا المشروع بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE). ترد معلومات تراخيص مكونات الأطراف الثالثة في [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md).

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
- وثائق AutoJs6: https://docs.autojs6.com
- وثائق وحدة compose: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- إشعارات الأطراف الثالثة: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
