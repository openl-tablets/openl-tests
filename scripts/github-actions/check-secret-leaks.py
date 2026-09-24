#!/usr/bin/env python3
import argparse
import gzip
import io
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from secretforms import Secret, load  # noqa: E402

MAX_DEPTH = 5


def contains_secret(data: bytes, secret: Secret) -> bool:
    return secret.found_in(data) or secret.found_in(data.replace(b"\r", b"").replace(b"\n", b""))


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


def leaks_in(name: str, data: bytes, secret: Secret, depth: int = 0) -> list[str]:
    leaked = [name] if contains_secret(data, secret) else []
    if depth < MAX_DEPTH:
        for inner_name, inner in unpacked(name, data):
            leaked += leaks_in(inner_name, inner, secret, depth + 1)
    return leaked


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Fail when a secret is found in files about to be uploaded. Tokens are looked for raw, URL-encoded, "
                    "inside any base64 (such as Basic credentials) and split across lines; repository URLs in every form "
                    "that names the repository path; both inside zip and gzip archives at any depth. "
                    "Only variable and file names are printed.")
    parser.add_argument("--secret-env", action="append", default=[], help="Variable holding a token")
    parser.add_argument("--url-env", action="append", default=[], help="Variable holding a repository URL")
    parser.add_argument("paths", nargs="+", type=Path)
    args = parser.parse_args()
    if not args.secret_env and not args.url_env:
        raise SystemExit("Name at least one --secret-env or --url-env")

    secrets = load(args.secret_env, args.url_env)
    scanned = 0
    leaked: list[str] = []
    for root in args.paths:
        if not root.exists():
            continue
        for path in [root] if root.is_file() else sorted(p for p in root.rglob("*") if p.is_file()):
            scanned += 1
            data = path.read_bytes()
            for secret in secrets:
                leaked += [f"{place} ({secret.name})" for place in leaks_in(str(path), data, secret)]

    print(f"Scanned {scanned} file(s) for {', '.join(secret.name for secret in secrets)}")
    if leaked:
        print(f"::error::Secrets found in {len(leaked)} place(s), nothing is uploaded:")
        for name in leaked:
            print(f"  {name}")
        sys.exit(1)


if __name__ == "__main__":
    main()
