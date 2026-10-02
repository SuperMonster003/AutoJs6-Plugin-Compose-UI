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

Compose UI هي إضافة لعرض الواجهات في AutoJs6. تعلن السكربتات عن واجهتها عبر الكائن العام `compose` المدمج في المضيف, وتعرضها الإضافة داخل عملية المضيف باستخدام Jetpack Compose و Material 3, لتوفر حلا تصريحيا واحدا لمحتوى الأنشطة في وضع `"ui";` وللنوافذ العائمة.

لا تحتوي الإضافة على أي شاشات مستقلة ولا تضيف مدخلا في المشغل. يكتشفها المضيف عبر خدمة INFO ويقرأ إصدارها وبيانات التوافق, ثم يحمل المعرض داخل عملية المضيف وفق العقد (`org.autojs.plugin.compose.api`). تبقى شجرة الواجهة والحالة والأحداث في جانب السكربت, ولا يقوم المعرض إلا بتطبيق الرقع على تركيب Compose وإعادة أحداث المستخدم إلى السكربت.

******

### الحالة الحالية

******

معاينة التطوير P1: يشغل محمل المضيف V1 والجلسات عدادا بمكونات Column / Text / Button في مضيف اختبار مخصص. لا يزال العارض الكامل وواجهة برمجة نصوص compose قيد التطوير.

******

### الميزات

******

القدرات الأساسية التي ستقدمها الإضافة:

- واجهة تصريحية: يعيد `compose.state` + `compose.mount(render)` الرسم تلقائيا عند تغير الحالة, بينما تتيح مقابض العقد طويلة العمر (`compose.Text({...})` وأمثالها) تعديل الخصائص والعقد الفرعية مباشرة
- المجموعة الأساسية من مكونات Material 3: التخطيطات (Column / Row / Box / LazyColumn وغيرها), النصوص, الأزرار, حقول الإدخال, المفاتيح, أشرطة التمرير, مؤشرات التقدم, البطاقات, مربعات الحوار
- سلاسل Modifier: يحافظ `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` على ترتيب العمليات, ويتحقق المضيف من العمليات المقيدة بالنطاق
- سطحان للعرض: محتوى النشاط لسكربتات `"ui";` (`compose.mount`) والنوافذ العائمة لأي سكربت (`compose.floaty`)
- عرض داخل العملية: يعمل المعرض داخل عملية المضيف دون أي جسر واجهة بين العمليات, فتبقى الأحداث وتحديثات الحالة منخفضة الكمون
- حزمة واحدة: بلا متغيرات ABI وبلا شفرة أصلية خاصة بالإضافة (فقط مكتبة AndroidX graphics-path المساعدة المرفقة مع Compose, مدمجة لجميع معماريات ABI الأربع), حزمة APK واحدة تناسب جميع الأجهزة

******

### طريقة الاستخدام

******

1. ثبت AutoJs6 الإصدار 6.8.0 (5308) أو أحدث
2. ثبت حزمة APK لهذه الإضافة (لا شيء لفتحه, فالإضافة بلا مدخل في المشغل)
3. تأكد في مركز الإضافات في AutoJs6 من أن Compose UI معروفة ومفعلة
4. استخدم الكائن العام `compose` مباشرة في السكربتات (ستصل قدرة العرض مع الإصدار 1.0.0)

******

### بداية سريعة

******

توضح الأمثلة أدناه الشكل المستهدف لواجهة البرمجة (محدد في الملحق A من خارطة الطريق, وغير قابل للتشغيل قبل تسليم العرض):

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
let status = compose.Text({ text: 'جار التحضير...' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ onClick: () => win.close() }, 'إغلاق'),
]), { x: 50, y: 300, raw: true });

threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => status.set({ text: `التقدم ${i}%` }));
    }
});
```

يوجد المرجع الكامل لواجهة البرمجة (فهرس المكونات, عمليات Modifier, كائنات الجلسة, رموز الأخطاء) في فصل وحدة compose من وثائق AutoJs6.

******

### التوافق

******

متطلبات التشغيل وحدود الإضافة:

- إصدار AutoJs6: 6.8.0 (5308) أو أحدث; تعرض المضيفات الأقدم الإضافة على أنها غير متوافقة في مركز الإضافات
- إصدار Android: 7.0 (API 24) أو أحدث
- معمارية المعالج: arm64-v8a / armeabi-v7a / x86_64 / x86 (الأربع جميعها مدمجة في حزمة APK الواحدة, دون اختيار حسب المعمارية)
- إصدار Compose: مضمن مع الإضافة (BOM 2026.09.00), مستقل عن بيئة Compose في المضيف
- إصدار العقد: 1; يتفاوض المضيف والإضافة على إصدار العقد ويرفضان التحميل مع خطأ واضح عند عدم التطابق

******

### الأسئلة الشائعة

******

- لماذا لا تظهر أيقونة الإضافة بعد التثبيت? ليس للإضافة واجهة مستقلة ولا مدخل في المشغل; ابحث عنها في مركز الإضافات في AutoJs6
- لماذا لا يعمل `compose` في السكربتات بعد? هذه معاينة تطوير في مرحلة P0; سيصل المعرض وواجهة برمجة السكربتات في مراحل لاحقة
- هل يلزم إلغاء تثبيت إضافات الواجهة الأخرى? لا, فإضافة Compose UI لا تتعارض مع وحدة `ui` الحالية ولا مع الإضافات الأخرى
- هل تحتاج السكربتات إلى تعديل بعد تحديث الإضافة? لا ما دام إصدار العقد ثابتا; تذكر ترقيات العقد صراحة في سجل التغييرات

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
minimum host build: 5308 (6.8.0)
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

_2026/10/02_

- `تلميح` معاينة التطوير P1: يشغل محمل المضيف V1 والجلسات عدادا بمكونات Column / Text / Button في مضيف اختبار مخصص. لا يزال العارض الكامل وواجهة برمجة نصوص compose قيد التطوير
- `تلميح` يتطلب AutoJs6 6.8.0 (5308) أو أحدث (يستكمل رقم البناء الأدنى الدقيق بعد اعتماد تغييرات جانب المضيف)
- `ميزة` هيكل مستودع الإضافة: سلسلة بناء إضافة إصدارات المنصة, اعتماديات Jetpack Compose BOM 2026.09.00, بروتوكول التفعيل Wake Activity وخدمة INFO (الفئة compose-ui)
- `ميزة` README وتعليمات مركز الإضافات وسجل التغييرات بعشر لغات, مولدة من مصادر JSON
- `ميزة` يدعم عداد المعاينة التحديثات التزايدية ويحرر الاستدعاءات عند الإغلاق; تحتفظ التحديثات المرفوضة بآخر واجهة صالحة
- `اعتمادية` إرفاق common-plugin-api.aar الإصدار 6.8.0 (5307) (MPL 2.0, مثبت بالتجزئة)
- `اعتمادية` إرفاق Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `اعتمادية` إضافة compose-ui-api.aar V1 (MPL 2.0, بصمة مقفلة), مع مواءمة الاعتماديات المشتركة مع المضيف

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
