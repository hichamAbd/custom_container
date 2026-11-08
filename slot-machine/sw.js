/*
 * Reel Riot - offline support for the installed app. Game files are fetched
 * fresh when online (so updates arrive right away) and served from the
 * cache when offline. Google Fonts are cached the first time they load.
 */
'use strict';

const CACHE = 'reel-riot-v1';
const FILES = [
  './',
  'index.html',
  'manifest.webmanifest',
  'css/style.css',
  'css/mascots.css',
  'css/features.css',
  'js/engine.js',
  'js/machines.js',
  'js/progress.js',
  'js/audio.js',
  'js/fx.js',
  'js/mascots.js',
  'js/bonus.js',
  'js/art.js',
  'js/firefx.js',
  'js/three3d.js',
  'js/app.js',
  'js/vendor/three.min.js',
  'icons/icon.svg',
  'icons/icon-192.png',
  'icons/icon-512.png'
];
const FONT_HOSTS = ['fonts.googleapis.com', 'fonts.gstatic.com'];

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);

  if (FONT_HOSTS.includes(url.hostname)) {
    event.respondWith(caches.open(CACHE).then(c => c.match(req).then(hit => hit || fetch(req).then(res => {
      c.put(req, res.clone());
      return res;
    }))));
    return;
  }
  if (url.origin !== self.location.origin) return;

  // Network first, cache as the offline fallback.
  event.respondWith(
    fetch(req).then(res => {
      if (res.ok) {
        const copy = res.clone();
        caches.open(CACHE).then(c => c.put(req, copy));
      }
      return res;
    }).catch(() => caches.match(req, { ignoreSearch: true }).then(hit => hit || caches.match('index.html')))
  );
});
