const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const I = require('./icons');
const A = require('./art');
const css = fs.readFileSync(path.join(__dirname, 'common.css'), 'utf8');

const ic = (name, size, color) =>
  `<svg class="icon" width="${size}" height="${size}" viewBox="0 0 24 24"><path fill="${color}" d="${I[name]}"/></svg>`;
const page = (cap, sub, screen, time = '06:45') => `<!doctype html><html lang="he" dir="rtl"><head><meta charset="utf-8"><style>${css}</style></head>
<body><div class="cap"><h1>${cap}</h1><p>${sub}</p></div><div class="phone"><div class="screen"><div class="status"><span>${time}</span><span>▲ ◉ 87%</span></div>${screen}</div></div></body></html>`;

const alarmCard = (time, sub, task, on, locked) => `
<div class="card" style="display:flex;align-items:center;margin:0 36px 24px;opacity:${on ? 1 : .6}">
  <div style="flex:1">
    <div style="display:flex;align-items:center;gap:14px"><span style="font-size:84px;font-weight:800;direction:ltr">${time}</span>${locked ? ic('lock', 38, '#FF8A3D') : ''}</div>
    <div class="muted" style="font-size:28px;margin-top:-4px">${sub}</div>
    <div class="chip" style="margin-top:14px">${ic('camera', 28, '#E7EAF3')} ${task}</div>
  </div>
  <div style="display:flex;flex-direction:column;align-items:center;gap:26px"><div class="switch ${on ? '' : 'off'}"></div>${ic('del', 40, '#B8C0D6')}</div>
</div>`;

const screens = {
  '1-dashboard': page('השעון לא נכבה<br>עד ש<b>תקום מהמיטה</b>', 'אין נודניק. אין כפתור כיבוי.', `
    <div style="display:flex;justify-content:space-between;padding:10px 40px 0">${ic('info', 44, '#E7EAF3')}${ic('addphoto', 44, '#E7EAF3')}</div>
    <div style="text-align:center;margin-top:10px">
      <div style="direction:ltr;font-size:170px;font-weight:800;line-height:1">22:13<span style="font-size:56px;color:#B8C0D6;font-weight:500">:08</span></div>
      <div class="muted" style="font-size:34px;margin-top:10px">יום ראשון, 8 בנובמבר</div>
      <div style="color:#FF8A3D;font-size:34px;margin:18px 0 30px">השעון הבא יצלצל בעוד 8 שעות ו־32 דקות</div>
    </div>
    ${alarmCard('06:45', 'עבודה · ימי חול (א׳–ה׳)', 'צילום אקראי (בינוני)', true, true)}
    ${alarmCard('09:30', 'שבת', 'צלם את המקרר', true, true)}
    ${alarmCard('07:15', 'פעם אחת', 'צלם את מתג התאורה', false, false)}
    <div class="btn" style="position:absolute;left:40px;bottom:60px;padding:30px 44px;font-size:34px;display:flex;align-items:center;gap:12px">${ic('add', 40, '#2B1100')} שעון מעורר חדש</div>`, '22:13'),

  '2-ringing': page('קום ו<b>צלם את המקרר</b>', 'רק כשהמצלמה מזהה את המקום — השעון נכבה', `
    <div style="text-align:center;margin-top:30px">
      ${ic('alarm', 92, '#FF8A3D')}
      <div style="direction:ltr;font-size:150px;font-weight:800;line-height:1.05">06:45</div>
      <div style="font-size:44px;font-weight:700">עבודה</div>
      <div class="muted" style="font-size:30px;margin-top:6px">השעון לא ייכבה עד שתשלים את המשימה</div>
    </div>
    <div style="margin:40px 44px 0">
      <div style="display:flex;align-items:center;gap:22px">
        <div style="width:120px;height:120px;border-radius:22px;overflow:hidden;flex:none">${A.fridge(120, 120)}</div>
        <div><div style="font-size:42px;font-weight:800">קום וצלם את המקרר</div><div class="muted" style="font-size:30px">צלם את דלת המקרר מקרוב</div></div>
      </div>
      <div style="margin-top:28px;height:480px;border-radius:44px;overflow:hidden;position:relative;background:#000">
        ${A.kitchen()}
        <div style="position:absolute;top:22px;right:22px;width:72px;height:72px;border-radius:50%;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center">${ic('flash', 38, '#fff')}</div>
      </div>
      <div style="margin-top:28px;height:14px;border-radius:7px;background:#1C2742"><div style="width:82%;height:100%;border-radius:7px;background:#FF8A3D"></div></div>
      <div class="muted" style="text-align:center;font-size:30px;margin-top:16px">כמעט! כוון בדיוק כמו בתמונה</div>
    </div>`),

  '3-places': page('צלם פעם אחת<br>את <b>המקומות בבית</b>', 'מקרר, מתג אור, כיור, קומקום — 30 מקומות לבחירה', `
    <div style="display:flex;align-items:center;padding:16px 40px">${ic('back', 44, '#E7EAF3')}<div style="flex:1;text-align:center;font-size:38px;font-weight:700">המקומות שלי בבית</div><div style="width:44px"></div></div>
    <div style="margin:10px 36px 24px;background:#5C2A00;color:#FFDBC8;border-radius:36px;padding:28px 32px">
      <div style="font-size:32px;font-weight:800">צולמו 6 מתוך 30 מקומות</div>
      <div style="font-size:26px;margin-top:6px">כל מקום מצלמים פעמיים מזוויות קצת שונות. בבוקר השעון ייכבה רק כשתצלם שוב את אותו מקום.</div>
    </div>
    ${[['מטבח', [['fridge', 'המקרר', 1], ['kettle', 'הקומקום', 1], ['sink', 'כיור המטבח', 1]]], ['אמבטיה ושירותים', [['toilet', 'האסלה', 1], ['sink', 'כיור האמבטיה', 0]]], ['כניסה', [['door', 'דלת הכניסה', 1], ['sw', 'מתג התאורה', 1]]]].map(([room, items]) => `
      <div style="font-size:32px;font-weight:700;margin:10px 44px 14px">${room}</div>
      ${items.map(([art, name, done]) => `
        <div class="card" style="display:flex;align-items:center;gap:24px;margin:0 36px 14px;padding:18px 24px;border-radius:30px">
          <div style="width:104px;height:104px;border-radius:22px;overflow:hidden;flex:none;background:#1C2742;display:flex;align-items:center;justify-content:center">${done ? A[art](104, 104) : ic('addphoto', 44, '#B8C0D6')}</div>
          <div style="flex:1"><div style="font-size:32px;font-weight:700">${name}</div><div style="font-size:24px;color:${done ? '#2EC27E' : '#B8C0D6'}">${done ? 'צולם ✓ — הקש לצילום מחדש' : 'צלם את הכיור והברז באמבטיה'}</div></div>
          ${done ? ic('del', 38, '#B8C0D6') : ''}
        </div>`).join('')}`).join('')}`),

  '4-missions': page('<b>465 משימות</b><br>כל בוקר משהו אחר', 'מקום אחד, שניים באותו חדר, או בחדרים שונים', `
    <div style="display:flex;align-items:center;padding:16px 40px">${ic('back', 44, '#E7EAF3')}<div style="flex:1;text-align:center;font-size:38px;font-weight:700">שעון מעורר חדש</div><div style="width:44px"></div></div>
    <div style="text-align:center;direction:ltr;font-size:150px;font-weight:800;margin:10px 0 20px"><span style="background:#5C2A00;color:#FFDBC8;border-radius:28px;padding:0 24px">06</span> : <span style="background:#1F2A44;border-radius:28px;padding:0 24px">45</span></div>
    <div class="card" style="margin:0 36px 22px">
      <div style="font-size:32px;font-weight:700;margin-bottom:16px">ימי חזרה</div>
      <div style="display:flex;gap:10px;flex-wrap:wrap">${['א׳', 'ב׳', 'ג׳', 'ד׳', 'ה׳', 'ו׳', 'ש׳'].map((d, i) => `<div class="chip" style="${i < 5 ? 'background:#5C2A00;border-color:#5C2A00;color:#FFDBC8' : ''};font-size:28px">${d}</div>`).join('')}</div>
    </div>
    <div class="card" style="margin:0 36px 22px">
      <div style="font-size:32px;font-weight:700">משימת הבוקר: קום וצלם</div>
      <div class="muted" style="font-size:25px;margin:8px 0 18px">כדי לכבות את הצלצול — וגם כדי לכבות, לערוך או למחוק שעון דרוך — צריך לקום ולצלם מקום בבית.</div>
      <div style="display:flex;align-items:center;gap:16px;font-size:30px;font-weight:700"><span style="width:34px;height:34px;border-radius:50%;border:4px solid #FF8A3D;display:inline-flex;align-items:center;justify-content:center"><span style="width:16px;height:16px;border-radius:50%;background:#FF8A3D"></span></span>משימה אקראית בכל בוקר</div>
      <div style="display:flex;gap:10px;flex-wrap:wrap;margin:18px 0 6px">
        <div class="chip">הכול (21)</div><div class="chip">קל (6)</div><div class="chip" style="background:#5C2A00;border-color:#5C2A00;color:#FFDBC8">בינוני (4)</div><div class="chip">קשה (11)</div>
      </div>
      <div class="muted" style="font-size:24px">שני מקומות באותו חדר</div>
      <div style="display:flex;align-items:center;gap:16px;font-size:30px;font-weight:700;margin-top:20px"><span style="width:34px;height:34px;border-radius:50%;border:4px solid #B8C0D6"></span>משימה קבועה מהספרייה</div>
    </div>
    <div class="card" style="margin:0 36px">
      <div style="font-size:32px;font-weight:700">צליל ועוצמה</div>
      <div class="btn" style="margin-top:18px;padding:20px;font-size:28px">${ic('music', 32, '#2B1100')} בחר קובץ שמע מהטלפון (mp3 וכו')</div>
      <div style="font-size:28px;font-weight:700;margin-top:20px">עוצמה בזמן צלצול: 100% (מקסימום)</div>
    </div>`),

  '5-protected': page('אי אפשר <b>לבטל מראש</b><br>בערב', 'כיבוי, עריכה או מחיקה של שעון דרוך — רק עם המשימה', `
    <div style="display:flex;align-items:center;padding:16px 40px">${ic('back', 44, '#E7EAF3')}<div style="flex:1;text-align:center;font-size:38px;font-weight:700">כיבוי שעון דרוך</div><div style="width:44px"></div></div>
    <div style="text-align:center;margin-top:30px">${ic('lock', 120, '#FF8A3D')}
      <div style="font-size:52px;font-weight:800;margin-top:20px">השעון 06:45 דרוך ומוגן</div>
      <div class="muted" style="font-size:31px;margin:20px 60px 0">כדי לכבות אותו עליך להשלים את המשימה הפיזית עכשיו. כך אי אפשר לבטל את השעון מראש בערב ולהמשיך לישון בבוקר.</div>
    </div>
    <div style="margin:60px 44px 0">
      <div style="display:flex;align-items:center;gap:22px">
        <div style="width:120px;height:120px;border-radius:22px;overflow:hidden;flex:none">${A.sw(120, 120)}</div>
        <div><div style="font-size:42px;font-weight:800">קום וצלם את מתג התאורה</div><div class="muted" style="font-size:30px">צלם את מתג האור מקרוב</div></div>
      </div>
      <div style="margin-top:28px;height:360px;border-radius:44px;overflow:hidden">${A.sw('100%', '100%').replace('width="100%" height="100%"', 'width="100%" height="100%" preserveAspectRatio="xMidYMid slice"')}</div>
    </div>`),

  '6-morning': page('<b>בוקר טוב!</b> ☀️', 'קמת, צילמת — והיום שלך כבר התחיל', `
    <div style="text-align:center;margin-top:180px">${ic('sun', 280, '#2EC27E')}
      <div style="font-size:120px;font-weight:900;margin-top:30px">בוקר טוב!</div>
      <div class="muted" style="font-size:40px;margin-top:10px">יום ראשון, 8 בנובמבר</div>
      <div style="font-size:36px;margin:40px 80px 0">כל הכבוד — קמת ועשית את המשימה. שיהיה יום מעולה ☀️</div>
    </div>
    <div class="btn" style="position:absolute;left:50px;right:50px;bottom:110px;padding:34px;font-size:38px">סגור</div>`),
};

// App icon 512×512 (same design as the launcher icon, full-bleed square: Play applies the mask).
const iconHtml = `<!doctype html><html><head><style>html,body{margin:0;width:512px;height:512px;background:#0F1729}</style></head><body>
<svg width="512" height="512" viewBox="18 18 72 72" xmlns="http://www.w3.org/2000/svg"><rect x="0" y="0" width="108" height="108" fill="#0F1729"/>
<path fill="#FF8A3D" d="M30,40 L40,30 A6,6 0 0 0 30,28 A10,10 0 0 0 28,38 Z"/><path fill="#FF8A3D" d="M78,40 L68,30 A6,6 0 0 1 78,28 A10,10 0 0 1 80,38 Z"/>
<path fill="#FFFFFF" d="M30,56 A24,24 0 1,0 78,56 A24,24 0 1,0 30,56 Z"/>
<path fill="#0F1729" d="M34,56 A20,20 0 1,0 74,56 A20,20 0 1,0 34,56 Z"/>
<path fill="#FFFFFF" d="M48,49 L50,46 L58,46 L60,49 Z"/>
<path fill="#FFFFFF" d="M42,49 H66 A2,2 0 0 1 68,51 V64 A2,2 0 0 1 66,66 H42 A2,2 0 0 1 40,64 V51 A2,2 0 0 1 42,49 Z"/>
<path fill="#0F1729" d="M47.5,57.5 A6.5,6.5 0 1,0 60.5,57.5 A6.5,6.5 0 1,0 47.5,57.5 Z"/>
<path fill="#FF8A3D" d="M50.5,57.5 A3.5,3.5 0 1,0 57.5,57.5 A3.5,3.5 0 1,0 50.5,57.5 Z"/>
<path fill="#FF8A3D" d="M62,51.5 A1.5,1.5 0 1,0 65,51.5 A1.5,1.5 0 1,0 62,51.5 Z"/></svg></body></html>`;

// Feature graphic 1024×500
const featureHtml = `<!doctype html><html lang="he" dir="rtl"><head><meta charset="utf-8"><style>${css}
html,body{width:1024px;height:500px}
body{display:flex;align-items:center;padding:0 60px;gap:30px}
</style></head><body>
<div style="flex:1">
  <div style="font-size:78px;font-weight:900;line-height:1;white-space:nowrap">מעורר משימה</div>
  <div style="font-size:36px;font-weight:700;color:#FF8A3D;margin-top:16px;white-space:nowrap">קום וצלם — רק אז השעון נכבה</div>
  <div style="font-size:27px;color:#c9d2ea;margin-top:14px">צלם את המקרר, את מתג האור, את הכיור…<br>465 משימות בוקר</div>
</div>
<div style="display:flex;gap:18px;transform:rotate(-6deg)">
  <div style="width:150px;height:150px;border-radius:30px;overflow:hidden;box-shadow:0 20px 50px rgba(0,0,0,.5)">${A.fridge(150, 150)}</div>
  <div style="width:150px;height:150px;border-radius:30px;overflow:hidden;box-shadow:0 20px 50px rgba(0,0,0,.5);margin-top:60px">${A.sw(150, 150)}</div>
</div>
</body></html>`;

(async () => {
  const out = path.join(__dirname, 'out');
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch();
  const shoot = async (html, file, w, h) => {
    const p = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 1 });
    await p.setContent(html, { waitUntil: 'networkidle' });
    await p.evaluate(() => document.fonts.ready);
    await p.screenshot({ path: path.join(out, file), type: 'png', omitBackground: false });
    await p.close();
  };
  for (const [name, html] of Object.entries(screens)) await shoot(html, `screenshot-${name}.png`, 1080, 1920);
  await shoot(iconHtml, 'icon-512.png', 512, 512);
  await shoot(featureHtml, 'feature-graphic-1024x500.png', 1024, 500);
  await browser.close();
  console.log(fs.readdirSync(out).join('\n'));
})();
