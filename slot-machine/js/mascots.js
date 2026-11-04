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
    // Ember (realistic)
    '<radialGradient id="dgBody" cx="0.38" cy="0.28" r="0.85"><stop offset="0" stop-color="#ff7448"/><stop offset="0.3" stop-color="#d22a1c"/><stop offset="0.7" stop-color="#7a0a12"/><stop offset="1" stop-color="#360208"/></radialGradient>' +
    '<linearGradient id="dgBelly" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ffe7a8"/><stop offset="0.5" stop-color="#e2a24a"/><stop offset="1" stop-color="#8a4c12"/></linearGradient>' +
    '<linearGradient id="dgWing" x1="0.1" y1="0" x2="0.7" y2="1"><stop offset="0" stop-color="#d0432c"/><stop offset="0.55" stop-color="#7c0c1a"/><stop offset="1" stop-color="#36040e"/></linearGradient>' +
    '<linearGradient id="dgWingFar" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#8a1a1e"/><stop offset="1" stop-color="#26020a"/></linearGradient>' +
    '<linearGradient id="dgHorn" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff6dc"/><stop offset="0.5" stop-color="#e3c690"/><stop offset="1" stop-color="#7a5a32"/></linearGradient>' +
    '<radialGradient id="dgEye" cx="0.5" cy="0.5" r="0.6"><stop offset="0" stop-color="#fff8c0"/><stop offset="0.35" stop-color="#ffcc30"/><stop offset="0.75" stop-color="#ff6a00"/><stop offset="1" stop-color="#8a1a00"/></radialGradient>' +
    '<radialGradient id="dgThroat" cx="0.35" cy="0.4" r="0.8"><stop offset="0" stop-color="#fff0a0"/><stop offset="0.35" stop-color="#ff9a2a"/><stop offset="0.75" stop-color="#b02008"/><stop offset="1" stop-color="#3a0004"/></radialGradient>' +
    '<pattern id="dgScales" width="8" height="6" patternUnits="userSpaceOnUse"><path d="M0 6Q4 0 8 6M-4 3Q0 -3 4 3M4 3Q8 -3 12 3" fill="none" stroke="#2a0206" stroke-width="0.8"/><path d="M1.5 5Q4 1.5 6.5 5" fill="none" stroke="#ff9a6a" stroke-width="0.6" opacity="0.5"/></pattern>' +
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

  // Ember: a crimson dragon drawn with lit, scaled surfaces (Dragon's Hoard).
  const EMBER = '<svg viewBox="0 0 220 250" class="mascot-svg" aria-hidden="true" focusable="false">' +
    '<ellipse class="m-shadow" cx="112" cy="243" rx="78" ry="7"/>' +
    '<g class="m-all">' +
      // far wing, behind the neck
      '<g class="em-wing-r"><path d="M114 116C106 92 100 72 96 56C92 44 90 34 88 24C72 16 50 10 30 8C38 20 42 32 40 46C52 46 62 54 66 66C76 64 86 72 92 84C102 84 110 96 114 116Z" fill="url(#dgWingFar)" stroke="#1e0206" stroke-width="2" stroke-linejoin="round"/>' +
        '<path d="M88 24L32 9M88 24L41 45M90 30L67 65M96 58L92 83" stroke="#3a0408" stroke-width="2.2" stroke-linecap="round" fill="none"/></g>' +
      // near wing
      '<g class="em-wing-l">' +
        '<path d="M102 126C92 104 84 84 78 70C72 58 68 50 64 42C48 36 26 32 6 34C16 44 20 56 18 70C30 70 40 78 44 90C54 88 64 94 70 106C80 104 92 110 102 126Z" fill="url(#dgWing)" stroke="#2a0408" stroke-width="2" stroke-linejoin="round"/>' +
        '<path d="M64 42L8 35M64 42L19 69M66 46L45 89M80 76L70 105" stroke="#4a060c" stroke-width="2.6" stroke-linecap="round" fill="none"/>' +
        '<path d="M60 44C46 46 34 52 26 62M58 58C52 66 50 76 50 86" stroke="#ff8a5a" stroke-width="1" opacity="0.35" fill="none"/>' +
        '<path d="M102 126C92 104 84 84 78 70C72 58 68 50 64 42" stroke="url(#dgBody)" stroke-width="7" stroke-linecap="round" fill="none"/>' +
        '<path d="M100 120C92 102 85 86 79 72" stroke="#ff9a6a" stroke-width="1.6" stroke-linecap="round" opacity="0.6" fill="none"/>' +
        '<path d="M64 42L58 32L68 38Z" fill="url(#dgHorn)"/></g>' +
      // hind leg
      '<g class="m-legs"><ellipse cx="76" cy="196" rx="31" ry="27" fill="url(#dgBody)" stroke="#2a0206" stroke-width="2"/>' +
        '<ellipse cx="76" cy="196" rx="31" ry="27" fill="url(#dgScales)" opacity="0.55"/>' +
        '<path d="M56 182C64 172 80 170 92 176" stroke="#ff8a5a" stroke-width="2" opacity="0.45" fill="none" stroke-linecap="round"/>' +
        '<path d="M50 228C56 218 82 216 96 222L106 226L98 229L103 234L90 233C78 237 58 237 50 231Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="2" stroke-linejoin="round"/>' +
        '<path d="M106 226L113 228L105 231ZM103 234L110 237L101 237Z" fill="url(#dgHorn)"/></g>' +
      // torso with belly plates
      '<g class="m-torso"><path d="M48 196C42 162 64 130 100 124C122 120 136 136 134 162C132 188 118 214 92 220C70 224 54 214 48 196Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="2"/>' +
        '<path d="M48 196C42 162 64 130 100 124C122 120 136 136 134 162C132 188 118 214 92 220C70 224 54 214 48 196Z" fill="url(#dgScales)" opacity="0.5"/>' +
        '<path d="M118 132C134 144 132 178 122 198C116 210 106 216 96 218C106 202 112 182 112 160C112 148 113 138 118 132Z" fill="url(#dgBelly)" stroke="#6a3408" stroke-width="1.4"/>' +
        '<path d="M116 146L130 148M115 160L132 162M114 174L130 178M110 188L126 192M104 202L118 206" stroke="#7a4210" stroke-width="1.3" opacity="0.75"/>' +
        '<path d="M60 150C70 136 84 128 100 126" stroke="#ffb08a" stroke-width="2.4" opacity="0.5" fill="none" stroke-linecap="round"/></g>' +
      // front leg
      '<path d="M106 158C126 164 135 190 132 214L144 222L138 227L144 232L119 232C110 230 108 222 110 214C112 200 104 184 98 172Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="2" stroke-linejoin="round"/>' +
      '<path d="M108 162C124 168 131 188 130 206" stroke="#ff8a5a" stroke-width="2" opacity="0.5" fill="none" stroke-linecap="round"/>' +
      '<path d="M144 222L151 224L143 227ZM144 232L151 234L142 234Z" fill="url(#dgHorn)"/>' +
      // tail wrapped around the feet
      '<g class="em-tail"><path d="M54 200C58 226 94 236 128 232C148 230 166 226 180 216L194 207L189 223L199 231L182 228C168 238 146 242 128 242C92 244 52 232 46 206Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="2" stroke-linejoin="round"/>' +
        '<path d="M60 220C80 232 110 236 140 232" stroke="#ff8a5a" stroke-width="2" opacity="0.45" fill="none" stroke-linecap="round"/>' +
        '<path d="M194 207L189 223L199 231L184 226Z" fill="url(#dgHorn)" opacity="0.85"/></g>' +
      // neck, spines and throat plates
      '<path d="M98 132C102 106 118 84 142 72L162 84C148 92 136 106 130 124C128 138 122 150 112 156Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="2"/>' +
      '<path d="M98 132C102 106 118 84 142 72L162 84C148 92 136 106 130 124C128 138 122 150 112 156Z" fill="url(#dgScales)" opacity="0.5"/>' +
      '<path d="M142 75C150 79 156 83 161 86C147 96 137 110 131 128C129 140 125 148 120 152C124 136 126 118 134 100C138 90 141 82 142 75Z" fill="url(#dgBelly)" stroke="#6a3408" stroke-width="1.2"/>' +
      '<path d="M100 120L92 112L104 112ZM106 104L98 94L110 97ZM115 91L110 80L121 85ZM126 81L124 70L133 77ZM90 138L80 134L92 130Z" fill="url(#dgHorn)" stroke="#5a3a18" stroke-width="0.8" stroke-linejoin="round"/>' +
      '<g class="m-head"><g transform="translate(140 80) scale(1.28) translate(-140 -80)">' +
        // horns and frills behind the skull
        '<path d="M142 56C130 46 116 42 100 44C114 48 124 54 132 64Z" fill="url(#dgHorn)" stroke="#5a3a18" stroke-width="1.2" stroke-linejoin="round"/>' +
        '<path d="M134 80L114 84L130 90ZM138 88L120 100L140 96Z" fill="#7a0a12" stroke="#2a0206" stroke-width="1.4" stroke-linejoin="round"/>' +
        // lower jaw: closed, slightly open, wide open
        '<g class="mouth mouth-smile"><path d="M138 84C154 87 176 85 199 80C197 87 187 91 173 93C159 95 146 93 138 88Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="1.8"/><path d="M174 84L177 90L180 84Z" fill="#fff6dc"/></g>' +
        '<g class="mouth mouth-sad"><path d="M138 84C154 89 176 89 197 84C195 90 186 94 173 95C159 96 146 94 138 88Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="1.8"/></g>' +
        '<g class="mouth mouth-o"><path d="M146 82C162 84 182 82 198 79C195 88 186 95 172 96C160 96 150 90 146 82Z" fill="url(#dgThroat)"/><path d="M140 86C154 92 172 98 192 98C189 104 178 106 164 103C152 100 143 94 140 88Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="1.8"/></g>' +
        '<g class="mouth mouth-open"><path d="M144 82C160 84 180 82 199 79C196 94 188 104 176 107C160 105 148 96 144 82Z" fill="url(#dgThroat)"/>' +
          '<path d="M140 85C150 95 170 106 193 109C189 115 176 117 162 113C150 109 141 99 140 88Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="1.8"/>' +
          '<path d="M168 106L171 100L174 107ZM178 108L181 102L184 108ZM158 102L161 97L164 104Z" fill="#fff6dc"/></g>' +
        // skull and upper jaw
        '<path d="M126 70C130 52 148 44 166 46C180 48 192 56 202 64C209 69 211 75 206 79C196 81 184 81 172 83C160 85 146 87 136 85C129 81 125 77 126 70Z" fill="url(#dgBody)" stroke="#2a0206" stroke-width="2"/>' +
        '<path d="M126 70C130 52 148 44 166 46C180 48 192 56 202 64C209 69 211 75 206 79C196 81 184 81 172 83C160 85 146 87 136 85C129 81 125 77 126 70Z" fill="url(#dgScales)" opacity="0.45"/>' +
        '<path d="M140 54C150 47 166 45 180 50C190 54 198 60 204 66" stroke="#ffa070" stroke-width="2.2" opacity="0.55" fill="none" stroke-linecap="round"/>' +
        '<path d="M164 81L167 87L170 81ZM184 79L187 85L190 79ZM196 78L198 83L201 77Z" fill="#fff6dc"/>' +
        '<ellipse cx="200" cy="66" rx="3.2" ry="2" fill="#2a0204" transform="rotate(-20 200 66)"/>' +
        // brow ridge, eye, lid
        '<path d="M150 56C158 49 172 49 181 56C173 55 165 57 159 61Z" fill="#5a060c"/>' +
        '<ellipse cx="165" cy="60" rx="12" ry="8" fill="#ff8a1a" opacity="0.35"/>' +
        '<g class="m-eyes"><path d="M154 61C158 54 170 54 176 60C170 66 159 67 154 61Z" fill="url(#dgEye)" stroke="#2a0204" stroke-width="0.8"/>' +
          '<g class="m-pupils"><path d="M164.5 56.5C166 59 166 62 164.5 64.5C163 62 163 59 164.5 56.5Z" fill="#140000"/><circle cx="161" cy="59" r="1.3" fill="#fff"/></g>' +
          '<g class="m-lids"><ellipse cx="164.5" cy="60" rx="10" ry="6" fill="#8a1012"/></g></g>' +
        '<g class="m-stars">' + star(164, 60, 11) + '</g>' +
        // main horn, over the skull
        '<path d="M150 50C140 34 124 24 104 18C120 30 132 44 140 58Z" fill="url(#dgHorn)" stroke="#5a3a18" stroke-width="1.2" stroke-linejoin="round"/>' +
        '<path d="M140 44L134 48M132 36L127 41M122 29L118 33" stroke="#8a6a40" stroke-width="1.2" stroke-linecap="round"/>' +
        '<path d="M182 54L188 46L190 56Z" fill="url(#dgHorn)" stroke="#5a3a18" stroke-width="0.8"/>' +
        '<g class="em-flame"><ellipse class="em-mouth" cx="203" cy="86" rx="13" ry="9" fill="url(#emFlame)"/></g>' +
      '</g></g>' +
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
