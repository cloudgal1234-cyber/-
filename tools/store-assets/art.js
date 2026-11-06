// Simple flat illustrations of household places (used as "photos" in the mockups).
const wrap = (bg, body, w = 120, h = 120) =>
  `<svg viewBox="0 0 120 120" width="${w}" height="${h}" xmlns="http://www.w3.org/2000/svg"><rect width="120" height="120" fill="${bg}"/>${body}</svg>`;
module.exports = {
  fridge: (w, h) => wrap('#e9dcc7', `<rect x="30" y="10" width="60" height="105" rx="8" fill="#f4f6f8" stroke="#c8ced6" stroke-width="2"/><line x1="30" y1="48" x2="90" y2="48" stroke="#c8ced6" stroke-width="2"/><rect x="78" y="20" width="4" height="20" rx="2" fill="#9aa3ad"/><rect x="78" y="56" width="4" height="30" rx="2" fill="#9aa3ad"/><rect x="40" y="60" width="16" height="12" rx="2" fill="#ff8a3d"/><circle cx="62" cy="30" r="5" fill="#4a90d9"/><rect x="0" y="112" width="120" height="8" fill="#b9a487"/>`, w, h),
  sw: (w, h) => wrap('#efe7da', `<rect x="35" y="25" width="50" height="70" rx="8" fill="#fbfbfb" stroke="#cfc6b8" stroke-width="2"/><rect x="48" y="40" width="24" height="40" rx="5" fill="#ececec" stroke="#cfc6b8" stroke-width="2"/><rect x="50" y="42" width="20" height="18" rx="4" fill="#dcdcdc"/>`, w, h),
  sink: (w, h) => wrap('#d7e3ea', `<rect x="0" y="70" width="120" height="50" fill="#8a6d52"/><rect x="0" y="66" width="120" height="10" fill="#e8e8e8"/><path d="M28 70 h64 l-6 22 h-52z" fill="#cfd6dc"/><path d="M60 66 v-34 h18 v8" stroke="#a8b0b8" stroke-width="7" fill="none" stroke-linecap="round"/>`, w, h),
  kettle: (w, h) => wrap('#f1e4d0', `<rect x="0" y="92" width="120" height="28" fill="#c9b08e"/><path d="M38 92 q-4 -40 22 -48 q26 8 22 48z" fill="#ff8a3d"/><rect x="50" y="38" width="20" height="8" rx="3" fill="#333"/><path d="M82 60 q16 4 8 26" stroke="#333" stroke-width="6" fill="none"/><path d="M38 64 l-14 -10" stroke="#ff8a3d" stroke-width="7" stroke-linecap="round"/>`, w, h),
  toilet: (w, h) => wrap('#dfe9ef', `<rect x="0" y="100" width="120" height="20" fill="#b7c4cc"/><rect x="40" y="16" width="44" height="34" rx="6" fill="#fafafa" stroke="#c6cfd6" stroke-width="2"/><ellipse cx="60" cy="66" rx="30" ry="12" fill="#fafafa" stroke="#c6cfd6" stroke-width="2"/><path d="M38 68 q4 28 22 32 q18 -4 22 -32" fill="#fafafa" stroke="#c6cfd6" stroke-width="2"/>`, w, h),
  door: (w, h) => wrap('#e7ddd0', `<rect x="32" y="10" width="56" height="110" rx="3" fill="#8b5e3c"/><rect x="40" y="20" width="40" height="38" rx="3" fill="#7a5234"/><rect x="40" y="66" width="40" height="44" rx="3" fill="#7a5234"/><circle cx="78" cy="64" r="4" fill="#e0c068"/>`, w, h),
  // Camera view: fridge in a kitchen corner, full frame
  kitchen: () => `<svg viewBox="0 0 400 300" width="100%" height="100%" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
    <rect width="400" height="300" fill="#e6d6bd"/><rect y="230" width="400" height="70" fill="#b99c76"/>
    <rect x="0" y="150" width="130" height="85" fill="#7d5a3c"/><rect x="0" y="142" width="130" height="10" fill="#d9d9d9"/>
    <rect x="300" y="150" width="100" height="85" fill="#7d5a3c"/><rect x="300" y="142" width="100" height="10" fill="#d9d9d9"/>
    <rect x="145" y="30" width="140" height="210" rx="12" fill="#f3f5f7" stroke="#c4cad2" stroke-width="3"/>
    <line x1="145" y1="110" x2="285" y2="110" stroke="#c4cad2" stroke-width="3"/>
    <rect x="262" y="48" width="8" height="44" rx="4" fill="#9aa3ad"/><rect x="262" y="125" width="8" height="70" rx="4" fill="#9aa3ad"/>
    <rect x="170" y="130" width="34" height="24" rx="3" fill="#ff8a3d"/><circle cx="214" cy="68" r="10" fill="#4a90d9"/><rect x="180" y="160" width="26" height="30" rx="3" fill="#7cc576"/>
    <circle cx="60" cy="125" r="16" fill="#3e8e41"/><rect x="52" y="125" width="16" height="20" fill="#a0522d"/></svg>`,
};
