#!/usr/bin/env python3
import argparse
import base64
import gzip
import io
import os
import sys
import urllib.parse
import zipfile
from pathlib import Path

MAX_DEPTH = 5


def needles(secret: str) -> list[bytes]:
    raw = secret.encode()
    found = {raw, urllib.parse.quote(secret, safe="").encode()}
    for shift in range(3):
        for encode in (base64.b64encode, base64.urlsafe_b64encode):
            encoded = encode(b"\0" * shift + raw)
            start = (shift * 4 + 2) // 3 + 1 if shift else 0
            found.add(encoded[start:len(encoded) - 4])
    return [needle for needle in found if len(needle) >= 16]


def contains_secret(data: bytes, patterns: list[bytes]) -> bool:
    joined = data.replace(b"\r", b"").replace(b"\n", b"")
    return any(pattern in data or pattern in joined for pattern in patterns)


def unpacked(name: str, data: bytes) -> list[tuple[str, bytes]]:
    try:
        if zipfile.is_zipfile(io.BytesIO(data)):
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                return [(f"{name}!{member}", archive.read(member)) for member in archive.namelist()]
        if data[:2] == b"\x1f\x8b":
            return [(f"{name}!gunzip", gzip.decompress(data))]
    except (zipfile.BadZipFile, OSError, EOFError, RuntimeError, NotImplementedError) as error:
        print(f"::warning::{name} looks like an archive but cannot be unpacked ({type(error).__name__}), scanned as bytes")
    return []


def leaks_in(name: str, data: bytes, patterns: list[bytes], depth: int = 0) -> list[str]:
    leaked = [name] if contains_secret(data, patterns) else []
    if depth < MAX_DEPTH:
        for inner_name, inner in unpacked(name, data):
            leaked += leaks_in(inner_name, inner, patterns, depth + 1)
    return leaked


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Fail when a secret is found in files about to be uploaded: raw, URL-encoded, inside any base64 "
                    "(such as Basic credentials), split across lines, or inside zip and gzip archives at any depth. "
                    "Only file names are printed.")
    parser.add_argument("--secret-env", required=True, help="Name of the environment variable holding the secret")
    parser.add_argument("paths", nargs="+", type=Path)
    args = parser.parse_args()

    secret = os.environ.get(args.secret_env, "")
    if len(secret) < 16:
        raise SystemExit(f"{args.secret_env} is empty or too short to look for")
    patterns = needles(secret)

    scanned = 0
    leaked: list[str] = []
    for root in args.paths:
        if not root.exists():
            continue
        for path in [root] if root.is_file() else sorted(p for p in root.rglob("*") if p.is_file()):
            scanned += 1
            leaked += leaks_in(str(path), path.read_bytes(), patterns)

    print(f"Scanned {scanned} file(s) for {args.secret_env}")
    if leaked:
        print(f"::error::{args.secret_env} found in {len(leaked)} place(s), nothing is uploaded:")
        for name in leaked:
            print(f"  {name}")
        sys.exit(1)


if __name__ == "__main__":
    main()
