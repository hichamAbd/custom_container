#!/usr/bin/env python3
"""
Serve Reel Riot on your local network so you can play it on your phone.

    python3 serve.py            # port 8080
    python3 serve.py 9000       # another port
    python3 serve.py --no-qr    # skip the QR code

Then open the printed http://192.168.x.x:8080 address on your phone (it must
be on the same Wi-Fi as this computer), or scan the QR code with the camera.
Only the Python standard library is used.
"""
import functools
import http.server
import os
import socket
import socketserver
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


# ----------------------------------------------------------------- QR code
# A compact QR encoder (byte mode, error correction level M), following the
# QR Code specification (ISO/IEC 18004). Enough for any local URL.

_ECC_PER_BLOCK_M = [-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26]
_BLOCKS_M = [-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16]
_FORMAT_M = 0  # format bits for error correction level M


def _raw_modules(ver):
    n = (16 * ver + 128) * ver + 64
    if ver >= 2:
        k = ver // 7 + 2
        n -= (25 * k - 10) * k - 55
        if ver >= 7:
            n -= 36
    return n


def _data_codewords(ver):
    return _raw_modules(ver) // 8 - _ECC_PER_BLOCK_M[ver] * _BLOCKS_M[ver]


def _gf_mul(x, y):
    z = 0
    for i in reversed(range(8)):
        z = (z << 1) ^ ((z >> 7) * 0x11D)
        z ^= ((y >> i) & 1) * x
    return z


def _rs_divisor(degree):
    result = [0] * (degree - 1) + [1]
    root = 1
    for _ in range(degree):
        for j in range(degree):
            result[j] = _gf_mul(result[j], root)
            if j + 1 < degree:
                result[j] ^= result[j + 1]
        root = _gf_mul(root, 0x02)
    return result


def _rs_remainder(data, divisor):
    result = [0] * len(divisor)
    for b in data:
        factor = b ^ result.pop(0)
        result.append(0)
        for i, coef in enumerate(divisor):
            result[i] ^= _gf_mul(coef, factor)
    return result


_MASKS = (
    lambda x, y: (x + y) % 2,
    lambda x, y: y % 2,
    lambda x, y: x % 3,
    lambda x, y: (x + y) % 3,
    lambda x, y: (x // 3 + y // 2) % 2,
    lambda x, y: x * y % 2 + x * y % 3,
    lambda x, y: (x * y % 2 + x * y % 3) % 2,
    lambda x, y: ((x + y) % 2 + x * y % 3) % 2,
)


class QR:
    def __init__(self, text):
        data = text.encode("utf-8")
        for ver in range(1, len(_BLOCKS_M)):
            count_bits = 8 if ver <= 9 else 16
            if 4 + count_bits + 8 * len(data) <= _data_codewords(ver) * 8:
                break
        else:
            raise ValueError("text too long for this QR encoder")
        self.ver = ver
        self.size = ver * 4 + 17
        n = self.size
        self.mod = [[False] * n for _ in range(n)]
        self.fn = [[False] * n for _ in range(n)]

        bits = []

        def put(val, length):
            for i in reversed(range(length)):
                bits.append((val >> i) & 1)

        put(0x4, 4)
        put(len(data), count_bits)
        for b in data:
            put(b, 8)
        cap = _data_codewords(ver) * 8
        put(0, min(4, cap - len(bits)))
        put(0, -len(bits) % 8)
        pad = 0xEC
        while len(bits) < cap:
            put(pad, 8)
            pad ^= 0xEC ^ 0x11
        codewords = [int("".join(map(str, bits[i:i + 8])), 2) for i in range(0, len(bits), 8)]

        self._function_patterns()
        self._place(self._interleave(codewords))
        best = None
        for mask in range(8):
            self._apply_mask(mask)
            self._format_bits(mask)
            score = self._penalty()
            if best is None or score < best[0]:
                best = (score, mask)
            self._apply_mask(mask)  # undo (XOR)
        self._apply_mask(best[1])
        self._format_bits(best[1])

    def _set_fn(self, x, y, dark):
        self.mod[y][x] = dark
        self.fn[y][x] = True

    def _function_patterns(self):
        n = self.size
        for i in range(n):
            self._set_fn(6, i, i % 2 == 0)
            self._set_fn(i, 6, i % 2 == 0)
        for cx, cy in ((3, 3), (n - 4, 3), (3, n - 4)):
            for dy in range(-4, 5):
                for dx in range(-4, 5):
                    x, y = cx + dx, cy + dy
                    if 0 <= x < n and 0 <= y < n:
                        self._set_fn(x, y, max(abs(dx), abs(dy)) not in (2, 4))
        pos = self._alignment_positions()
        last = len(pos) - 1
        for i, ax in enumerate(pos):
            for j, ay in enumerate(pos):
                if (i, j) in ((0, 0), (0, last), (last, 0)):
                    continue
                for dy in range(-2, 3):
                    for dx in range(-2, 3):
                        self._set_fn(ax + dx, ay + dy, max(abs(dx), abs(dy)) != 1)
        self._format_bits(0)
        if self.ver >= 7:
            rem = self.ver
            for _ in range(12):
                rem = (rem << 1) ^ ((rem >> 11) * 0x1F25)
            v = self.ver << 12 | rem
            for i in range(18):
                bit = (v >> i) & 1 == 1
                a, b = n - 11 + i % 3, i // 3
                self._set_fn(a, b, bit)
                self._set_fn(b, a, bit)

    def _alignment_positions(self):
        if self.ver == 1:
            return []
        k = self.ver // 7 + 2
        step = (self.ver * 8 + k * 3 + 5) // (k * 4 - 4) * 2
        return list(reversed([self.size - 7 - i * step for i in range(k - 1)] + [6]))

    def _format_bits(self, mask):
        data = _FORMAT_M << 3 | mask
        rem = data
        for _ in range(10):
            rem = (rem << 1) ^ ((rem >> 9) * 0x537)
        bits = (data << 10 | rem) ^ 0x5412
        bit = lambda i: (bits >> i) & 1 == 1
        n = self.size
        for i in range(6):
            self._set_fn(8, i, bit(i))
        self._set_fn(8, 7, bit(6))
        self._set_fn(8, 8, bit(7))
        self._set_fn(7, 8, bit(8))
        for i in range(9, 15):
            self._set_fn(14 - i, 8, bit(i))
        for i in range(8):
            self._set_fn(n - 1 - i, 8, bit(i))
        for i in range(8, 15):
            self._set_fn(8, n - 15 + i, bit(i))
        self._set_fn(8, n - 8, True)

    def _interleave(self, data):
        ver = self.ver
        nblocks = _BLOCKS_M[ver]
        ecclen = _ECC_PER_BLOCK_M[ver]
        raw = _raw_modules(ver) // 8
        nshort = nblocks - raw % nblocks
        shortlen = raw // nblocks
        div = _rs_divisor(ecclen)
        blocks = []
        k = 0
        for i in range(nblocks):
            dat = data[k:k + shortlen - ecclen + (0 if i < nshort else 1)]
            k += len(dat)
            ecc = _rs_remainder(dat, div)
            if i < nshort:
                dat = dat + [0]
            blocks.append(dat + ecc)
        out = []
        for i in range(len(blocks[0])):
            for j, blk in enumerate(blocks):
                if i != shortlen - ecclen or j >= nshort:
                    out.append(blk[i])
        return out

    def _place(self, data):
        n = self.size
        i = 0
        right = n - 1
        while right >= 1:
            if right == 6:
                right = 5
            for vert in range(n):
                for j in range(2):
                    x = right - j
                    upward = ((right + 1) & 2) == 0
                    y = (n - 1 - vert) if upward else vert
                    if not self.fn[y][x] and i < len(data) * 8:
                        self.mod[y][x] = (data[i >> 3] >> (7 - (i & 7))) & 1 == 1
                        i += 1
            right -= 2

    def _apply_mask(self, mask):
        f = _MASKS[mask]
        for y in range(self.size):
            for x in range(self.size):
                if not self.fn[y][x] and f(x, y) == 0:
                    self.mod[y][x] = not self.mod[y][x]

    def _penalty(self):
        n = self.size
        m = self.mod
        score = 0
        lines = [m[y] for y in range(n)] + [[m[y][x] for y in range(n)] for x in range(n)]
        finder_a = [True, False, True, True, True, False, True, False, False, False, False]
        finder_b = finder_a[::-1]
        for line in lines:
            run = 1
            for i in range(1, n + 1):
                if i < n and line[i] == line[i - 1]:
                    run += 1
                else:
                    if run >= 5:
                        score += 3 + run - 5
                    run = 1
            padded = [False] * 4 + list(line) + [False] * 4
            for i in range(len(padded) - 10):
                window = padded[i:i + 11]
                if window == finder_a or window == finder_b:
                    score += 40
        for y in range(n - 1):
            for x in range(n - 1):
                c = m[y][x]
                if c == m[y][x + 1] == m[y + 1][x] == m[y + 1][x + 1]:
                    score += 3
        dark = sum(sum(1 for c in row if c) for row in m)
        total = n * n
        score += 10 * (abs(dark * 20 - total * 10) // total)
        return score

    def terminal(self, quiet=2):
        """Black-on-white half-block rendering, two module rows per line."""
        n = self.size
        dark = lambda x, y: 0 <= x < n and 0 <= y < n and self.mod[y][x]
        out = []
        for y in range(-quiet, n + quiet, 2):
            row = []
            for x in range(-quiet, n + quiet):
                top, bottom = dark(x, y), dark(x, y + 1)
                row.append("█" if top and bottom else "▀" if top else "▄" if bottom else " ")
            out.append("\033[38;5;16;48;5;231m" + "".join(row) + "\033[0m")
        return "\n".join(out)


# ------------------------------------------------------------------ server

def lan_addresses():
    found = []
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("10.255.255.255", 1))  # no packet is sent
        found.append(s.getsockname()[0])
        s.close()
    except OSError:
        pass
    try:
        for info in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET):
            found.append(info[4][0])
    except OSError:
        pass
    seen = []
    for ip in found:
        if ip not in seen and not ip.startswith("127."):
            seen.append(ip)
    return seen


class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = dict(http.server.SimpleHTTPRequestHandler.extensions_map)
    extensions_map.update({
        ".js": "text/javascript",
        ".css": "text/css",
        ".html": "text/html",
        ".svg": "image/svg+xml",
        ".png": "image/png",
        ".webmanifest": "application/manifest+json",
    })

    def end_headers(self):
        self.send_header("Cache-Control", "no-cache")
        super().end_headers()

    def log_message(self, fmt, *args):
        pass  # keep the terminal clean


class Server(socketserver.ThreadingMixIn, http.server.HTTPServer):
    daemon_threads = True
    allow_reuse_address = True


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    show_qr = "--no-qr" not in sys.argv
    port = int(args[0]) if args else 8080
    if os.name == "nt":
        os.system("")  # turn on colour escape codes in the Windows console

    handler = functools.partial(Handler, directory=HERE)
    httpd = None
    for p in range(port, port + 20):
        try:
            httpd = Server(("0.0.0.0", p), handler)
            port = p
            break
        except OSError:
            continue
    if httpd is None:
        sys.exit("Could not find a free port near %d." % port)

    ips = lan_addresses()
    print()
    print("  REEL RIOT is running!")
    print()
    print("  On this computer:  http://localhost:%d" % port)
    if ips:
        for ip in ips:
            print("  On your phone:     http://%s:%d" % (ip, port))
        print()
        print("  Your phone must be on the same Wi-Fi network as this computer.")
        if show_qr:
            print("  Scan this with your phone camera:")
            print()
            print(QR("http://%s:%d/" % (ips[0], port)).terminal())
    else:
        print("  No network connection found, so only this computer can play.")
    print()
    print("  If the phone can't connect, allow Python through the firewall for")
    print("  private networks. Press Ctrl+C to stop.")
    print()
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n  Stopped. Thanks for playing!")
    finally:
        httpd.server_close()


if __name__ == "__main__":
    main()
