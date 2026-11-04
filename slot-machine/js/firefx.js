/*
 * Reel Riot - real-time fire. A WebGL fragment shader draws a turbulent
 * flame jet (layered value noise) from a point toward a target, swept
 * across an angle over time. The canvas only exists and animates while a
 * jet is burning. Without WebGL, breathe() resolves and nothing is drawn.
 */
(function (root) {
  'use strict';

  const SCALE = 0.5; // fire is soft, so half resolution looks the same and costs a quarter
  let canvas = null, gl = null, prog = null, U = {}, raf = 0, jet = null;

  const VS = 'attribute vec2 p;void main(){gl_Position=vec4(p,0.,1.);}';
  const FS = [
    'precision mediump float;',
    'uniform vec2 R;uniform float T;uniform vec2 O;uniform float A;uniform vec2 L;',
    'float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}',
    'float n(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(h(i),h(i+vec2(1.,0.)),f.x),mix(h(i+vec2(0.,1.)),h(i+vec2(1.,1.)),f.x),f.y);}',
    'float fbm(vec2 p){float v=0.,a=.5;for(int i=0;i<5;i++){v+=a*n(p);p=p*2.02+vec2(1.7,9.2);a*=.5;}return v;}',
    'void main(){',
    '  vec2 d=gl_FragCoord.xy-O;',
    '  float c=cos(A),s=sin(A);',
    '  vec2 q=vec2(d.x*c+d.y*s,-d.x*s+d.y*c);',
    '  float reach=L.x,alpha=L.y,k=R.y/700.;',
    '  float f=0.;',
    '  if(q.x>-6.*k&&q.x<reach*1.12){',
    '    float nn=fbm(vec2(q.x*.010/k-T*6.,q.y*.020/k+T*.7));',
    '    float n2=fbm(vec2(q.x*.022/k-T*9.,q.y*.034/k));',
    '    float w=(5.*k+max(q.x,0.)*.16)*(.8+n2*.5);',
    '    float y=q.y+(nn-.5)*w*1.3+sin(q.x*.013/k-T*7.)*w*.15;',
    '    f=smoothstep(w,w*.12,abs(y));',
    '    f*=smoothstep(reach*1.12,reach*.78,q.x);',
    '    f*=smoothstep(-6.*k,8.*k,q.x);',
    '    f*=.5+nn;',
    '    f=clamp(f*alpha*1.05,0.,1.);',
    '  }',
    '  float glow=exp(-dot(d,d)/(2600.*k*k))*.5*alpha;',
    '  vec3 col=vec3(1.4*f,1.05*pow(f,2.),.5*pow(f,5.))+vec3(1.,.5,.15)*glow;',
    '  gl_FragColor=vec4(col,1.);',
    '}'
  ].join('\n');

  function init() {
    if (canvas) return !!prog;
    canvas = document.createElement('canvas');
    canvas.className = 'fire-fx';
    canvas.setAttribute('aria-hidden', 'true');
    canvas.hidden = true;
    document.body.appendChild(canvas);
    try { gl = canvas.getContext('webgl', { antialias: false, alpha: false, premultipliedAlpha: false }); } catch (e) { gl = null; }
    if (!gl) return false;
    const sh = (type, src) => { const x = gl.createShader(type); gl.shaderSource(x, src); gl.compileShader(x); return x; };
    prog = gl.createProgram();
    gl.attachShader(prog, sh(gl.VERTEX_SHADER, VS));
    gl.attachShader(prog, sh(gl.FRAGMENT_SHADER, FS));
    gl.linkProgram(prog);
    if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) { prog = null; return false; }
    gl.useProgram(prog);
    gl.bindBuffer(gl.ARRAY_BUFFER, gl.createBuffer());
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
    const loc = gl.getAttribLocation(prog, 'p');
    gl.enableVertexAttribArray(loc);
    gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
    ['R', 'T', 'O', 'A', 'L'].forEach(k => { U[k] = gl.getUniformLocation(prog, k); });
    return true;
  }

  function size() {
    const w = Math.max(1, Math.round(innerWidth * SCALE)), h = Math.max(1, Math.round(innerHeight * SCALE));
    if (canvas.width !== w || canvas.height !== h) { canvas.width = w; canvas.height = h; }
  }

  function frame(now) {
    if (!jet) return;
    const t = (now - jet.start) / 1000;
    if (t > jet.dur) {
      canvas.hidden = true;
      const done = jet.done;
      jet = null;
      done();
      return;
    }
    size();
    const k = Math.min(1, t / jet.dur);
    const ease = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
    const angle = jet.a0 + (jet.a1 - jet.a0) * ease;
    const reach = Math.min(1, t / 0.35) * jet.len;
    const alpha = t < jet.dur - 0.45 ? Math.min(1, t / 0.12) : Math.max(0, (jet.dur - t) / 0.45);
    gl.viewport(0, 0, canvas.width, canvas.height);
    gl.uniform2f(U.R, canvas.width, canvas.height);
    gl.uniform1f(U.T, now / 1000);
    // WebGL's y axis points up, the page's points down.
    gl.uniform2f(U.O, jet.x * SCALE, (innerHeight - jet.y) * SCALE);
    gl.uniform1f(U.A, -angle);
    gl.uniform2f(U.L, reach * SCALE, alpha);
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
    raf = requestAnimationFrame(frame);
  }

  /*
   * Breathe a jet from page point `from` toward the `to` points, sweeping
   * from the first target to the last. Resolves when the flame dies out.
   */
  function breathe(from, to, dur) {
    return new Promise(resolve => {
      if (!init() || !to.length) { resolve(false); return; }
      const ang = p => Math.atan2(p.y - from.y, p.x - from.x);
      const dist = p => Math.hypot(p.x - from.x, p.y - from.y);
      const first = to[0], last = to[to.length - 1];
      let a0 = ang(first), a1 = ang(last);
      if (Math.abs(a1 - a0) > Math.PI) a1 += a1 < a0 ? Math.PI * 2 : -Math.PI * 2;
      if (jet) jet.done(true);
      cancelAnimationFrame(raf);
      jet = {
        x: from.x, y: from.y, a0: a0, a1: a1,
        len: Math.max.apply(null, to.map(dist)) * 1.08,
        dur: dur || 1.6, start: performance.now(), done: resolve
      };
      canvas.hidden = false;
      raf = requestAnimationFrame(frame);
    });
  }

  function supported() { return init(); }

  root.SlotFire = { breathe: breathe, supported: supported };
}(typeof self !== 'undefined' ? self : this));
