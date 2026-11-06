# מעורר משימה – שעון מעורר מבוסס משימות פיזיות

אפליקציית אנדרואיד נייטיב (Kotlin + Jetpack Compose) של שעון מעורר "קשוח":
השעון לא נכבה עד שמשלימים משימה שבוחרים לכל שעון:

| משימה | מה צריך לעשות | הגדרות |
|---|---|---|
| 📷 סריקת ברקוד | לקום ולסרוק מוצר שנשמר מראש (למשל קופסת החלב במקרר) | ברקוד יעד |
| 🚶 צעדים | ללכת עם הטלפון | 10–500 צעדים |
| 📳 טלטול | לנער את הטלפון בחוזקה | 10–300 ניעורים |
| ➗ תרגילי חשבון | לפתור תרגילים במקלדת מספרים | 1–10 תרגילים, קל / בינוני / קשה |
| 🧩 משחק זיכרון | לחזור על רצף משבצות מהבהבות (הרצף מתארך בכל סבב) | 1–6 סבבים |
| ⌨️ הקלדת משפט | להקליד משפט בדיוק (המשפט לא ניתן להעתקה) | 1–5 משפטים |

במסך ההגדרה יש כפתור "נסה את המשימה עכשיו" לתרגול לפני הדריכה.
גם **כיבוי, עריכה או מחיקה של שעון דרוך** דורשים לבצע את המשימה באותו רגע, כך שאי אפשר "לבטל מראש" בערב.

## יכולות

| תחום | מימוש |
|---|---|
| תזמון אמין | `AlarmManager.setAlarmClock()` – מדויק, פטור מ-Doze, מציג אייקון שעון בשורת המצב |
| צלצול ברקע / מסך נעול | `AlarmService` – Foreground Service מסוג `mediaPlayback` עם wake lock |
| הקפצת מסך | ערוץ התראות `IMPORTANCE_HIGH` + Full-screen intent ל-`AlarmActivity` (`showWhenLocked`, `turnScreenOn`) |
| סריקת ברקוד | CameraX + ML Kit Barcode Scanning (מודל מקומי מובנה, עובד בלי אינטרנט), כולל פנס |
| משימת טלטול | חיישן Accelerometer (`SensorManager`), סף 2.1g, ספירה עם השהייה בין ניעורים |
| משימת צעדים | `TYPE_STEP_DETECTOR` (עם הרשאת פעילות גופנית), ואם אין – זיהוי צעדים מחיישן התאוצה |
| חשבון / זיכרון / הקלדה | Compose בלבד, ללא הרשאות; התרגילים והמשפטים נוצרים ב-`data/TaskContent.kt` |
| שמירת נתונים | Room (SQLite) ב-Device-Protected Storage – השעונים נדרכים מחדש ומצלצלים גם אחרי אתחול לפני פתיחת הנעילה |
| הישרדות | דריכה מחדש אחרי אתחול, עדכון אפליקציה, שינוי שעה/אזור זמן ושינוי הרשאת שעונים מדויקים |
| צלילים | 3 צלילים מובנים שמסונתזים בקוד (סירנה, צפצוף, מנגינה), צליל המערכת או כל צליל מהמכשיר (Ringtone picker) |
| עיצוב | Material 3, עברית מלאה, RTL כפוי, ערכות כהה/בהירה, Edge-to-edge |

### מנגנון ההגנה (Anti-Cheat)
* שעון **דרוך** (מתג פעיל) מסומן במנעול. כיבוי המתג, לחיצה לעריכה או מחיקה מעבירים ל"מסך אתגר" – הפעולה מתבצעת רק אחרי סריקת הברקוד / הטלטול.
* דריכה של שעון כבוי – חופשית.
* בזמן צלצול: כפתור Back חסום, כפתורי עוצמה "נבלעים" במסך האזעקה, ועוצמת ערוץ ה-Alarm ננעלת (מוחזרת אוטומטית כל 1.5 שניות אם הורידו אותה).
* אין כפתור "כבה" או "נודניק" בהתראה. אם ההתראה מוחלקת (אפשרי באנדרואיד 14+) היא מוצגת מחדש. פתיחת האפליקציה בזמן צלצול מחזירה למסך האזעקה.
* משימת צעדים במכשיר בלי שום חיישן תנועה עוברת אוטומטית לתרגילי חשבון, כך שתמיד אפשר לכבות.
* אם התהליך נהרג בזמן צלצול, השירות חוזר לצלצל (`START_REDELIVER_INTENT`).
* **משימת חירום** (רק במשימת ברקוד): אם אין גישה לברקוד (המוצר נגמר/אבד) אפשר לבצע 150 ניעורים במקום. זה מונע מצב שבו השעון לא ניתן לכיבוי לעולם – חשוב גם לבטיחות וגם לעמידה במדיניות Google Play.

### מגבלות שחשוב לדעת
אנדרואיד לא מאפשר לאפליקציה רגילה (ובטח לא אפליקציה ב-Google Play) למנוע הסרה, "עצירה בכוח" (Force stop) או כיבוי של המכשיר. Force stop מבטל את השעונים שנקבעו עד שהאפליקציה נפתחת שוב. ההגנה היא על כל מה שקורה **בתוך** האפליקציה.

## מבנה הקוד

```
app/src/main/java/com/taskalarm/app/
├── TaskAlarmApp.kt              # Application – יצירת ערוצי התראות
├── data/                        # Room: Alarm, AlarmDao, AppDatabase (+migration), AlarmRepository, NextAlarmCalculator, Days, TaskContent
├── alarm/                       # AlarmScheduler, AlarmReceiver, BootReceiver, AlarmService,
│                                # NotificationHelper, AlarmSoundPlayer, SoundLibrary, ToneSynth
└── ui/
    ├── MainActivity.kt          # Navigation: permissions → dashboard → edit / challenge
    ├── dashboard/               # מסך הבית – שעון גדול, רשימת שעונים, FAB
    ├── edit/                    # הגדרת שעון – שעה, ימים, משימה, ברקוד יעד, צליל ועוצמה
    ├── challenge/               # מסך האתגר לפעולות על שעון דרוך
    ├── ringing/                 # AlarmActivity + RingingScreen + "בוקר טוב!"
    ├── task/                    # BarcodeScannerView, ShakeTask, StepsTask, MathTask, MemoryTask, TypingTask, TaskChallenge
    ├── permissions/             # מסך הרשאות עם הסברים + PermissionUtils
    └── theme/
```

## בנייה

דרישות: Android Studio (Narwhal ומעלה) או JDK 17 + Android SDK 36.

```bash
./gradlew testDebugUnitTest      # בדיקות יחידה
./gradlew assembleDebug          # APK לבדיקה: app/build/outputs/apk/debug/
./gradlew bundleRelease          # AAB להעלאה ל-Google Play
```

ה-workflow ב-`.github/workflows/android.yml` מריץ בדיקות, בונה APK ו-AAB ומריץ lint על כל push.

### חתימה לשחרור
צור מפתח העלאה ושמור `keystore.properties` בשורש הפרויקט (מוחרג מ-git):

```properties
storeFile=/absolute/path/upload-keystore.jks
storePassword=********
keyAlias=upload
keyPassword=********
```

```bash
keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

מומלץ להפעיל Play App Signing ב-Play Console.

## Google Play
ראו [`docs/PLAY_STORE.md`](docs/PLAY_STORE.md) – הצהרות הרשאות, טופס Data safety, דירוג תוכן ורשימת בדיקות לפני הגשה,
ואת [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) – מדיניות פרטיות שיש לפרסם בכתובת ציבורית ולקשר אליה מה-Console.

## בדיקה ידנית מומלצת
1. צור שעון לעוד 2 דקות עם ברקוד, נעל את המסך וחכה – המסך צריך להידלק ולהציג את האזעקה.
2. נסה Back, כפתורי עוצמה, החלקת ההתראה ופתיחת האפליקציה – האזעקה ממשיכה.
3. סרוק ברקוד אחר – "זה לא הברקוד הנכון". סרוק את הנכון – "בוקר טוב!".
4. נסה לכבות/למחוק/לערוך שעון דרוך – נדרש אתגר.
5. אתחל את המכשיר בלי לפתוח נעילה – השעון עדיין מצלצל בזמן.
