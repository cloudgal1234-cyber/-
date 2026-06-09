'use client';

import { useState, useEffect, useRef, useCallback } from 'react';

/* ══════════════════════════════════════════════════════════════
   TYPES
══════════════════════════════════════════════════════════════ */
type GameState = 'intro' | 'scanning' | 'station' | 'victory';
type FxName = 'adventure' | 'success' | 'curious' | 'magic' | 'victory';

interface Station {
  n: number;
  riddle: string;
  fx: FxName;
  emoji: string;
  label: string;
  bg: string;
  accent: string;
}

/* ══════════════════════════════════════════════════════════════
   GAME DATA
══════════════════════════════════════════════════════════════ */
const STATIONS: Station[] = [
  {
    n: 1,
    riddle:
      'חוקרים צעירים, התחנה הראשונה שלכם נמצאת במקום שבו הספרים מספרים סודות. ' +
      'לאן כולם הולכים בבית הספר כדי לקרוא בהנאה או להשאיל ספר בשקט? ' +
      'רוצו לשם וחפשו את הכרטיס!',
    fx: 'adventure',
    emoji: '📚',
    label: 'תחנה ראשונה',
    bg: 'from-[#060e24] via-[#0e2040] to-[#0a1a38]',
    accent: '#60a5fa',
  },
  {
    n: 2,
    riddle:
      'כל הכבוד! המקום הבא הוא המקום הכי טעים ומריח טוב בבית הספר. ' +
      'יש שם סירים, קערות, מתכונים, ומכינים שם דברים טעימים לאכול. ' +
      'רוצו לשם וחפשו את הכרטיס הבא!',
    fx: 'success',
    emoji: '🍳',
    label: 'תחנה שנייה',
    bg: 'from-[#280800] via-[#451200] to-[#5a1800]',
    accent: '#fb923c',
  },
  {
    n: 3,
    riddle:
      'פיצחתם את זה! עכשיו עברו למקום שנמצא בחוץ, אבל יש בו המון צלילים ומנגינות. ' +
      'מקום שאפשר לנגן בו על כל מיני כלי נגינה מיוחדים תחת כיפת השמיים. ' +
      'חפשו את הכרטיס!',
    fx: 'curious',
    emoji: '🎵',
    label: 'תחנה שלישית',
    bg: 'from-[#15032e] via-[#25085a] to-[#2e0a6e]',
    accent: '#c084fc',
  },
  {
    n: 4,
    riddle:
      'אתם פשוט מעולים! המקום הבא הוא מגרש גדול בחוץ עם טבעות גבוהות ורשתות. ' +
      'לשם כולם הולכים כדי לכדרר, לרוץ ולקלוע סלים. ' +
      'רוצו למגרש וחפשו את הכרטיס האחרון!',
    fx: 'magic',
    emoji: '🏀',
    label: 'תחנה רביעית',
    bg: 'from-[#001b0d] via-[#003820] to-[#004827]',
    accent: '#4ade80',
  },
  {
    n: 5,
    riddle:
      'מדהים, הגעתם לסוף המשימה! המקום האחרון חבוי ממש בתוך הכיתה, ' +
      'מתחת למקום שבו המורה יושבת ומלמדת אתכם בכל יום. ' +
      'הסוד הגדול וההפתעה מחכים לכם ממש שם למטה! רוצו לבדוק ביחד!',
    fx: 'victory',
    emoji: '🏆',
    label: 'תחנה חמישית',
    bg: 'from-[#251500] via-[#432800] to-[#573200]',
    accent: '#fbbf24',
  },
];

const INTRO_TEXT =
  'שלום לכם בלשים צעירים! הגעתם למשימה הסודית של סוף השנה. ' +
  'ברחבי בית הספר חבויים כרטיסים סודיים. בכל פעם שתמצאו כרטיס, ' +
  'תסרקו אותו ותקשיבו טוב טוב לחידה שתוביל אתכם למקום הבא. ' +
  'מוכנים? צאו לדרך וחפשו את הכרטיס הראשון!';

/* ══════════════════════════════════════════════════════════════
   AUDIO ENGINE
══════════════════════════════════════════════════════════════ */
class AudioEngine {
  private actx: AudioContext | null = null;
  private bgActive = false;
  private bgNext = 0;
  private master: GainNode | null = null;

  private cx(): AudioContext {
    if (!this.actx)
      this.actx = new (window.AudioContext || (window as any).webkitAudioContext)();
    if (this.actx.state === 'suspended') this.actx.resume();
    return this.actx;
  }

  private out(): GainNode {
    const c = this.cx();
    if (!this.master) {
      this.master = c.createGain();
      this.master.gain.value = 1;
      this.master.connect(c.destination);
    }
    return this.master;
  }

  private tone(
    freq: number, t: number, dur: number, vol: number,
    shape: OscillatorType = 'sine',
  ) {
    const c = this.cx();
    const o = c.createOscillator();
    const g = c.createGain();
    o.type = shape;
    o.frequency.setValueAtTime(freq, t);
    g.gain.setValueAtTime(0.001, t);
    g.gain.linearRampToValueAtTime(vol, t + 0.015);
    g.gain.exponentialRampToValueAtTime(0.001, t + dur);
    o.connect(g);
    g.connect(this.out());
    o.start(t);
    o.stop(t + dur + 0.05);
  }

  private kick(t: number) {
    const c = this.cx();
    const o = c.createOscillator();
    const g = c.createGain();
    o.frequency.setValueAtTime(150, t);
    o.frequency.exponentialRampToValueAtTime(0.001, t + 0.18);
    g.gain.setValueAtTime(0.65, t);
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.2);
    o.connect(g);
    g.connect(this.out());
    o.start(t);
    o.stop(t + 0.22);
  }

  private snare(t: number) {
    const c = this.cx();
    const buf = c.createBuffer(1, Math.ceil(c.sampleRate * 0.2), c.sampleRate);
    const d = buf.getChannelData(0);
    for (let i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
    const src = c.createBufferSource();
    src.buffer = buf;
    const hpf = c.createBiquadFilter();
    hpf.type = 'highpass';
    hpf.frequency.value = 1800;
    const g = c.createGain();
    g.gain.setValueAtTime(0.25, t);
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.18);
    src.connect(hpf);
    hpf.connect(g);
    g.connect(this.out());
    src.start(t);
  }

  private hh(t: number, vol = 0.055) {
    const c = this.cx();
    const buf = c.createBuffer(1, Math.ceil(c.sampleRate * 0.04), c.sampleRate);
    const d = buf.getChannelData(0);
    for (let i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
    const src = c.createBufferSource();
    src.buffer = buf;
    const hpf = c.createBiquadFilter();
    hpf.type = 'highpass';
    hpf.frequency.value = 7000;
    const g = c.createGain();
    g.gain.setValueAtTime(vol, t);
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.04);
    src.connect(hpf);
    hpf.connect(g);
    g.connect(this.out());
    src.start(t);
  }

  private scheduleBar(t: number) {
    const bpm = 126;
    const b = 60 / bpm;
    // Drums
    this.kick(t);
    this.kick(t + b * 2);
    this.snare(t + b);
    this.snare(t + b * 3);
    for (let i = 0; i < 16; i++) this.hh(t + i * b * 0.25);
    // Bass groove (E2 pattern)
    ([
      [82.4, 0], [82.4, b * 0.5], [110, b], [82.4, b * 1.5],
      [98, b * 2], [82.4, b * 2.5], [123.5, b * 3], [82.4, b * 3.5],
    ] as [number, number][]).forEach(([f, o]) =>
      this.tone(f, t + o, b * 0.42, 0.28, 'triangle'),
    );
    // Detective melody
    [329.63, 392, 440, 392, 329.63, 261.63, 293.66, 329.63].forEach((f, i) =>
      this.tone(f, t + i * b * 0.5, b * 0.38, 0.09, 'square'),
    );
  }

  private tick() {
    if (!this.bgActive) return;
    const c = this.cx();
    const barLen = 4 * (60 / 126);
    while (this.bgNext < c.currentTime + 1.5) {
      this.scheduleBar(this.bgNext);
      this.bgNext += barLen;
    }
    setTimeout(() => this.tick(), 400);
  }

  startBackground() {
    this.bgActive = true;
    this.bgNext = this.cx().currentTime;
    this.tick();
  }

  stopBackground() {
    this.bgActive = false;
    const old = this.master;
    this.master = null;
    if (old) {
      const c = this.cx();
      old.gain.linearRampToValueAtTime(0, c.currentTime + 0.5);
      setTimeout(() => old.disconnect(), 600);
    }
  }

  playFx(type: FxName) {
    const c = this.cx();
    const t = c.currentTime + 0.05;

    switch (type) {
      case 'adventure':
        [261.63, 329.63, 392, 523.25].forEach((f, i) =>
          this.tone(f, t + i * 0.13, 0.38, 0.45, 'square'),
        );
        this.tone(523.25, t + 0.56, 0.65, 0.5, 'square');
        break;

      case 'success': {
        this.tone(392, t, 0.18, 0.45, 'sine');
        this.tone(523.25, t + 0.18, 0.22, 0.5, 'sine');
        this.tone(659.25, t + 0.38, 0.6, 0.55, 'sine');
        const ac = this.cx();
        const buf = ac.createBuffer(1, Math.ceil(ac.sampleRate * 1.1), ac.sampleRate);
        const d = buf.getChannelData(0);
        for (let i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
        const src = ac.createBufferSource();
        src.buffer = buf;
        const bpf = ac.createBiquadFilter();
        bpf.type = 'bandpass';
        bpf.frequency.value = 2500;
        bpf.Q.value = 0.5;
        const g = ac.createGain();
        g.gain.setValueAtTime(0.28, t + 0.45);
        g.gain.exponentialRampToValueAtTime(0.001, t + 1.55);
        src.connect(bpf);
        bpf.connect(g);
        g.connect(this.out());
        src.start(t + 0.45);
        break;
      }

      case 'curious':
        [293.66, 369.99, 440, 554.37, 659.25].forEach((f, i) =>
          this.tone(f, t + i * 0.1, 0.35, 0.3, 'sine'),
        );
        break;

      case 'magic':
        [1046.5, 1318.5, 1567.98, 2093, 2637].forEach((f, i) => {
          this.tone(f, t + i * 0.1, 0.55, 0.22, 'sine');
          this.tone(f * 2, t + i * 0.1 + 0.04, 0.22, 0.07, 'sine');
        });
        break;

      case 'victory':
        [261.63, 329.63, 392, 523.25, 659.25, 784, 1046.5].forEach((f, i) => {
          this.tone(f, t + i * 0.1, 0.55, 0.38, 'sawtooth');
          this.tone(f * 1.26, t + i * 0.1, 0.55, 0.16, 'sawtooth');
        });
        [523.25, 659.25, 784, 1046.5].forEach(f =>
          this.tone(f, t + 0.85, 1.5, 0.28, 'sawtooth'),
        );
        break;
    }
  }
}

/* ══════════════════════════════════════════════════════════════
   SPEECH
══════════════════════════════════════════════════════════════ */
async function say(text: string, pitch = 1.0, rate = 0.9): Promise<void> {
  return new Promise(res => {
    if (typeof window === 'undefined' || !window.speechSynthesis) { res(); return; }
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.lang = 'he-IL';
    u.pitch = pitch;
    u.rate = rate;
    u.volume = 1;

    const setVoice = () => {
      const v = speechSynthesis.getVoices().find(v => v.lang.startsWith('he'));
      if (v) u.voice = v;
    };
    setVoice();
    speechSynthesis.onvoiceschanged = setVoice;

    // Chrome bug: synthesis stops after ~15s without this keepalive
    const id = setInterval(() => {
      if (!speechSynthesis.speaking) { clearInterval(id); return; }
      speechSynthesis.pause();
      speechSynthesis.resume();
    }, 12000);

    u.onend = () => { clearInterval(id); res(); };
    u.onerror = () => { clearInterval(id); res(); };
    speechSynthesis.speak(u);
  });
}

/* ══════════════════════════════════════════════════════════════
   NFC UTILITIES
══════════════════════════════════════════════════════════════ */
function stationFromNDEF(msg: any): number | null {
  for (const r of msg?.records ?? []) {
    if (r.recordType === 'text') {
      try {
        const n = parseInt(
          new TextDecoder(r.encoding ?? 'utf-8').decode(r.data).trim(),
          10,
        );
        if (n >= 1 && n <= 5) return n;
      } catch { /* empty */ }
    }
  }
  return null;
}

const loadMap = (): Record<string, number> => {
  try { return JSON.parse(localStorage.getItem('nfc_station_map') ?? '{}'); }
  catch { return {}; }
};
const saveMap = (m: Record<string, number>) =>
  localStorage.setItem('nfc_station_map', JSON.stringify(m));

/* ══════════════════════════════════════════════════════════════
   STAR FIELD (deterministic, avoids hydration mismatch)
══════════════════════════════════════════════════════════════ */
const STARS = Array.from({ length: 55 }, (_, i) => ({
  id: i,
  sz: 1 + (i * 1.7) % 2.5,
  top: (i * 13 + 7) % 100,
  left: (i * 23 + 11) % 100,
  op: 0.08 + ((i * 7) % 7) / 10,
  dur: 2 + (i % 3),
  del: (i * 0.31) % 3,
}));

function Stars() {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden">
      {STARS.map(s => (
        <span
          key={s.id}
          className="absolute rounded-full bg-white"
          style={{
            width: s.sz,
            height: s.sz,
            top: `${s.top}%`,
            left: `${s.left}%`,
            opacity: s.op,
            animation: `nfc-twinkle ${s.dur}s ease-in-out infinite`,
            animationDelay: `${s.del}s`,
          }}
        />
      ))}
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   INTRO SCREEN
══════════════════════════════════════════════════════════════ */
function IntroScreen({
  onStart,
  onTitleTap,
}: {
  onStart: () => Promise<void>;
  onTitleTap: () => void;
}) {
  const [busy, setBusy] = useState(false);

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-gray-950 via-blue-950 to-gray-950 px-6 text-center">
      <Stars />

      <div className="relative z-10 flex flex-col items-center gap-8">
        <div className="animate-bounce text-8xl drop-shadow-2xl">🔍</div>

        {/* 7 taps → admin */}
        <button
          onClick={onTitleTap}
          className="cursor-default select-none focus:outline-none"
          aria-label="כותרת"
        >
          <h1 className="text-4xl font-extrabold leading-snug text-white drop-shadow-lg sm:text-5xl">
            משחק הבלשים
            <br />
            <span className="text-yellow-400">הסודי</span>
          </h1>
        </button>

        <p className="max-w-xs text-lg text-blue-200">
          משימת סוף שנה מיוחדת — לבלשים צעירים בלבד! 🌟
        </p>

        <button
          onClick={async () => {
            if (busy) return;
            setBusy(true);
            await onStart();
          }}
          disabled={busy}
          className={[
            'mt-2 rounded-2xl bg-gradient-to-br from-yellow-400 to-orange-500',
            'px-10 py-5 text-2xl font-bold text-white shadow-2xl shadow-orange-600/40',
            'transition-all active:scale-95',
            busy
              ? 'animate-pulse opacity-80'
              : 'hover:scale-105 hover:shadow-orange-500/60',
          ].join(' ')}
        >
          {busy ? '⏳ מכין משימה...' : '🚀 התחל משימה!'}
        </button>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   SCANNING SCREEN
══════════════════════════════════════════════════════════════ */
function ScanningScreen({
  nfcOk,
  nfcErr,
  onManual,
}: {
  nfcOk: boolean | null;
  nfcErr: string | null;
  onManual: (n: number) => void;
}) {
  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-gray-950 via-indigo-950 to-gray-950 px-6 text-center">
      <Stars />

      <div className="relative z-10 flex flex-col items-center gap-7">
        {/* Pulsing rings */}
        <div className="relative flex h-32 w-32 items-center justify-center">
          {[1, 2, 3].map(i => (
            <span
              key={i}
              className="absolute rounded-full border-2 border-blue-400/40"
              style={{
                inset: 0,
                animation: `nfc-waveOut 2s ease-out infinite`,
                animationDelay: `${i * 0.55}s`,
                transform: `scale(${1 + i * 0.45})`,
              }}
            />
          ))}
          <span className="text-6xl">📱</span>
        </div>

        <h2 className="text-3xl font-bold text-white">מחפשים כרטיס סודי...</h2>

        <p className="text-lg text-blue-300">
          {nfcOk === false
            ? 'מכשיר זה אינו תומך ב-NFC — השתמשו במצב הדגמה'
            : 'קרבו כרטיס NFC למכשיר'}
        </p>

        {nfcErr && (
          <div className="max-w-xs rounded-xl border border-red-500/40 bg-red-900/40 px-4 py-3 text-sm text-red-200">
            {nfcErr}
          </div>
        )}

        {/* Demo / fallback station buttons */}
        {(nfcOk === false || nfcErr) && (
          <div className="mt-2 w-full max-w-xs">
            <p className="mb-3 text-sm text-gray-400">בחר תחנה ידנית:</p>
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-5">
              {STATIONS.map(s => (
                <button
                  key={s.n}
                  onClick={() => onManual(s.n)}
                  className="rounded-xl border border-white/20 bg-white/10 p-3 text-white transition hover:bg-white/20 active:scale-95"
                >
                  <div className="text-2xl">{s.emoji}</div>
                  <div className="mt-1 text-xs">{s.n}</div>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   STATION SCREEN
══════════════════════════════════════════════════════════════ */
function AudioBars() {
  return (
    <span className="flex h-6 items-end gap-0.5">
      {[0, 1, 2, 3].map(i => (
        <span
          key={i}
          className="w-1.5 rounded-sm bg-white"
          style={{
            height: 8,
            animation: `nfc-barBounce 0.6s ease-in-out infinite`,
            animationDelay: `${i * 0.13}s`,
          }}
        />
      ))}
    </span>
  );
}

function StationScreen({
  station,
  playing,
  onPlay,
  onBack,
}: {
  station: Station;
  playing: boolean;
  onPlay: () => void;
  onBack: () => void;
}) {
  return (
    <div
      className={`relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b ${station.bg} px-6 text-center`}
    >
      <Stars />

      {/* Glow behind emoji */}
      <div
        className="pointer-events-none absolute left-1/2 top-1/3 h-64 w-64 -translate-x-1/2 -translate-y-1/2 rounded-full opacity-20 blur-3xl"
        style={{ background: station.accent }}
      />

      <div className="relative z-10 flex flex-col items-center gap-8">
        <div className="animate-bounce text-9xl drop-shadow-2xl">{station.emoji}</div>

        <div>
          <p className="text-base text-white/55">{station.label}</p>
          <h2 className="mt-1 text-5xl font-extrabold text-white drop-shadow-lg">
            🎉 מצאתם!
          </h2>
        </div>

        <button
          onClick={onPlay}
          disabled={playing}
          className={[
            'flex items-center gap-3 rounded-3xl',
            'border-2 border-white/30 bg-white/20 backdrop-blur-sm',
            'px-8 py-6 text-xl font-bold text-white shadow-2xl',
            'transition-all active:scale-95',
            playing
              ? 'animate-pulse'
              : 'hover:scale-105 hover:border-white/50 hover:bg-white/30',
          ].join(' ')}
        >
          {playing ? (
            <>
              <AudioBars />
              <span>מנגן חידה סודית...</span>
            </>
          ) : (
            '🔊 השמע חידה סודית'
          )}
        </button>

        <button
          onClick={onBack}
          className="text-sm text-white/35 transition hover:text-white/65"
        >
          ← חזרה לסריקה
        </button>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   VICTORY SCREEN
══════════════════════════════════════════════════════════════ */
const CONFETTI_PIECES = Array.from({ length: 40 }, (_, i) => ({
  id: i,
  color: ['#fbbf24', '#f87171', '#60a5fa', '#34d399', '#a78bfa', '#fb923c'][i % 6],
  top: (i * 11 + 5) % 100,
  left: (i * 17 + 3) % 100,
  dur: 2.2 + (i % 3) * 0.7,
  del: (i * 0.19) % 2.5,
  rot: (i * 37) % 360,
}));

function VictoryScreen({ onRestart }: { onRestart: () => void }) {
  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-gradient-to-b from-yellow-950 via-amber-900 to-orange-950 px-6 text-center">
      {CONFETTI_PIECES.map(c => (
        <span
          key={c.id}
          className="pointer-events-none absolute h-3 w-3 rounded-sm"
          style={{
            background: c.color,
            top: `${c.top}%`,
            left: `${c.left}%`,
            transform: `rotate(${c.rot}deg)`,
            animation: `nfc-confetti ${c.dur}s linear infinite`,
            animationDelay: `${c.del}s`,
          }}
        />
      ))}

      <div className="relative z-10 flex flex-col items-center gap-8">
        <div
          className="text-9xl"
          style={{ animation: 'nfc-trophy 1s ease-in-out infinite alternate' }}
        >
          🏆
        </div>

        <div>
          <h2 className="text-5xl font-extrabold text-white drop-shadow-lg">כל הכבוד!</h2>
          <p className="mt-3 text-xl text-yellow-300">השלמתם את משימת הבלשים!</p>
          <p className="mt-2 text-white/55">אתם בלשים מעולים מדרגה ראשונה 🌟</p>
        </div>

        <div className="flex flex-wrap justify-center gap-3 text-4xl">
          {['⭐', '🎊', '🌟', '🎉', '✨', '🥇'].map((e, i) => (
            <span
              key={i}
              className="animate-bounce"
              style={{ animationDelay: `${i * 0.1}s` }}
            >
              {e}
            </span>
          ))}
        </div>

        <button
          onClick={onRestart}
          className="mt-2 rounded-2xl border border-white/30 bg-white/20 px-8 py-4 text-lg font-bold text-white transition hover:bg-white/30 active:scale-95"
        >
          🔄 משחק חדש
        </button>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   ADMIN PANEL  (7 taps on title → opens)
══════════════════════════════════════════════════════════════ */
function AdminPanel({
  onClose,
  onManual,
  nfcOk,
}: {
  onClose: () => void;
  onManual: (n: number) => void;
  nfcOk: boolean | null;
}) {
  const [map, setMap] = useState<Record<string, number>>(loadMap);
  const [scanning, setScanning] = useState(false);
  const [pending, setPending] = useState<string | null>(null);

  const scanForTag = async () => {
    if (!nfcOk) return;
    setScanning(true);
    try {
      const r = new (window as any).NDEFReader();
      await r.scan();
      r.addEventListener(
        'reading',
        (e: any) => { setPending(e.serialNumber); setScanning(false); },
        { once: true },
      );
    } catch { setScanning(false); }
  };

  const assign = (num: number) => {
    if (!pending) return;
    const m = { ...map, [pending]: num };
    setMap(m);
    saveMap(m);
    setPending(null);
  };

  const remove = (serial: string) => {
    const m = { ...map };
    delete m[serial];
    setMap(m);
    saveMap(m);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/80">
      <div
        className="w-full max-w-lg overflow-y-auto rounded-t-3xl bg-gray-900 p-6"
        style={{ maxHeight: '88vh' }}
      >
        <div className="mb-6 flex items-center justify-between">
          <h3 className="text-xl font-bold text-white">⚙️ הגדרות מנהל</h3>
          <button onClick={onClose} className="text-2xl text-gray-400 hover:text-white">✕</button>
        </div>

        {/* Demo buttons */}
        <section className="mb-6">
          <p className="mb-3 font-semibold text-gray-300">🎮 מצב הדגמה — עיין בתחנה:</p>
          <div className="grid grid-cols-5 gap-2">
            {STATIONS.map(s => (
              <button
                key={s.n}
                onClick={() => { onManual(s.n); onClose(); }}
                className="rounded-xl border border-white/20 bg-white/10 p-3 text-center text-white transition hover:bg-white/20 active:scale-95"
              >
                <div className="text-2xl">{s.emoji}</div>
                <div className="mt-1 text-xs">{s.n}</div>
              </button>
            ))}
          </div>
        </section>

        {/* NFC serial → station mapping */}
        {nfcOk && (
          <section>
            <p className="mb-3 font-semibold text-gray-300">📡 מיפוי כרטיסי NFC לתחנות:</p>

            {Object.keys(map).length === 0 && (
              <p className="mb-3 text-sm text-gray-500">אין מיפויים. סרוק כרטיס להוספה.</p>
            )}

            {Object.entries(map).map(([serial, num]) => {
              const st = STATIONS.find(s => s.n === num);
              return (
                <div
                  key={serial}
                  className="mb-2 flex items-center justify-between rounded-xl bg-white/5 px-3 py-2"
                >
                  <span className="font-mono text-xs text-gray-400">
                    …{serial.slice(-10)}
                  </span>
                  <span className="text-white">{st?.emoji} {st?.label}</span>
                  <button
                    onClick={() => remove(serial)}
                    className="text-sm text-red-400 hover:text-red-300"
                  >
                    הסר
                  </button>
                </div>
              );
            })}

            <button
              onClick={scanForTag}
              disabled={scanning}
              className="mt-2 w-full rounded-xl bg-blue-800 py-3 font-bold text-white transition hover:bg-blue-700 disabled:opacity-50"
            >
              {scanning ? '📡 ממתין לכרטיס...' : '+ סרוק כרטיס חדש'}
            </button>

            {pending && (
              <div className="mt-3 rounded-xl border border-green-500/30 bg-green-900/30 p-3">
                <p className="mb-2 text-sm text-green-300">
                  ✓ כרטיס זוהה: …{pending.slice(-10)}
                </p>
                <p className="mb-2 text-xs text-gray-400">הקצה לתחנה:</p>
                <div className="flex flex-wrap gap-2">
                  {STATIONS.map(s => (
                    <button
                      key={s.n}
                      onClick={() => assign(s.n)}
                      className="rounded-lg border border-white/20 bg-white/15 px-3 py-1 text-sm text-white transition hover:bg-white/25"
                    >
                      {s.emoji} {s.n}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </section>
        )}

        <p className="mt-6 text-xs text-gray-600">
          כרטיסים עם רשומת טקסט NDEF המכילה ספרה 1–5 מזוהים אוטומטית.
        </p>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   MAIN PAGE COMPONENT
══════════════════════════════════════════════════════════════ */
export default function NFCDetectivePage() {
  const [state, setState] = useState<GameState>('intro');
  const [station, setStation] = useState<Station | null>(null);
  const [playing, setPlaying] = useState(false);
  const [nfcOk, setNfcOk] = useState<boolean | null>(null);
  const [nfcErr, setNfcErr] = useState<string | null>(null);
  const [showAdmin, setShowAdmin] = useState(false);
  const [taps, setTaps] = useState(0);

  const engRef = useRef<AudioEngine | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const audio = useCallback((): AudioEngine => {
    if (!engRef.current) engRef.current = new AudioEngine();
    return engRef.current;
  }, []);

  useEffect(() => {
    setNfcOk('NDEFReader' in window);
  }, []);

  // Activate a station (scan or manual)
  const activateStation = useCallback(
    (s: Station) => {
      abortRef.current?.abort();
      window.speechSynthesis?.cancel();
      setPlaying(false);
      setStation(s);
      setState('station');
      audio().stopBackground();
      audio().playFx(s.fx);
    },
    [audio],
  );

  // Start NFC scanning
  const startNFC = useCallback(async () => {
    if (!('NDEFReader' in window)) return;
    abortRef.current?.abort();
    const ctrl = new AbortController();
    abortRef.current = ctrl;
    try {
      const reader = new (window as any).NDEFReader();
      await reader.scan({ signal: ctrl.signal });
      setNfcErr(null);
      reader.addEventListener('reading', (e: any) => {
        const fromNDEF = stationFromNDEF(e.message);
        const fromMap = loadMap()[e.serialNumber] ?? null;
        const num = fromNDEF ?? fromMap;
        if (num) {
          const found = STATIONS.find(s => s.n === num);
          if (found) activateStation(found);
        }
      });
      reader.addEventListener('readingerror', () =>
        setNfcErr('שגיאה בקריאת הכרטיס — נסו שוב'),
      );
    } catch (err: any) {
      if (err?.name === 'AbortError') return;
      setNfcErr(
        err?.name === 'NotAllowedError'
          ? 'נדרשת הרשאת NFC — אנא אשרו בחלון הדפדפן'
          : `שגיאת NFC: ${err?.message ?? 'שגיאה לא ידועה'}`,
      );
    }
  }, [activateStation]);

  // "Start Mission" button
  const handleStart = useCallback(async () => {
    await say(INTRO_TEXT, 1.75, 1.1);
    setState('scanning');
    audio().startBackground();
    await startNFC();
  }, [audio, startNFC]);

  // "Play riddle" button
  const handlePlayRiddle = useCallback(async () => {
    if (!station || playing) return;
    setPlaying(true);
    await say(station.riddle, 1.3, 0.87);
    setPlaying(false);
    if (station.n === 5) {
      // Final station → victory after a beat
      setTimeout(() => {
        audio().playFx('victory');
        setState('victory');
      }, 700);
    }
  }, [station, playing, audio]);

  // Back to scanning (station → scan)
  const handleBackToScan = useCallback(() => {
    window.speechSynthesis?.cancel();
    setStation(null);
    setPlaying(false);
    setState('scanning');
    audio().startBackground();
    startNFC();
  }, [audio, startNFC]);

  // Restart entire game
  const handleRestart = useCallback(() => {
    window.speechSynthesis?.cancel();
    abortRef.current?.abort();
    audio().stopBackground();
    setStation(null);
    setPlaying(false);
    setNfcErr(null);
    setState('intro');
  }, [audio]);

  // Hidden admin: 7 taps on title
  const handleTitleTap = () =>
    setTaps(n => {
      if (n + 1 >= 7) { setShowAdmin(true); return 0; }
      return n + 1;
    });

  return (
    <>
      {state === 'intro' && (
        <IntroScreen onStart={handleStart} onTitleTap={handleTitleTap} />
      )}
      {state === 'scanning' && (
        <ScanningScreen
          nfcOk={nfcOk}
          nfcErr={nfcErr}
          onManual={n => {
            const s = STATIONS.find(st => st.n === n);
            if (s) activateStation(s);
          }}
        />
      )}
      {state === 'station' && station && (
        <StationScreen
          station={station}
          playing={playing}
          onPlay={handlePlayRiddle}
          onBack={handleBackToScan}
        />
      )}
      {state === 'victory' && (
        <VictoryScreen onRestart={handleRestart} />
      )}
      {showAdmin && (
        <AdminPanel
          onClose={() => setShowAdmin(false)}
          onManual={n => {
            const s = STATIONS.find(st => st.n === n);
            if (s) activateStation(s);
            setShowAdmin(false);
          }}
          nfcOk={nfcOk}
        />
      )}
    </>
  );
}
