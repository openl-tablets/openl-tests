#!/usr/bin/env python3
import argparse
import json
import os
import subprocess

STUDIO_REPOSITORY = "ghcr.io/openl-tablets/webstudio"
WS_REPOSITORY = "ghcr.io/openl-tablets/ws"
PLATFORM = "linux/amd64"
MOVING_TAG = "latest"


def inspect(reference: str) -> dict:
    completed = subprocess.run(
        ["docker", "buildx", "imagetools", "inspect", reference, "--format", "{{json .}}"],
        capture_output=True, text=True,
    )
    if completed.returncode != 0:
        raise SystemExit(f"::error::Image {reference} does not exist in ghcr.io or cannot be read: {completed.stderr.strip()}")
    return json.loads(completed.stdout)


def resolve(repository: str, tag: str, moving: bool) -> dict:
    requested = f"{repository}:{tag}"
    inspected = inspect(requested)
    image = inspected.get("image") or {}
    config = image.get(PLATFORM, image) if isinstance(image, dict) else {}
    labels = (config.get("config") or {}).get("Labels") or {}
    digest = inspected["manifest"]["digest"]
    return {
        "image": f"{repository}@{digest}",
        "requested": requested,
        "digest": digest,
        "revision": labels.get("org.opencontainers.image.revision", ""),
        "created": labels.get("org.opencontainers.image.created", ""),
        "moving": moving,
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Resolve the Studio and Rule Services tags to digests once, so every shard tests the same build even when "
                    "the tag is republished during the run, and write the images with their revision and build time as step outputs."
    )
    parser.add_argument("--tag", required=True, help="Studio tag; Rule Services uses <tag>-all.")
    args = parser.parse_args()

    moving = args.tag == MOVING_TAG
    studio = resolve(STUDIO_REPOSITORY, args.tag, moving)
    ws = resolve(WS_REPOSITORY, f"{args.tag}-all", moving)
    for image in (studio, ws):
        print(f"{image['requested']} -> {image['image']} (revision {image['revision'] or 'unknown'}, built {image['created'] or 'unknown'})")
    if studio["revision"] and ws["revision"] and studio["revision"] != ws["revision"]:
        print(f"::warning::{studio['requested']} and {ws['requested']} are built from different openl-tablets revisions: "
              f"{studio['revision']} and {ws['revision']}")

    outputs = {
        "application_version": args.tag,
        "studio_image": studio["image"],
        "ws_image": ws["image"],
        "images": json.dumps([studio, ws]),
    }
    output_file = os.environ.get("GITHUB_OUTPUT")
    if output_file:
        with open(output_file, "a", encoding="utf-8") as handle:
            for key, value in outputs.items():
                handle.write(f"{key}={value}\n")


if __name__ == "__main__":
    main()
