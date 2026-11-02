/*
 * Reel Riot - the three machine hosts, drawn as SVG with separately
 * animated parts. Moods are CSS states set through data-mood:
 *   idle, happy, cheer, dance, wow, sad, focus, plus fire (Ember) and
 *   beam (Zib).
 *
 *   DJ Pina  - a pineapple DJ in neon shades (Neon Fruit Frenzy)
 *   Ember    - a chubby baby dragon (Dragon's Hoard)
 *   Zib      - a little alien flying a saucer (Cosmic Cascade)
 */
(function (root) {
  'use strict';

  const DEFS = '<svg class="mascot-defs" width="0" height="0" aria-hidden="true" focusable="false" style="position:absolute;width:0;height:0;overflow:hidden"><defs>' +
    // Pina
    '<radialGradient id="pinaBody" cx="0.38" cy="0.32" r="0.78"><stop offset="0" stop-color="#fff2a6"/><stop offset="0.35" stop-color="#ffd23f"/><stop offset="0.72" stop-color="#f2a516"/><stop offset="1" stop-color="#bf6a04"/></radialGradient>' +
    '<linearGradient id="pinaLeaf" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8dff8f"/><stop offset="0.5" stop-color="#22c55e"/><stop offset="1" stop-color="#0d6e2f"/></linearGradient>' +
    '<linearGradient id="pinaShade" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ff3fd5"/><stop offset="0.5" stop-color="#8b3dff"/><stop offset="1" stop-color="#2ee8ff"/></linearGradient>' +
    '<linearGradient id="pinaPhone" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#5ff4ff"/><stop offset="1" stop-color="#1690d0"/></linearGradient>' +
    '<linearGradient id="pinaShoe" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ff7ae8"/><stop offset="1" stop-color="#c0189e"/></linearGradient>' +
    '<clipPath id="pinaClip"><ellipse cx="100" cy="148" rx="56" ry="64"/></clipPath>' +
    // Ember
    '<radialGradient id="emBody" cx="0.4" cy="0.32" r="0.8"><stop offset="0" stop-color="#ffa36a"/><stop offset="0.45" stop-color="#ff5a2a"/><stop offset="0.8" stop-color="#d6301a"/><stop offset="1" stop-color="#9c1c0e"/></radialGradient>' +
    '<linearGradient id="emBelly" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff3cc"/><stop offset="1" stop-color="#ffc56a"/></linearGradient>' +
    '<linearGradient id="emWing" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#c46bff"/><stop offset="1" stop-color="#5e17a8"/></linearGradient>' +
    '<linearGradient id="emHorn" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fffbe6"/><stop offset="1" stop-color="#e3ad55"/></linearGradient>' +
    '<radialGradient id="emFlame" cx="0.5" cy="0.15" r="0.9"><stop offset="0" stop-color="#fffbe0"/><stop offset="0.3" stop-color="#ffe066"/><stop offset="0.62" stop-color="#ff8a1a"/><stop offset="1" stop-color="#e0300a" stop-opacity="0.85"/></radialGradient>' +
    // Zib
    '<linearGradient id="zbHull" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f3f6ff"/><stop offset="0.5" stop-color="#a9b4e2"/><stop offset="1" stop-color="#555f96"/></linearGradient>' +
    '<linearGradient id="zbRim" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffffff"/><stop offset="1" stop-color="#c3ccf2"/></linearGradient>' +
    '<radialGradient id="zbDome" cx="0.4" cy="0.3" r="0.8"><stop offset="0" stop-color="#d8f3ff" stop-opacity="0.55"/><stop offset="1" stop-color="#7ab4ff" stop-opacity="0.28"/></radialGradient>' +
    '<radialGradient id="zbSkin" cx="0.4" cy="0.3" r="0.8"><stop offset="0" stop-color="#d2ffbe"/><stop offset="0.5" stop-color="#72e46c"/><stop offset="1" stop-color="#2c963a"/></radialGradient>' +
    '<linearGradient id="zbBeam" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8affda" stop-opacity="0.75"/><stop offset="1" stop-color="#8affda" stop-opacity="0"/></linearGradient>' +
    '<linearGradient id="rkBody" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#aeb8e4"/><stop offset="0.45" stop-color="#ffffff"/><stop offset="1" stop-color="#8b96c6"/></linearGradient>' +
    '</defs></svg>';

  function star(cx, cy, r) {
    let d = '';
    for (let i = 0; i < 10; i++) {
      const a = -Math.PI / 2 + i * Math.PI / 5;
      const rr = i % 2 ? r * 0.45 : r;
      d += (i ? 'L' : 'M') + (cx + Math.cos(a) * rr).toFixed(1) + ' ' + (cy + Math.sin(a) * rr).toFixed(1);
    }
    return '<path d="' + d + 'Z" fill="#fff36b" stroke="#ff9f1a" stroke-width="2" stroke-linejoin="round"/>';
  }

  function pinaHatch() {
    let d = '';
    for (let k = -7; k <= 7; k++) {
      const c = 100 + k * 17;
      d += 'M' + (c - 92) + ' 56L' + (c + 92) + ' 240M' + (c + 92) + ' 56L' + (c - 92) + ' 240';
    }
    let dots = '';
    for (let y = 92; y <= 210; y += 17) {
      for (let x = 49; x <= 151; x += 17) {
        const ox = ((y - 92) / 17) % 2 ? 8.5 : 0;
        dots += '<circle cx="' + (x + ox) + '" cy="' + y + '" r="2.2"/>';
      }
    }
    return '<g clip-path="url(#pinaClip)"><path d="' + d + '" stroke="#b5680a" stroke-width="2.2" opacity="0.5" fill="none"/><g fill="#8f4f00" opacity="0.45">' + dots + '</g></g>';
  }

  const PINA = '<svg viewBox="0 0 200 250" class="mascot-svg" aria-hidden="true" focusable="false">' +
    '<ellipse class="m-shadow" cx="100" cy="242" rx="54" ry="7"/>' +
    '<g class="m-all">' +
      '<g class="m-legs"><rect x="77" y="198" width="13" height="32" rx="6" fill="#7a4a12"/><rect x="110" y="198" width="13" height="32" rx="6" fill="#7a4a12"/>' +
        '<ellipse cx="80" cy="232" rx="18" ry="8.5" fill="url(#pinaShoe)"/><ellipse cx="120" cy="232" rx="18" ry="8.5" fill="url(#pinaShoe)"/>' +
        '<path d="M66 236h26M108 236h26" stroke="#fff" stroke-width="2.5" stroke-linecap="round" opacity="0.85"/></g>' +
      '<g class="pina-leaves">' +
        '<path d="M100 98C80 86 60 78 40 78C60 68 86 74 100 98Z" fill="url(#pinaLeaf)" stroke="#0b5c27" stroke-width="2"/>' +
        '<path d="M100 98C120 86 140 78 160 78C140 68 114 74 100 98Z" fill="url(#pinaLeaf)" stroke="#0b5c27" stroke-width="2"/>' +
        '<path d="M100 94C88 72 82 48 66 30C88 38 98 62 100 94Z" fill="url(#pinaLeaf)" stroke="#0b5c27" stroke-width="2"/>' +
        '<path d="M100 94C112 72 118 48 134 30C112 38 102 62 100 94Z" fill="url(#pinaLeaf)" stroke="#0b5c27" stroke-width="2"/>' +
        '<path d="M100 96C91 68 93 36 100 8C107 36 109 68 100 96Z" fill="url(#pinaLeaf)" stroke="#0b5c27" stroke-width="2"/>' +
      '</g>' +
      '<g class="m-torso"><ellipse cx="100" cy="148" rx="58" ry="66" fill="url(#pinaBody)"/>' + pinaHatch() +
        '<ellipse cx="100" cy="148" rx="58" ry="66" fill="none" stroke="#a85a02" stroke-width="2.5"/></g>' +
      '<g class="m-arm-l"><path d="M52 152C38 160 30 174 32 188" stroke="#c97a06" stroke-width="10" stroke-linecap="round" fill="none"/><circle cx="32" cy="192" r="11" fill="#fff" stroke="#d4d4e6" stroke-width="2"/></g>' +
      '<g class="m-arm-r"><path d="M148 152C162 160 170 174 168 188" stroke="#c97a06" stroke-width="10" stroke-linecap="round" fill="none"/><circle cx="168" cy="192" r="11" fill="#fff" stroke="#d4d4e6" stroke-width="2"/></g>' +
      '<g class="m-face">' +
        '<g class="m-stars">' + star(79, 126, 12) + star(121, 126, 12) + '</g>' +
        '<g class="pina-shades"><rect x="57" y="115" width="39" height="25" rx="11" fill="url(#pinaShade)" stroke="#1b0b33" stroke-width="3.5"/><rect x="104" y="115" width="39" height="25" rx="11" fill="url(#pinaShade)" stroke="#1b0b33" stroke-width="3.5"/>' +
          '<rect x="94" y="122" width="12" height="5" rx="2" fill="#1b0b33"/><path d="M65 122h11M112 122h11" stroke="#fff" stroke-width="3" stroke-linecap="round" opacity="0.85"/></g>' +
        '<path d="M44 134C44 82 156 82 156 134" fill="none" stroke="#1b0b33" stroke-width="8" stroke-linecap="round"/>' +
        '<path d="M44 134C44 82 156 82 156 134" fill="none" stroke="url(#pinaPhone)" stroke-width="3.5" stroke-linecap="round"/>' +
        '<rect x="33" y="119" width="19" height="31" rx="8" fill="url(#pinaPhone)" stroke="#1b0b33" stroke-width="3"/><rect x="148" y="119" width="19" height="31" rx="8" fill="url(#pinaPhone)" stroke="#1b0b33" stroke-width="3"/>' +
        '<circle cx="42.5" cy="134.5" r="4.5" fill="#ff3fd5"/><circle cx="157.5" cy="134.5" r="4.5" fill="#ff3fd5"/>' +
        '<ellipse cx="67" cy="157" rx="9" ry="5.5" fill="#ff6fa8" opacity="0.6"/><ellipse cx="133" cy="157" rx="9" ry="5.5" fill="#ff6fa8" opacity="0.6"/>' +
        '<path class="mouth mouth-smile" d="M80 158Q100 180 120 158" fill="none" stroke="#5c2a00" stroke-width="4.5" stroke-linecap="round"/>' +
        '<g class="mouth mouth-open"><path d="M78 156Q100 198 122 156Z" fill="#5c1408"/><ellipse cx="100" cy="176" rx="11" ry="6" fill="#ff6b8a"/></g>' +
        '<ellipse class="mouth mouth-o" cx="100" cy="168" rx="8" ry="10" fill="#5c1408"/>' +
        '<path class="mouth mouth-sad" d="M84 172Q100 160 116 172" fill="none" stroke="#5c2a00" stroke-width="4.5" stroke-linecap="round"/>' +
      '</g>' +
    '</g></svg>';

  const EMBER = '<svg viewBox="0 0 220 250" class="mascot-svg" aria-hidden="true" focusable="false">' +
    '<ellipse class="m-shadow" cx="110" cy="243" rx="62" ry="7"/>' +
    '<g class="m-all">' +
      '<g class="em-tail"><path d="M150 200C184 208 206 192 204 166C203 152 192 146 186 156" fill="none" stroke="#d6301a" stroke-width="16" stroke-linecap="round"/>' +
        '<path d="M150 200C184 208 206 192 204 166" fill="none" stroke="#ff7a45" stroke-width="6" stroke-linecap="round" opacity="0.6"/><path d="M184 150L208 138L198 164Z" fill="#ffb21e" stroke="#c46a00" stroke-width="2"/></g>' +
      '<g class="em-wing-l"><path d="M80 128C56 94 22 88 8 114C22 114 26 126 24 138C36 130 44 138 46 150C56 140 66 142 78 150Z" fill="url(#emWing)" stroke="#45107a" stroke-width="3" stroke-linejoin="round"/>' +
        '<path d="M78 132L24 116M78 138L34 134M78 144L48 148" stroke="#45107a" stroke-width="2" opacity="0.6"/></g>' +
      '<g class="em-wing-r"><path d="M140 128C164 94 198 88 212 114C198 114 194 126 196 138C184 130 176 138 174 150C164 140 154 142 142 150Z" fill="url(#emWing)" stroke="#45107a" stroke-width="3" stroke-linejoin="round"/>' +
        '<path d="M142 132L196 116M142 138L186 134M142 144L172 148" stroke="#45107a" stroke-width="2" opacity="0.6"/></g>' +
      '<g class="m-legs"><ellipse cx="84" cy="228" rx="23" ry="12" fill="url(#emBody)"/><ellipse cx="136" cy="228" rx="23" ry="12" fill="url(#emBody)"/>' +
        '<path d="M68 234l4-6 4 6M80 236l4-6 4 6M92 234l4-6 4 6M124 234l4-6 4 6M136 236l4-6 4 6M148 234l4-6 4 6" fill="#fff6dc" stroke="#e0b37a" stroke-width="1"/></g>' +
      '<g class="m-torso"><ellipse cx="110" cy="178" rx="55" ry="52" fill="url(#emBody)"/><ellipse cx="110" cy="188" rx="33" ry="38" fill="url(#emBelly)"/>' +
        '<path d="M86 172Q110 179 134 172M83 187Q110 194 137 187M86 202Q110 209 134 202M92 216Q110 222 128 216" stroke="#e39a3f" stroke-width="2.5" fill="none" opacity="0.7"/></g>' +
      '<g class="m-arm-l"><path d="M68 170C56 176 52 186 56 198" stroke="#e0441e" stroke-width="12" stroke-linecap="round" fill="none"/><circle cx="57" cy="200" r="8" fill="#ff7a45"/></g>' +
      '<g class="m-arm-r"><path d="M152 170C164 176 168 186 164 198" stroke="#e0441e" stroke-width="12" stroke-linecap="round" fill="none"/><circle cx="163" cy="200" r="8" fill="#ff7a45"/></g>' +
      '<g class="m-head">' +
        '<path d="M80 64C70 46 72 32 82 20C86 36 92 48 98 60Z" fill="url(#emHorn)" stroke="#c98a3a" stroke-width="2"/>' +
        '<path d="M140 64C150 46 148 32 138 20C134 36 128 48 122 60Z" fill="url(#emHorn)" stroke="#c98a3a" stroke-width="2"/>' +
        '<path d="M62 94L42 82L62 108Z" fill="#ffb21e" stroke="#c46a00" stroke-width="2" stroke-linejoin="round"/><path d="M158 94L178 82L158 108Z" fill="#ffb21e" stroke="#c46a00" stroke-width="2" stroke-linejoin="round"/>' +
        '<ellipse cx="110" cy="98" rx="52" ry="46" fill="url(#emBody)"/>' +
        '<path d="M100 54L106 42L112 54L118 44L122 58" fill="#ffb21e" stroke="#c46a00" stroke-width="2" stroke-linejoin="round"/>' +
        '<ellipse cx="110" cy="120" rx="32" ry="21" fill="#ff8f5a"/><ellipse cx="99" cy="113" rx="3.5" ry="2.6" fill="#7a1a08"/><ellipse cx="121" cy="113" rx="3.5" ry="2.6" fill="#7a1a08"/>' +
        '<g class="m-eyes"><ellipse cx="88" cy="90" rx="14" ry="16" fill="#fff"/><ellipse cx="132" cy="90" rx="14" ry="16" fill="#fff"/>' +
          '<g class="m-pupils"><circle cx="90" cy="93" r="8" fill="#1d0b10"/><circle cx="134" cy="93" r="8" fill="#1d0b10"/><circle cx="93" cy="89" r="3" fill="#fff"/><circle cx="137" cy="89" r="3" fill="#fff"/></g>' +
          '<g class="m-lids"><ellipse cx="88" cy="90" rx="15.5" ry="17.5" fill="#e8461f"/><ellipse cx="132" cy="90" rx="15.5" ry="17.5" fill="#e8461f"/></g></g>' +
        '<g class="m-stars">' + star(88, 90, 15) + star(132, 90, 15) + '</g>' +
        '<path class="mouth mouth-smile" d="M94 130Q110 142 126 130" fill="none" stroke="#7a1a08" stroke-width="4" stroke-linecap="round"/>' +
        '<g class="mouth mouth-open"><path d="M92 126Q110 156 128 126Z" fill="#5a0f05"/><path d="M98 127l3 7 3-7ZM116 127l3 7 3-7Z" fill="#fff"/></g>' +
        '<ellipse class="mouth mouth-o" cx="110" cy="134" rx="7" ry="8" fill="#5a0f05"/>' +
        '<path class="mouth mouth-sad" d="M96 138Q110 128 124 138" fill="none" stroke="#7a1a08" stroke-width="4" stroke-linecap="round"/>' +
        '<g class="em-flame"><path d="M110 134C92 152 80 182 92 222C98 204 106 194 110 180C114 194 122 204 128 222C140 182 128 152 110 134Z" fill="url(#emFlame)"/>' +
          '<path d="M110 146C102 160 98 176 104 196C106 186 108 180 110 174C112 180 114 186 116 196C122 176 118 160 110 146Z" fill="#fffbe0" opacity="0.9"/></g>' +
      '</g>' +
    '</g></svg>';

  function zibLights() {
    const xs = [26, 52, 80, 110, 140, 168, 194];
    return xs.map(function (x, i) {
      const y = 148 + Math.pow((x - 110) / 98, 2) * -6 + 4;
      const c = ['#ff3fd5', '#2ee8ff', '#ffd84d'][i % 3];
      return '<circle cx="' + x + '" cy="' + y.toFixed(1) + '" r="5.5" fill="' + c + '" style="--i:' + i + '"/>';
    }).join('');
  }

  const ZIB = '<svg viewBox="0 0 220 240" class="mascot-svg" aria-hidden="true" focusable="false">' +
    '<g class="zb-beam"><path d="M84 164L136 164L190 238L30 238Z" fill="url(#zbBeam)"/></g>' +
    '<ellipse class="m-shadow" cx="110" cy="236" rx="50" ry="6"/>' +
    '<g class="m-all">' +
      '<g class="m-alien">' +
        '<g class="zb-ant"><path d="M98 70C94 56 88 46 80 40" stroke="#4fd65c" stroke-width="4" stroke-linecap="round" fill="none"/><circle cx="79" cy="38" r="6.5" fill="#ffd84d"/>' +
          '<path d="M122 70C126 56 132 46 140 40" stroke="#4fd65c" stroke-width="4" stroke-linecap="round" fill="none"/><circle cx="141" cy="38" r="6.5" fill="#ff3fd5"/></g>' +
        '<path d="M78 134C80 112 140 112 142 134Z" fill="#e6ebff" stroke="#9aa6d6" stroke-width="2"/>' +
        '<g class="m-arm-r"><path d="M136 124C148 116 152 106 150 98" stroke="#5fd25c" stroke-width="7" stroke-linecap="round" fill="none"/><circle cx="150" cy="95" r="6.5" fill="#72e46c"/></g>' +
        '<ellipse cx="110" cy="94" rx="35" ry="31" fill="url(#zbSkin)"/>' +
        '<g class="m-eyes"><ellipse cx="96" cy="94" rx="10" ry="14" fill="#10131f" transform="rotate(-18 96 94)"/><ellipse cx="124" cy="94" rx="10" ry="14" fill="#10131f" transform="rotate(18 124 94)"/>' +
          '<g class="m-pupils"><circle cx="99" cy="88" r="3.6" fill="#fff"/><circle cx="127" cy="88" r="3.6" fill="#fff"/></g>' +
          '<g class="m-lids"><g transform="rotate(-18 96 94)"><ellipse cx="96" cy="94" rx="11.5" ry="15.5" fill="#62d860"/></g><g transform="rotate(18 124 94)"><ellipse cx="124" cy="94" rx="11.5" ry="15.5" fill="#62d860"/></g></g></g>' +
        '<g class="m-stars">' + star(96, 94, 12) + star(124, 94, 12) + '</g>' +
        '<path class="mouth mouth-smile" d="M102 110Q110 117 118 110" fill="none" stroke="#1d4a1a" stroke-width="3" stroke-linecap="round"/>' +
        '<path class="mouth mouth-open" d="M100 108Q110 124 120 108Z" fill="#1d3a1a"/>' +
        '<ellipse class="mouth mouth-o" cx="110" cy="113" rx="4.5" ry="5.5" fill="#1d3a1a"/>' +
        '<path class="mouth mouth-sad" d="M103 115Q110 109 117 115" fill="none" stroke="#1d4a1a" stroke-width="3" stroke-linecap="round"/>' +
      '</g>' +
      '<path d="M54 134C54 64 166 64 166 134Z" fill="url(#zbDome)" stroke="rgba(255,255,255,0.75)" stroke-width="3"/>' +
      '<path d="M70 110C74 88 90 76 108 74" stroke="rgba(255,255,255,0.8)" stroke-width="5" stroke-linecap="round" fill="none"/>' +
      '<ellipse cx="110" cy="150" rx="100" ry="26" fill="url(#zbHull)" stroke="#3c4470" stroke-width="3"/>' +
      '<ellipse cx="110" cy="137" rx="76" ry="12" fill="url(#zbRim)"/>' +
      '<g class="zb-lights">' + zibLights() + '</g>' +
      '<ellipse cx="110" cy="171" rx="34" ry="8" fill="#555f96" stroke="#3c4470" stroke-width="2"/>' +
    '</g></svg>';

  const ART = { neon: PINA, dragon: EMBER, cosmic: ZIB };
  const NAMES = { neon: 'DJ Pina', dragon: 'Ember', cosmic: 'Zib' };

  let defsAdded = false;
  function ensureDefs() {
    if (defsAdded || typeof document === 'undefined') return;
    defsAdded = true;
    const holder = document.createElement('div');
    holder.innerHTML = DEFS;
    document.body.appendChild(holder.firstChild);
  }

  function html(id, extraClass) {
    ensureDefs();
    return '<span class="mascot mascot-' + id + (extraClass ? ' ' + extraClass : '') + '" data-mood="idle" role="img" aria-label="' + NAMES[id] + '">' +
      ART[id] + '<span class="mascot-bubble" hidden></span></span>';
  }

  // Sets a mood. With `ms`, falls back to the element's base mood afterwards.
  function mood(el, m, ms) {
    if (!el) return;
    clearTimeout(el._moodTimer);
    el.dataset.mood = m;
    if (ms) el._moodTimer = setTimeout(function () { el.dataset.mood = el.dataset.base || 'idle'; }, ms);
  }

  function base(el, m) {
    if (!el) return;
    el.dataset.base = m;
    clearTimeout(el._moodTimer);
    el.dataset.mood = m;
  }

  function say(el, text, ms) {
    if (!el) return;
    const b = el.querySelector('.mascot-bubble');
    if (!b) return;
    clearTimeout(b._t);
    b.textContent = text;
    b.hidden = false;
    b.classList.remove('pop');
    void b.offsetWidth;
    b.classList.add('pop');
    b._t = setTimeout(function () { b.hidden = true; }, ms || 1800);
  }

  root.SlotMascots = { html: html, mood: mood, base: base, say: say, names: NAMES, ensureDefs: ensureDefs };
}(typeof self !== 'undefined' ? self : this));
