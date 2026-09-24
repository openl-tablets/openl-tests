#!/usr/bin/env python3
import argparse
import base64
import os
import re
import urllib.parse

MIN_TOKEN_LENGTH = 16
MIN_URL_FORM_LENGTH = 6


def token_forms(secret: str) -> list[bytes]:
    raw = secret.encode()
    found = {raw, urllib.parse.quote(secret, safe="").encode()}
    for shift in range(3):
        for encode in (base64.b64encode, base64.urlsafe_b64encode):
            encoded = encode(b"\0" * shift + raw)
            start = (shift * 4 + 2) // 3 + 1 if shift else 0
            found.add(encoded[start:len(encoded) - 4])
    return [form for form in found if len(form) >= MIN_TOKEN_LENGTH]


def url_forms(url: str) -> list[bytes]:
    trimmed = url.strip()
    without_scheme = re.sub(r"^[^@/]*@", "", re.sub(r"^[a-zA-Z]+://", "", trimmed))
    bare = re.sub(r"\.git$", "", without_scheme.rstrip("/"))
    path = bare.split("/", 1)[1] if "/" in bare else bare
    forms = {trimmed, without_scheme, bare + ".git", bare, path + ".git", path, path.replace("/", "%2F")}
    return [form.encode() for form in forms if len(form) >= MIN_URL_FORM_LENGTH and ("/" in form or "%2F" in form)]


class Secret:
    def __init__(self, name: str, forms: list[bytes], ignore_case: bool):
        self.name = name
        self.forms = forms
        self.ignore_case = ignore_case

    def found_in(self, data: bytes) -> bool:
        haystack = data.lower() if self.ignore_case else data
        return any((form.lower() if self.ignore_case else form) in haystack for form in self.forms)


def load(token_variables: list[str], url_variables: list[str]) -> list[Secret]:
    secrets: list[Secret] = []
    for name in token_variables:
        value = os.environ.get(name, "").strip()
        if len(value) < MIN_TOKEN_LENGTH:
            raise SystemExit(f"{name} is empty or too short to look for")
        secrets.append(Secret(name, token_forms(value), False))
    for name in url_variables:
        value = os.environ.get(name, "").strip()
        if not value:
            raise SystemExit(f"{name} is empty, there is nothing to look for")
        secrets.append(Secret(name, url_forms(value), True))
    return secrets


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Print ::add-mask:: commands for the forms of the secrets GitHub does not mask by itself: Basic "
                    "credentials made of a token and a login, and every form of a repository URL. Fails when a secret is empty.")
    parser.add_argument("--basic", action="append", default=[], metavar="TOKEN_VARIABLE=LOGIN")
    parser.add_argument("--url-env", action="append", default=[])
    args = parser.parse_args()
    pairs = [entry.split("=", 1) for entry in args.basic]
    load([name for name, _ in pairs], args.url_env)
    for name, login in pairs:
        credentials = f"{login}:{os.environ[name].strip()}".encode()
        print(f"::add-mask::{base64.b64encode(credentials).decode()}")
    for name in args.url_env:
        forms = {form.decode() for form in url_forms(os.environ[name])}
        for form in sorted(forms | {form.lower() for form in forms}):
            print(f"::add-mask::{form}")


if __name__ == "__main__":
    main()
