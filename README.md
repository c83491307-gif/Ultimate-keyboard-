<div align="center">

# ⌨️ NovaKeys — كيبورد أندرويد احترافي

### لوحة مفاتيح ذكية: Clipboard • OCR • Universal Copy • ثيمات قابلة للتخصيص

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Privacy](https://img.shields.io/badge/Privacy-Offline%20First-success)
![License](https://img.shields.io/badge/License-MIT-blue)
![Status](https://img.shields.io/badge/Status-Active%20development-orange)

</div>

> **حالة المشروع بشفافية (2026-10-29):** أصبح المستودع يحتوي على خدمة لوحة مفاتيح عربية/إنجليزية أساسية، ومدير كلمات مرور، وحافظة صور متعددة العناصر، وتحويل النص والإيموجي إلى PNG، وخدمة Autofill أولية. نجح بناء Debug وتشغيل اختبارات الوحدة ورفع APK كـ CI artifact للالتزام البرمجي `8335878a` في GitHub Actions. لا يعد ذلك اختبارًا على هاتف فعلي أو إصدار Release موقّعًا. هذا لا يثبت اختباراً على جهاز فعلي، ولا يعني اكتمال كل المميزات المذكورة أدناه. ملف APK متاح كـ CI artifact مؤقت وليس إصداراً رسمياً منشوراً.

## 📖 نظرة عامة

**NovaKeys** مشروع لوحة مفاتيح أندرويد يهدف إلى جمع تجربة كتابة قابلة للتخصيص، ومدير حافظة، واستخراج النصوص من الصور، وأدوات مساعدة في تطبيق واحد. الأولوية للسرعة والخصوصية والمعالجة المحلية حيثما أمكن.

### رموز الحالة

| الرمز | المعنى |
|:---:|---|
| ✅ | منفّذ ومتحقق منه في إصدار محدد |
| 🚧 | قيد التطوير أو لم يكتمل التحقق |
| 🗺️ | مخطط للمستقبل |

لا تُستخدم علامة الإنجاز إلا بعد وجود تنفيذ فعلي واختبار يمكن مراجعته.

---

## ⚙️ إعداد لوحة المفاتيح الافتراضية والرسم

- شاشة الإعداد تعرض حالة NovaKeys اعتمادًا على إعدادات أندرويد: هل لوحة المفاتيح مفعّلة، وهل هي الافتراضية حاليًا.
- زر **Enable NovaKeys** يفتح إعدادات طرق الإدخال لتمكينها يدويًا، وزر **Choose default keyboard** يفتح منتقي لوحة المفاتيح النظامي. لا يحاول التطبيق تغيير لوحة المفاتيح الافتراضية سرًا؛ الاختيار النهائي للمستخدم.
- تظهر أدوات الإعداد حتى قبل فتح خزنة كلمات المرور، وتُحدّث الحالة عند الرجوع من إعدادات أندرويد.
- زر **✎ Draw** داخل شريط أدوات الكيبورد يفتح لوحة رسم بالإصبع مع **Undo** و**Clear** و**Cancel** و**Save & insert image**.
- الرسم يُحوّل محليًا إلى PNG ويُحفظ في حافظة الصور الخاصة بـ NovaKeys، ثم يحاول إدراجه في التطبيق الحالي إن كان يدعم إدخال الصور. وإلا يُنسخ URI إلى حافظة النظام، ويمكن مشاركة الصورة من مكتبة الصور.
- لا تحتاج ميزة الرسم إلى صلاحيات إضافية. يتم إخفاء أدوات الصور والرسم في حقول كلمات المرور.


### خزنة كلمات المرور والنسخ الاحتياطي

- **نسخة محلية مشفرة:** اختر `Export encrypted backup` لحفظ نسخة JSON مشفرة، أو `Import phone backup` لاستعادتها. يمكنك أيضاً حفظ النسخة مباشرة داخل مجلد `NovaKeys` الذي تختاره في ذاكرة الهاتف.
- **ذاكرة الهاتف والملفات:** اختر مجلداً من منتقي أندرويد ليُنشئ التطبيق بداخله مجلد `NovaKeys`. يمكنك استيراد ملفات وصور ووسائط متعددة إليه، عرض الملفات التي حفظها التطبيق، وتصديرها إلى موقع آخر.
- **بدون حسابات:** لا يوجد تسجيل Gmail أو Firebase؛ النسخ والملفات محلية على الجهاز أو في المجلد الذي تختاره.
- **لا تفقد عبارة مرور النسخة:** لا يستطيع التطبيق فك النسخة المشفرة من دونها. ملفات CSV العادية تحتوي كلمات مرور مقروءة.
- ملف APK المنشور من قناة `latest` هو بناء اختبار Debug، وليس إصدار متجر إنتاجيًا.

## ✨ المميزات

### 1. 📋 Clipboard — إدارة الحافظة

| الميزة | الوصف | الحالة المستهدفة |
|---|---|:---:|
| سجل محلي كبير | حفظ النصوص المنسوخة باستخدام Room | 🚧 |
| التقاط النسخ تلقائياً | ضمن ما يسمح به إصدار أندرويد وسياق التطبيق | 🚧 |
| تصنيف ذكي | نص، روابط، أرقام، بريد، OTP، كود وصور عند الدعم | 🗺️ |
| تثبيت العناصر | إبقاء العناصر المهمة في الأعلى | 🗺️ |
| مجلدات ووسوم | تنظيم السجل | 🗺️ |
| بحث سريع | فهرسة وبحث في السجل | 🗺️ |
| حذف تلقائي | سياسات احتفاظ للعناصر الحساسة | 🗺️ |
| استثناءات حساسة | تجنب حفظ البيانات الحساسة حيث يمكن تحديد السياق | 🚧 |
| قوالب واختصارات | توسيع اختصار إلى نص جاهز | 🗺️ |
| لصق متعدد | إدراج عدة عناصر بفاصل يختاره المستخدم | 🗺️ |
| تنظيف النص | إزالة التنسيق والمسافات الزائدة وتغيير حالة الأحرف | 🗺️ |
| تشفير محلي | تشفير البيانات وإدارة المفاتيح بأمان | 🗺️ |
| نسخ احتياطي | نسخ محلي مشفر + استيراد/تصدير | 🗺️ |
| مزامنة مشفرة | مزامنة اختيارية بين الأجهزة | 🗺️ |

**قيد مهم:** يقيّد أندرويد وصول التطبيقات إلى الحافظة في الخلفية. لا يمكن ضمان التقاط كل عملية نسخ من كل تطبيق. يجب أن يكون الالتقاط واضحاً للمستخدم وألا يحفظ كلمات المرور أو بيانات الدفع دون قصد.

### 2. 🖼️ حافظة لقطات الشاشة والصور (Image Clipboard)

- استيراد مجموعة من الصور ولقطات الشاشة دفعة واحدة عبر أداة اختيار الصور الرسمية في أندرويد.
- الاحتفاظ بنسخ محلية داخل مساحة NovaKeys الخاصة، مع شريط يعرض أحدث 6 صور داخل الكيبورد.
- لا يوجد حد برمجي لعدد الصور المحفوظة؛ الحد الفعلي هو المساحة الحرة على الجهاز. يمكن حذف صورة بعينها أو مسح الحافظة كاملة دون حذف النسخ الأصلية من المعرض.
- مزامنة تلقائية اختيارية للقطات الجديدة التي تظهر في مجلدات/أسماء Screenshots، عند منح صلاحية مكتبة الصور الكاملة وبقاء خدمة لوحة مفاتيح NovaKeys عاملة.
- الضغط على صورة من شريط الكيبورد يحاول إدراجها مباشرة عبر `InputConnection.commitContent` إذا أعلن التطبيق الحالي أنه يقبل صورًا. إذا رفض التطبيق ذلك، تُنسخ وصلة الصورة إلى حافظة النظام؛ اللصق كصورة يعتمد على دعم التطبيق المستقبل.
- يمكن مشاركة أي صورة محفوظة إلى تطبيق آخر من مكتبة الصور.

**قيود أندرويد:** لا يمكن ضمان التقاط كل لقطة شاشة في جميع الحالات. المزامنة التلقائية تتوقف إذا أوقف النظام/المستخدم خدمة NovaKeys، أو لم يمنح التطبيق صلاحية الصور الكاملة، أو لم يكشف الجهاز عن اللقطة ضمن MediaStore. تبقى أداة الاستيراد اليدوي متاحة دون منح إذن مكتبة الصور الشامل. ولا يتم التقاط الشاشة أو تجاوز تطبيقات الحماية بصمت.

### 3. 📝 تحويل النص المنسوخ إلى صورة

- زر `T→IMG` في الكيبورد يفتح محررًا يقرأ النص الموجود في الحافظة، مع إمكانية تعديله أو لصق نص آخر.
- توليد PNG محليًا بتخطيط واضح، وخلفية هادئة، ودعم التفاف السطور والنص العربي والإنجليزي والإيموجي عبر خطوط النظام.
- حفظ الصورة تلقائيًا في حافظة الصور لتظهر في شريط الكيبورد، مع إمكانية مشاركتها أو نسخ رابطها إلى حافظة النظام.
- حد حماية عملي: 6000 حرف تقريبًا للنص المدخل وارتفاع أقصى للصورة، لتجنب استهلاك ذاكرة الجهاز بشكل مفرط. النص الطويل يمكن تقسيمه إلى صور متعددة.

تتم المعالجة محليًا دون إرسال النص أو الإيموجي إلى خادم. يجب تأكيد نجاح البناء واختبار عرض الإيموجي الفعلي على الجهاز قبل اعتماد النتيجة النهائية.

### 3. 🔍 OCR — استخراج النص من الصور

- مسار OCR محلي باستخدام ML Kit أو Tesseract عند توفر المحرك واللغة المناسبين. 🚧
- استهداف العربية والإنجليزية ولغات إضافية وفق دعم المحرك. 🚧
- اختيار صورة من الجهاز واستخراج النص منها. 🚧
- تحديد منطقة من الصورة قبل الاستخراج. 🗺️
- OCR مباشر بالكاميرا بعد موافقة المستخدم. 🗺️
- تحسين ترتيب الأسطر والفقرات؛ لا يُضمن استرجاع الجداول المعقدة بدقة كاملة. 🗺️
- ترجمة النص عبر نموذج محلي أو مزود خارجي يختاره المستخدم. 🗺️
- قراءة QR والباركود بمكتبة مناسبة. 🗺️

يجب توثيق ما يعمل دون اتصال وما يحتاج إلى تنزيل حزمة لغة أو نموذج إضافي.

### 4. 🌐 Universal Copy — نسخ النصوص من الشاشة

| الطريقة | الوظيفة | الحالة المستهدفة |
|---|---|:---:|
| Accessibility Scan | قراءة النصوص التي تكشفها شجرة إمكانية الوصول | 🚧 |
| OCR Fallback | تحليل لقطة شاشة بموافقة المستخدم | 🗺️ |
| Overlay Picker | اختيار النص من واجهة فوق الشاشة | 🗺️ |
| زر عائم | اختصار اختياري لأدوات النسخ | 🗺️ |
| Quick Settings Tile | اختصار من الإعدادات السريعة | 🗺️ |

لا يمكن لهذه الميزة قراءة كل محتوى كل تطبيق. قد يتعذر الوصول إلى المحتوى المحمي أو المخفي أو المرسوم داخل الصور والفيديو. خدمات Accessibility وصلاحيات الظهور فوق التطبيقات اختيارية، ويجب شرح سبب استخدامها.

### 5. ⌨️ تجربة الكتابة

| الميزة | الحالة المستهدفة |
|---|:---:|
| لوحة مفاتيح أصلية عبر InputMethodService | 🚧 |
| تخطيطات عربية وإنجليزية أساسية | 🚧 |
| تصحيح تلقائي واقتراح كلمات | 🗺️ |
| توقع الكلمة التالية وتعلم اختياري على الجهاز | 🗺️ |
| دعم لغات متعددة بحسب القواميس المتاحة | 🗺️ |
| تشكيل وهمزات وكشيدة وأرقام عربية/هندية | 🗺️ |
| Transliteration من اللاتينية إلى العربية | 🗺️ |
| وضع اليد الواحدة وتغيير الحجم | 🗺️ |
| لوحة عائمة أو منقسمة للأجهزة المناسبة | 🗺️ |
| صف أرقام وصف رموز قابلان للتخصيص | 🗺️ |
| تحريك مؤشر الكتابة عبر مفتاح المسافة | 🗺️ |
| Cut / Copy / Paste / Select All | 🗺️ |
| ضغط مطول للأحرف البديلة | 🗺️ |
| دعم لوحة مفاتيح فيزيائية | 🗺️ |
| كتابة صوتية دون اتصال | 🗺️ |

الأداء الفعلي يعتمد على الجهاز وإصدار أندرويد والقواميس والنماذج. أي أرقام أداء في هذا الملف أهداف قياس وليست نتائج benchmark مؤكدة.

### 6. 🔐 مدير كلمات المرور والتعبئة التلقائية

- إضافة وتعديل وحذف بيانات الدخول مع البحث والمجلدات.
- مولّد كلمات مرور محلي وتدقيق أولي لكلمات المرور الضعيفة أو المكررة.
- استيراد CSV متوافق مع أعمدة Chrome وBitwarden، وتصدير بصيغتي CSV عامة وBitwarden.
- خدمة Android Autofill أولية، مع تطبيع النطاق وتجاوز حقول الدفع المكتشفة.
- قفل واجهة الخزنة بالبصمة أو بيانات اعتماد قفل الجهاز؛ جلسة فتح محدودة زمنياً.
- **تحذير:** ملفات CSV المصدرة تحتوي كلمات مرور كنص واضح. لا تحفظها في مكان مشترك، واحذفها بعد الاستيراد.
- **حدود التحقق:** يجب اختبار التعبئة والحفظ والتوافق مع تطبيقات ومواقع فعلية. لا تُعلن Inline Suggestions مخصصة كميزة منفذة حالياً؛ يستخدم النظام واجهة Autofill القياسية عند توفرها.

### 7. 😀 Emoji • GIF • Stickers

- لوحة إيموجي مع بحث ومفضلة وسجل حديث. 🗺️
- درجات البشرة والأعلام وفق دعم Unicode والخطوط. 🗺️
- Kaomoji ورموز خاصة ونصوص مزخرفة. 🗺️
- GIF وملصقات عبر مزود خارجي اختياري مع توضيح سياسة الخصوصية. 🗺️
- إنشاء ملصقات من الصور ومعالجة الخلفية محلياً عند توفر مكتبات مناسبة. 🗺️

### 8. 🧠 أدوات ذكية داخل الكيبورد

| الأداة | الوظيفة المستهدفة | الحالة |
|---|---|:---:|
| AI Rewrite | إعادة صياغة النص وتغيير النبرة | 🗺️ |
| Translate | ترجمة النص قبل إدراجه | 🗺️ |
| Calculator | حساب تعبيرات مثل 15% of 2400 | 🗺️ |
| Converter | تحويل الوحدات والمناطق الزمنية والعملات | 🗺️ |
| Password Generator | توليد كلمات مرور محلياً | 🗺️ |
| Date/Time | إدراج التاريخ والوقت | 🗺️ |
| Case Tools | UPPER وlower وTitle وcamelCase وsnake_case | 🗺️ |
| Text Expander | اختصارات نصية مع متغيرات | 🗺️ |
| Quick Search | فتح بحث الويب أو الموسوعات | 🗺️ |
| Share Location | إدراج الموقع بعد طلب إذن صريح | 🗺️ |

يمكن تنفيذ الأدوات الحسابية والنصية البسيطة محلياً. قد تتطلب الترجمة وإعادة الصياغة نموذجاً محلياً كبيراً أو مزوداً خارجياً ومفتاح API. لن تُرسل النصوص إلى مزود خارجي دون توضيح وموافقة المستخدم.

---

## 🎨 نظام الثيمات والألوان

### ثيمات مقترحة

| الثيم | الخلفية | المفاتيح | اللون المميز | النص |
|---|---|---|---|---|
| 🌑 Midnight | #0B0F19 | #161B2B | #6C63FF | #E6E8F0 |
| ⚫ AMOLED Black | #000000 | #0D0D0D | #00E5FF | #FFFFFF |
| ☀️ Snow | #F5F7FA | #FFFFFF | #1976D2 | #1A1C1E |
| 🌊 Ocean | #06283D | #0F4C75 | #3FC1C9 | #F1FEFF |
| 🌲 Forest | #0F2A1D | #1B4332 | #52B788 | #E9F5EC |
| 🌅 Sunset | #2B1020 | #4A1942 | #FF7E5F | #FFEFE8 |
| 🍬 Candy | #FFE5F1 | #FFFFFF | #FF4D8D | #4A1230 |
| 🌌 Aurora | #0A0E27 | #1B1F4B | #7DF9C0 | #EAF6FF |
| 🔥 Ember | #1A0A0A | #2E1111 | #FF5A36 | #FFE9E3 |
| 🧊 Glacier | #E8F4FA | #FFFFFF | #00A6D6 | #0C2A36 |
| 👾 Cyberpunk | #0D0221 | #1A0B3B | #FF2A6D / #05D9E8 | #F0F0FF |
| 🖥️ Terminal | #000A00 | #0A1A0A | #39FF14 | #B6FFB0 |
| 🏜️ Sand | #F4E9D8 | #FFF8EC | #C97B3A | #3A2A1A |
| 🌸 Sakura | #FFF0F5 | #FFFFFF | #F48FB1 | #4A2A35 |
| 🕌 Emerald Gold | #06221A | #0B3D2E | #D4AF37 | #F6F1DC |

هذه لوحات تصميم أولية؛ لا تُعد الثيمات متاحة حتى تنفيذها واختبارها.

### التخصيص المستهدف

- Material You / Dynamic Color على الأجهزة المدعومة. 🗺️
- منتقي RGB / HSV / HEX لكل عنصر. 🗺️
- تدرجات لونية متعددة نقاط التوقف. 🗺️
- خلفية من المعرض مع ضبابية وتعتيم. 🗺️
- خلفيات متحركة مع مراعاة الأداء والبطارية. 🗺️
- شفافية وتأثيرات بصرية قابلة للضبط. 🗺️
- وضع فاتح وداكن حسب النظام أو الوقت. 🗺️
- ثيم مستقل لكل تطبيق. 🗺️
- استيراد وتصدير الثيمات بصيغة novatheme. 🗺️
- متجر ثيمات مجتمعي. 🗺️

### الأشكال والتأثيرات

الأشكال المقترحة: Rounded، Square، Pill، Circle، Hexagon، Diamond، Squircle، Cut-Corner، Bubble، Mechanical، وFlat.

التأثيرات المستهدفة: ظلال وحدود قابلة للتخصيص، Ripple، معاينة المفتاح، أثر Swipe، انتقالات Spring / Fade / Slide، إضاءة RGB اختيارية، واهتزاز قابل للتخصيص.

### الخطوط والتخطيطات

- دعم خطوط النظام أولاً، ثم استيراد خطوط بعد التحقق من سلامة الملفات وحقوق الاستخدام.
- ضبط ارتفاع لوحة المفاتيح وتباعد المفاتيح.
- تخطيطات مستهدفة: QWERTY وAZERTY وQWERTZ وDvorak وColemak وعربي 101 وتخطيط عربي مناسب للمستخدم المصري.
- أصوات نقر اختيارية مع زر كتم واضح.

كل خيار يحتاج إلى تنفيذ واختبار قبل اعتباره متاحاً.

---

## 🔒 الخصوصية والأمان

| المبدأ | متطلبات التنفيذ |
|---|---|
| Local-first | تخزين ونسخ احتياطي محلي دون تسجيل دخول أو خدمة سحابية |
| تشفير محلي | مكتبة موثوقة وإدارة آمنة لمفاتيح التشفير |
| قفل الحافظة | قفل اختياري بالبصمة أو PIN |
| Incognito Mode | تعطيل الحفظ والتعلم أثناء تفعيله |
| الحقول الحساسة | تعطيل الاقتراحات والتعلم في حقول كلمات المرور وبيانات الدفع |
| لا تحليلات ولا إعلانات | عدم إضافة SDK للتتبع أو الإعلانات |
| الشفافية | توثيق كل صلاحية واتصال خارجي |

### الصلاحيات

| الصلاحية أو الخدمة | السبب | الحالة |
|---|---|---|
| BIND_INPUT_METHOD | ربط خدمة لوحة المفاتيح بالنظام | مطلوبة للكيبورد |
| AccessibilityService | قراءة النصوص التي تكشفها التطبيقات | اختيارية لـ Universal Copy |
| SYSTEM_ALERT_WINDOW | زر عائم أو واجهة فوق التطبيقات | اختيارية |
| CAMERA | OCR مباشر وقراءة الباركود | اختيارية |
| RECORD_AUDIO | الكتابة الصوتية | اختيارية |
| USE_BIOMETRIC | قفل الحافظة بالبصمة | اختيارية |
| POST_NOTIFICATIONS | إشعارات الخدمات عند الحاجة | حسب التنفيذ وإصدار أندرويد |

> لوحة المفاتيح تتعامل مع نصوص حساسة. يجب ألا تُرسل المدخلات إلى الشبكة أو تُسجل سراً. وصف التطبيق بأنه Offline-first لا يغني عن مراجعة الكود والأذونات والاعتماديات.

### قيود أندرويد

على Android 10 وما بعده، تُقيّد قراءة الحافظة في الخلفية. قد يتطلب الالتقاط التلقائي أن تكون NovaKeys لوحة المفاتيح النشطة أو أن يستخدم المستخدم مساراً مدعوماً ومصرحاً به. تختلف القيود حسب إصدار أندرويد والشركة المصنّعة.

---

## 📲 التثبيت والتنزيل

### أحدث نسخة للاختبار

- [تنزيل APK مباشرة](https://github.com/c83491307-gif/Ultimate-keyboard-/releases/download/latest/app-debug.apk)
- [تنزيل ZIP البديل](https://github.com/c83491307-gif/Ultimate-keyboard-/releases/download/latest/app-debug-apk.zip) — فك الضغط ثم ثبّت ملف APK.
- [بصمات SHA-256](https://github.com/c83491307-gif/Ultimate-keyboard-/releases/download/latest/SHA256SUMS.txt)
- [صفحة الإصدارات وسجل البناء](https://github.com/c83491307-gif/Ultimate-keyboard-/releases)

**تنبيه:** إصدار `latest` هو Debug للاختبار وليس إصدار إنتاج موقّعًا للنشر على Google Play. النسخة الحالية تحتاج اختبار بناء وتثبيت بعد تغييرات التخزين المحلي. قد يتطلب تثبيته تفعيل السماح بالتثبيت من المصدر الذي نزّلت منه الملف. إذا توقف تنزيل APK عند اكتماله، جرّب ZIP البديل ثم فك الضغط وتحقق من SHA-256 عند الحاجة.

### حالة النشر الإنتاجي

إعداد البناء يدعم استهداف Android 16 / API 36، وتشغيل اختبارات الوحدة وAndroid Lint، وتجهيز Android App Bundle موقّع عند إنشاء وسم إصدار مثل `v1.0.0`. يتطلب ذلك إعداد أسرار التوقيع في GitHub Actions ومراجعة التطبيق على أجهزة حقيقية. نجاح CI لا يعني تلقائيًا قبول التطبيق في المتجر.

قبل النشر، راجع [مسودة سياسة الخصوصية](PRIVACY_POLICY.md) و[قائمة جاهزية المتاجر](docs/STORE_RELEASE_CHECKLIST.md). يجب استبدال بيانات التواصل التجريبية، وإكمال إقرارات Data safety ومحتوى التطبيق ومواد صفحة المتجر قبل الإرسال.

### المتطلبات المستهدفة

| العنصر | الهدف |
|---|---|
| Android | Android 8.0 / API 26 أو أحدث |
| الذاكرة | 3GB كمرجع استرشادي؛ 4GB أو أكثر أفضل لبعض المميزات |
| المساحة | تتحدد وفق القواميس ونماذج OCR والصوت المضمنة |
| المعمارية | ARM64 أولاً؛ تُختبر ARMv7 وx86_64 قبل إعلان دعمها |

---

## ⚙️ الإعدادات المستهدفة

- 🎨 **المظهر:** الثيمات والألوان والأشكال والخطوط.
- ⌨️ **الكتابة:** التصحيح والتوقع والإيماءات واللغات.
- 📋 **الحافظة:** مدة الاحتفاظ والتثبيت والاستثناءات.
- 🔍 **OCR:** اللغة والدقة ومصدر الصور.
- 🌐 **Universal Copy:** الخدمات الاختيارية وإدارة صلاحياتها.
- 🧠 **الأدوات الذكية:** اختيار المحرك المحلي أو المزود الخارجي.
- 🔒 **الخصوصية:** القفل ووضع التخفي والتحكم في البيانات.
- 💾 **النسخ الاحتياطي:** التصدير والاستيراد بعد اكتمال التنفيذ.

---

## 🏗️ البناء من المصدر

### المتطلبات المستهدفة

- إصدار حديث ومتوافق من Android Studio.
- JDK 17 إذا تطابق مع إعدادات Gradle.
- Android SDK المتوافق مع إعدادات المشروع.

بعد إضافة ملفات المشروع الفعلية إلى المستودع، ستكون خطوات البناء المتوقعة:

`bash
git clone https://github.com/c83491307-gif/Ultimate-keyboard-.git
cd Ultimate-keyboard-
./gradlew assembleDebug
`

المسار المعتاد لملف Debug بعد نجاح البناء:

`text
app/build/outputs/apk/debug/app-debug.apk
`

لا يمكن ضمان نجاح البناء قبل وجود ملفات Gradle والكود ومكونات Android SDK المطلوبة. لا تنشر إصدار Release موقّعاً قبل إعداد مفاتيح التوقيع بأمان واختبار الحزمة.

### توقيع الإصدار

- لا ترفع ملف keystore أو كلمات المرور أو مفاتيح التوقيع إلى Git.
- خزّن أسرار التوقيع في إعدادات CI الآمنة.
- انشر بصمة SHA-256 لكل APK رسمي.
- اختبر التثبيت والترقية وإزالة التطبيق على جهاز فعلي.

---

## 🧱 البنية التقنية المستهدفة

| الطبقة | التقنية المقترحة |
|---|---|
| اللغة | Kotlin + Coroutines + Flow |
| الواجهة | Jetpack Compose حيث يناسب ذلك ودعم دورة حياة خدمة الإدخال |
| محرك الكيبورد | InputMethodService |
| قاعدة البيانات | Room وFTS عند الحاجة |
| تشفير قاعدة البيانات | SQLCipher أو بديل موثوق بعد مراجعة التوافق |
| OCR | Google ML Kit أو Tesseract وفق اللغات وطريقة التوزيع |
| قراءة الشاشة | AccessibilityService اختياري |
| التنبؤ | قاموس محلي ونموذج TFLite عند توفيره |
| الصوت | محرك محلي مثل whisper.cpp بعد التحقق من الحجم والأداء |
| حقن الاعتماديات | Hilt إذا كان مناسباً لحجم المشروع |
| الإعدادات | DataStore |
| الاختبارات | JUnit واختبارات واجهة وأجهزة فعلية |
| CI/CD | GitHub Actions لبناء التطبيق وتشغيل الاختبارات |

### هيكل المشروع المقترح

`text
NovaKeys/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/                 # Kotlin
│       ├── res/                  # Android resources
│       └── assets/               # Licensed local assets
├── docs/
├── gradle/
├── .github/workflows/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── LICENSE
└── README.md
`

قد يتغير الهيكل النهائي وفق تقسيم الوحدات بعد تنفيذ المشروع.

---

## 🚀 الأداء والتحقق

الأرقام التالية أهداف قياس وليست نتائج مثبتة:

| المقياس | الهدف |
|---|---|
| ظهور لوحة المفاتيح | أقل من 150ms على جهاز مرجعي محدد |
| الاستجابة للضغطات | سلاسة قريبة من 60fps |
| استهلاك الذاكرة | قياسه أثناء الكتابة وفتح الحافظة وOCR |
| البطارية | اختبار الاستخدام الواقعي والخلفية |
| OCR | قياس لكل لغة وحجم صورة |
| البحث | قياس على أحجام قواعد بيانات معلنة |

يجب نشر نتائج القياس مع نوع الجهاز وإصدار أندرويد وحجم الاختبار، وعدم عرض الأهداف كأنها نتائج فعلية.

---

## 🗺️ خارطة الطريق

- [ ] إعداد مشروع Android قابل للبناء وCI.
- [ ] تنفيذ InputMethodService ولوحة عربية/إنجليزية أساسية.
- [ ] إعداد شاشة الإعدادات واختيار الكيبورد من إعدادات أندرويد.
- [ ] تنفيذ الحافظة المحلية مع احترام قيود النظام.
- [ ] إضافة الثيمات وتخصيص التخطيط.
- [ ] إضافة OCR محلي واختبارات للغات المستهدفة.
- [ ] إضافة Universal Copy اختيارياً مع شرح الصلاحيات.
- [ ] إضافة قفل الحافظة والتشفير بعد مراجعة أمنية.
- [ ] إضافة النسخ الاحتياطي المشفر.
- [ ] إضافة أدوات النص والحساب والترجمة.
- [ ] تقييم الكتابة الصوتية دون اتصال.
- [ ] اختبار الأداء والبطارية وإمكانية الوصول.
- [ ] إصدار APK تجريبي موثق مع SHA-256.
- [ ] نشر إصدار مستقر بعد إكمال الاختبارات.

---

## ❓ الأسئلة الشائعة

<details>
<summary><b>هل الحافظة غير محدودة فعلاً؟</b></summary>

الهدف ألا يفرض التطبيق حداً صغيراً ثابتاً على عدد العناصر. السعة الفعلية تعتمد على مساحة الجهاز وسياسات الاحتفاظ، كما أن قيود أندرويد تمنع ضمان التقاط كل عملية نسخ في الخلفية.
</details>

<details>
<summary><b>هل يسجل الكيبورد ما أكتبه؟</b></summary>

التصميم المستهدف محلي دون تسجيل خفي أو إرسال للنصوص. يجب مراجعة الكود والاعتماديات والأذونات قبل اعتبار ذلك ضماناً نهائياً.
</details>

<details>
<summary><b>هل Universal Copy يعمل في كل التطبيقات؟</b></summary>

لا يمكن ضمان ذلك. يعتمد على ما تكشفه التطبيقات لخدمات إمكانية الوصول، وقد يتعذر الوصول إلى المحتوى المحمي أو النصوص المرسومة داخل الصور والفيديو.
</details>

<details>
<summary><b>هل يعمل OCR دون إنترنت؟</b></summary>

يمكن أن يعمل محلياً إذا كان المحرك وحزمة اللغة المطلوبة موجودين على الجهاز. يجب توثيق أي تنزيل مطلوب مسبقاً.
</details>

<details>
<summary><b>هل يوجد APK الآن؟</b></summary>

لم يُنشر APK متحقق منه في هذا المستودع حتى الآن. راجع صفحة Releases بعد إضافة ملفات المشروع وإتمام البناء والاختبارات.
</details>

---

## 🤝 المساهمة

المساهمات مرحب بها في التنفيذ والتوثيق والاختبارات وإمكانية الوصول والترجمة.

`bash
git checkout -b feature/your-feature
git add .
git commit -m "feat: describe your change"
git push origin feature/your-feature
`

ثم افتح Pull Request مع وصف التغيير وطريقة اختباره. لا ترفع مفاتيح API أو بيانات شخصية أو ملفات توقيع.

---

## 🙏 شكر وتقدير

- [AOSP LatinIME](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/)
- [Google ML Kit](https://developers.google.com/ml-kit)
- [whisper.cpp](https://github.com/ggml-org/whisper.cpp)
- [SQLCipher](https://www.zetetic.net/sqlcipher/)
- [Unicode Emoji](https://unicode.org/emoji/)

تُراجع التراخيص وشروط الاستخدام لكل مكتبة أو نموذج أو خط قبل تضمينه.

---

## 📄 الترخيص

يُقترح ترخيص المشروع بموجب MIT. لا يصبح الترخيص نافذاً قبل إضافة ملف LICENSE يتضمن النص الصحيح والتحقق من حقوق المكونات المضمنة.

<div align="center">

**NovaKeys — كتابة أسرع، تخصيص أوسع، وخصوصية أولاً.**

</div>


---

## 🔐 Password Manager + Autofill (implementation in progress)

The repository now includes an initial Kotlin/Jetpack Compose Android project scaffold and source files for the password-vault feature. This is source code, not a verified release: the repository has **not yet been confirmed to build**, and no APK should be treated as tested until CI succeeds.

### Implemented source files
- `PasswordEntry`, `PasswordDao`, and `PasswordDatabase`: Room-backed credential records and search by site.
- `DatabaseKeyManager`: creates a random 256-bit SQLCipher passphrase and wraps it with an AES-256-GCM key generated in Android Keystore.
- `NovaAutofillService`: scans autofill structure for username/password fields, proposes saved credentials for matching domains, and uses Android's save flow. It deliberately skips structures identified as payment-related.
- `NovaInputMethodService`: registers the Android input-method service. Inline suggestion rendering is **not implemented yet**; Autofill uses the Android system fallback UI.
- `PasswordTools`: cryptographically secure password generation and basic weak/reused-password checks.
- `PasswordCsv`: CSV import/export support for common Chrome and Bitwarden column names.
- `MainActivity`: Compose vault UI with search, add/edit/delete, folders, CSV pickers, password generation, and shortcuts to Android's keyboard/autofill settings.
- `.github/workflows/android.yml`: CI workflow to build a debug APK and run unit tests.

### Build and verify
The project uses Android SDK 35, JDK 17, Gradle 8.9, Kotlin 2.0.21, Room 2.6.1, and SQLCipher for Android 4.6.1. Open the repository in Android Studio or run `gradle assembleDebug testDebugUnitTest` in an environment with Android SDK 35 installed. The GitHub Actions workflow is intended to provide a repeatable build artifact after a successful run.

### Important security and platform limits
- Autofill requires the user to explicitly enable NovaKeys under Android Settings → Passwords, passkeys & data / Autofill service. IME activation is a separate user-controlled setting.
- Android/system UI controls whether an app can receive inline suggestions. The IME request hook does not guarantee inline chips in every browser or app; the Autofill framework may use its fallback UI.
- The service only sees fields exposed by the host app's Autofill framework. Some browsers and custom login forms do not expose usable field metadata.
- Payment-field detection is a conservative heuristic, not a formal guarantee that every payment form is recognized.
- CSV files contain plaintext passwords. Export only to a location you control, and delete the export securely when no longer needed. Imported CSV rows are also plaintext while the file exists.
- The vault database is encrypted at rest with SQLCipher; its passphrase is wrapped using an AES-256-GCM Android Keystore key. Device compromise, rooted devices, screenshots, process-memory access, and app-level authorization remain separate threat considerations.
- The vault screen now requires Android biometric or device-credential authentication; a short five-minute session marker also gates Autofill reads/saves. This is an app-level convenience gate, not a hardware-backed per-secret authentication transaction, and it still requires a security review and device testing.
- Inline suggestions are not yet implemented: the IME currently exposes a keyboard-service scaffold only. A production inline renderer using Android `InlinePresentation` and device/browser compatibility tests remain outstanding.
- Domain matching is normalized for common URL/`www.` differences but is not a full public-suffix or subdomain policy engine. Do not rely on this as your only password manager until these gaps are independently tested.
- The application requests no Internet permission; CSV file access uses Android's Storage Access Framework. Dependencies are resolved during development/CI, not by the installed app.

### Current status
- [x] Initial Android project structure and manifest declarations
- [x] Encrypted Room/SQLCipher database and Keystore-wrapped database key
- [x] Initial AutofillService and IME service declaration
- [ ] InlineSuggestionsRequest + InlinePresentation rendering and compatibility tests
- [x] Local password generator and basic weak/reused detection
- [x] CSV parsing/export utility and Compose management UI
- [x] CI workflow and initial unit tests
- [x] Successful CI build and debug APK artifact uploaded (device installation remains unverified)
- [ ] Inline presentations verified in supported browsers and apps
- [x] Biometric/device-credential gate for vault UI and five-minute Autofill session gate
- [ ] Security review, per-dataset Autofill authentication, and robust CSV edge-case tests



## Latest implementation pass (2026-10-29)

- Added biometric/device-credential authentication before showing the vault screen; successful authentication opens a five-minute local session.
- Autofill lookup and save requests now require that recent authenticated session, normalize common domain variants, and skip payment-related forms when detected.
- Added a weak/reused password audit summary and a warning before exporting plaintext CSV.
- Added Android CI and initial unit tests. CI run #37 completed successfully: `assembleDebug` and `testDebugUnitTest` passed, and the `novakeys-debug-apk` artifact was uploaded. Physical-device installation and runtime behavior remain unverified.

- Fixed initial CI compile blockers: the vault activity now extends `FragmentActivity` for AndroidX BiometricPrompt, and unsupported inline-presentation imports were removed until a real renderer is implemented.


### Keyboard setup and drawing panel (2026-10-29)

- Added a setup card in the app that detects whether NovaKeys is enabled and selected as the current default keyboard, with shortcuts to Android input-method settings and the system keyboard picker.
- Added an offline finger-drawing panel to the keyboard toolbar, including Undo, Clear, Cancel, PNG save, and an attempt to insert the saved image into the receiving app.
- Drawing/image tools remain hidden for sensitive/password input fields. Whether image insertion works depends on the target app accepting rich-content input; the PNG is saved locally either way.
- Added unit tests for enabled/default keyboard status detection. CI run: https://github.com/c83491307-gif/Ultimate-keyboard-/actions/runs/40846685204 (success; APK artifact uploaded). This confirms compilation and unit tests, not physical-device UX testing.
