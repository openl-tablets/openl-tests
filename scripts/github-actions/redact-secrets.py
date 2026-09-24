#!/usr/bin/env python3
import argparse
import gzip
import io
import os
import re
import sys
import urllib.parse
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from secretforms import load, url_forms  # noqa: E402

MEDIA_SUFFIXES = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".webm", ".mp4", ".ico", ".woff", ".woff2"}


def replacements(token_variables: list[str], url_variables: list[str]) -> list[tuple[re.Pattern, bytes]]:
    load(token_variables, url_variables)
    pairs: list[tuple[bytes, bytes, int]] = []
    for name in token_variables:
        value = os.environ[name].strip()
        placeholder = ("${" + name + "}").encode()
        pairs += [(form, placeholder, 0) for form in {value.encode(), urllib.parse.quote(value, safe="").encode()}]
    for name in url_variables:
        placeholder = ("${" + name + "}").encode()
        pairs += [(form, placeholder, re.IGNORECASE) for form in url_forms(os.environ[name])]
    pairs.sort(key=lambda pair: len(pair[0]), reverse=True)
    return [(re.compile(re.escape(form), flags), placeholder) for form, placeholder, flags in pairs]


def archive_kind(data: bytes) -> str:
    try:
        if zipfile.is_zipfile(io.BytesIO(data)):
            return "zip"
    except (OSError, RuntimeError):
        return ""
    return "gzip" if data[:2] == b"\x1f\x8b" else ""


def members(data: bytes, kind: str) -> list[tuple[zipfile.ZipInfo | None, bytes]]:
    if kind == "zip":
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            return [(info, archive.read(info)) for info in archive.infolist()]
    return [(None, gzip.decompress(data))]


def has_secret(name: str, data: bytes, pairs) -> bool:
    if any(pattern.search(data) for pattern, _ in pairs):
        return True
    kind = archive_kind(data)
    if not kind:
        return False
    try:
        return any(has_secret(f"{name}!{info.filename if info else 'gunzip'}", inner, pairs) for info, inner in members(data, kind))
    except (zipfile.BadZipFile, OSError, EOFError, RuntimeError, NotImplementedError):
        return False


def redact(name: str, data: bytes, pairs) -> bytes:
    if not has_secret(name, data, pairs):
        return data
    kind = archive_kind(data)
    try:
        if kind == "zip":
            buffer = io.BytesIO()
            with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as target:
                for info, inner in members(data, kind):
                    target.writestr(info, redact(f"{name}!{info.filename}", inner, pairs))
            return buffer.getvalue()
        if kind == "gzip":
            return gzip.compress(redact(f"{name}!gunzip", members(data, kind)[0][1], pairs))
    except (zipfile.BadZipFile, OSError, EOFError, RuntimeError, NotImplementedError):
        pass
    if Path(name.rsplit("!", 1)[-1]).suffix.lower() in MEDIA_SUFFIXES:
        return data
    for pattern, placeholder in pairs:
        data = pattern.sub(lambda _: placeholder, data)
    return data


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Replace the values of secrets with ${VARIABLE} in files about to be uploaded, inside zip and gzip "
                    "archives too. Tokens are replaced only in plain and URL-encoded form; any other form, and any "
                    "secret inside a media file, is left for check-secret-leaks.py to refuse. Only file names are printed.")
    parser.add_argument("--token-env", action="append", default=[], help="Variable holding a token")
    parser.add_argument("--url-env", action="append", default=[], help="Variable holding a repository URL")
    parser.add_argument("paths", nargs="+", type=Path)
    args = parser.parse_args()

    pairs = replacements(args.token_env, args.url_env)
    redacted: list[str] = []
    for root in args.paths:
        if not root.exists():
            continue
        for path in [root] if root.is_file() else sorted(p for p in root.rglob("*") if p.is_file()):
            data = path.read_bytes()
            result = redact(str(path), data, pairs)
            if result != data:
                path.write_bytes(result)
                redacted.append(str(path))
    print(f"Replaced secret values with variable names in {len(redacted)} file(s)")
    for name in redacted:
        print(f"  {name}")


if __name__ == "__main__":
    main()
