# Pocket Scanner

A phone-friendly web app that turns a photo of a document into a flat, straight scan.

1. **Take a photo** (opens the camera) or **choose one from your gallery**.
2. **Drag the four corners** onto the corners of the page. A magnifier shows exactly where each corner lands.
   Optionally pick a page shape (A4, US Letter, ID card, square) to lock the proportions.
3. **Straighten**: the page is perspective-corrected into a rectangle.
4. Pick a look: *Original*, *Magic color* (removes shadows, whitens paper), *Grayscale* or *Black & white*. Rotate if needed.
5. **Save to gallery**:
   - iPhone/iPad: the share sheet opens, tap **Save Image** to put it in Photos.
   - Android: the share sheet opens (pick your gallery/Photos app), or it downloads to *Downloads*, which shows up in the gallery.
   - Desktop: the JPEG is downloaded.

Everything runs in the browser. The photo is never uploaded anywhere.

## Run it

It's a static site with no build step and no dependencies: `index.html`, `manifest.webmanifest`, `icon.svg`.

- Locally: `python3 -m http.server` in this folder, then open `http://localhost:8000`.
- On GitHub Pages: with Pages serving `/docs` from the default branch, it's at
  `https://hichamabd.github.io/custom_container/scanner/`.
- To give it its own repo, copy these three files to the root of a new repo and enable Pages for it.

Sharing to the photo gallery needs HTTPS (GitHub Pages provides it). On a phone you can use
"Add to Home Screen" to launch it like an app.
