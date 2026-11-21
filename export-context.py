#!/usr/bin/env python3
"""Export a Kotlin/Android/KMP repository as shareable, split Markdown context.

Usage (from the repository root):
    py export-context.py
    py export-context.py --max-chars 100000

Standard-library only. Does not execute project code or alter project files.
Review generated files for sensitive values before sharing.
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import zipfile
from pathlib import Path

EXCLUDED_DIRS = {
    ".git", ".gradle", ".idea", ".kotlin", ".cxx", ".vs", ".vscode",
    ".fleet", ".swiftpm", ".build", ".next", "__pycache__", "build",
    "out", "target", "node_modules", "venv", ".venv", "dist", "Pods",
    "DerivedData", "captures", "anchor-context-export", ".anchor-context-export",
}

TEXT_EXTENSIONS = {
    ".kt", ".kts", ".java", ".swift", ".m", ".mm", ".h", ".xml",
    ".toml", ".properties", ".gradle", ".md", ".txt", ".json", ".yaml",
    ".yml", ".sh", ".bash", ".bat", ".cmd", ".ps1", ".py", ".sql",
    ".proto", ".plist", ".pbxproj", ".xcworkspacedata", ".entitlements",
    ".storyboard", ".strings", ".stringsdict", ".xcconfig", ".xcscheme",
    ".html", ".css", ".js", ".ts", ".tsx", ".svg", ".csv", ".gitignore",
    ".gitattributes", ".editorconfig", ".pro", ".rules", ".conf", ".cfg",
    ".ini", ".graphql", ".mdx", ".c", ".cpp", ".hpp", ".def",
}
TEXT_FILENAMES = {
    "gradlew", "gradlew.bat", "Dockerfile", "Makefile", "LICENSE", "NOTICE",
    "Gemfile", "Podfile", "README", ".gitignore", ".gitattributes", ".editorconfig",
}
SECRET_EXTENSIONS = {".keystore", ".jks", ".p12", ".pfx", ".pem", ".key", ".mobileprovision", ".cer", ".p8"}
SECRET_NAMES = {"local.properties", "google-services.json", "googleservice-info.plist", "id_rsa", "id_ed25519"}
SECRET_KEY_PATTERN = re.compile(
    r"(?im)^(\s*['\"]?[\w.-]*(?:password|passwd|secret|api[_-]?key|"
    r"access[_-]?token|auth[_-]?token|private[_-]?key|storepassword|"
    r"keypassword|client[_-]?secret)[\w.-]*['\"]?\s*[:=]\s*)([^\r\n]+)$"
)


def is_secret(path: Path) -> bool:
    name = path.name.lower()
    return (
        name in SECRET_NAMES
        or path.suffix.lower() in SECRET_EXTENSIONS
        or name == ".env"
        or name.startswith(".env.")
        or name.startswith("credentials")
        or name.startswith("secrets.")
        or name.startswith("secret.")
        or "service-account" in name
    )


def git(root: Path, *args: str) -> str:
    try:
        result = subprocess.run(
            ["git", *args], cwd=root, capture_output=True, text=True,
            encoding="utf-8", errors="replace", timeout=10, check=False,
        )
        return result.stdout.strip() if result.returncode == 0 else "(unavailable)"
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return "(unavailable)"


def scan(root: Path):
    text_files = []
    other_files = []
    for base, dirs, files in os.walk(root, followlinks=False):
        dirs[:] = sorted(
            d for d in dirs if d not in EXCLUDED_DIRS and not (Path(base) / d).is_symlink()
        )
        for name in sorted(files):
            path = Path(base) / name
            if path.is_symlink() or is_secret(path):
                continue
            relative = path.relative_to(root).as_posix()
            try:
                size = path.stat().st_size
            except OSError:
                continue
            if path.suffix.lower() in TEXT_EXTENSIONS or name in TEXT_FILENAMES:
                text_files.append((relative, path, size))
            else:
                other_files.append((relative, size))
    return text_files, other_files


def chunks_of_lines(text: str, char_limit: int):
    """Break very large files without losing any lines."""
    result, current, length = [], [], 0
    for line in text.splitlines(keepends=True):
        if current and length + len(line) > char_limit:
            result.append("".join(current))
            current, length = [], 0
        if len(line) > char_limit:
            if current:
                result.append("".join(current))
                current, length = [], 0
            result.extend(line[i:i + char_limit] for i in range(0, len(line), char_limit))
        else:
            current.append(line)
            length += len(line)
    if current:
        result.append("".join(current))
    return result or [""]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--max-chars", type=int, default=120_000,
                        help="Approximate maximum characters per Markdown part (default: 120000)")
    parser.add_argument("--max-file-mb", type=float, default=5.0,
                        help="Skip individual text files larger than this (default: 5 MB)")
    args = parser.parse_args()
    if args.max_chars < 12_000 or args.max_file_mb <= 0:
        parser.error("--max-chars must be >= 12000 and --max-file-mb must be positive")

    root = Path.cwd().resolve()
    out = root / "anchor-context-export"
    out.mkdir(exist_ok=True)
    for old in out.glob("anchor-context-part-*.md"):
        old.unlink()

    text_files, other_files = scan(root)
    head = git(root, "rev-parse", "--short", "HEAD")
    branch = git(root, "branch", "--show-current")
    status = git(root, "status", "--short")
    log = git(root, "log", "-12", "--oneline")

    manifest = [
        "# Anchor repository context export\n",
        f"Root folder: `{root.name}`\n",
        f"Git branch: `{branch}` | HEAD: `{head}`\n",
        "\n## Git status (snapshot only)\n",
        "~~~~text\n" + (status or "(clean)") + "\n~~~~\n",
        "\n## Recent commits\n",
        "~~~~text\n" + log + "\n~~~~\n",
        "\n## Text/source/configuration files exported\n",
    ]
    manifest.extend(f"- `{rel}` ({size:,} bytes)\n" for rel, _p, size in text_files)
    manifest.extend(["\n## Non-text resources listed but not embedded\n"])
    manifest.extend(f"- `{rel}` ({size:,} bytes)\n" for rel, size in other_files)
    manifest.append(
        "\nGenerated folders, local IDE files, caches, and likely secret files were excluded. "
        "Binary assets are listed, not reproduced. Please check exported source for "
        "hardcoded keys, identifiers, or private data before sharing.\n"
    )
    (out / "00-INDEX.md").write_text("".join(manifest), encoding="utf-8")

    part_no = 0
    parts = []
    current = ["# Anchor source context — part 1\n\n"]
    used = len(current[0])
    max_file_bytes = int(args.max_file_mb * 1024 * 1024)
    embedded = 0
    skipped = []

    def save_part():
        nonlocal part_no, current, used
        if len(current) <= 1:
            return
        part_no += 1
        filename = f"anchor-context-part-{part_no:02d}.md"
        (out / filename).write_text("".join(current), encoding="utf-8")
        parts.append(filename)
        current = [f"# Anchor source context — part {part_no + 1}\n\n"]
        used = len(current[0])

    for rel, path, size in text_files:
        if size > max_file_bytes:
            skipped.append((rel, "exceeds --max-file-mb"))
            continue
        try:
            raw = path.read_bytes()
            if b"\0" in raw:
                skipped.append((rel, "binary content"))
                continue
            decoded = raw.decode("utf-8-sig", errors="replace")
        except OSError as error:
            skipped.append((rel, f"unreadable: {error}"))
            continue
        decoded = SECRET_KEY_PATTERN.sub(r"\1[REDACTED — review original locally]", decoded)
        fragments = chunks_of_lines(decoded, max(1000, args.max_chars - 250))
        for i, fragment in enumerate(fragments, start=1):
            label = f"## `{rel}`" + (f" (segment {i}/{len(fragments)})" if len(fragments) > 1 else "")
            block = label + "\n\n~~~~text\n" + fragment + ("" if fragment.endswith("\n") else "\n") + "~~~~\n\n"
            if used + len(block) > args.max_chars and len(current) > 1:
                save_part()
            current.append(block)
            used += len(block)
        embedded += 1
    save_part()

    if skipped:
        (out / "00-INDEX.md").open("a", encoding="utf-8").write(
            "\n## Text files not embedded\n" +
            "".join(f"- `{rel}` — {reason}\n" for rel, reason in skipped)
        )

    readme = (
        "# Export instructions\n\n"
        "Upload `00-INDEX.md` and all `anchor-context-part-XX.md` files into your new ChatGPT chat, "
        "or upload `anchor-project-context.zip` if ZIP attachments are supported.\n\n"
        "The exporter copies text only, does not execute Gradle, and excludes common build outputs, "
        "IDE metadata, and obvious secrets. It cannot detect all private information; REVIEW BEFORE UPLOADING.\n"
    )
    (out / "README.md").write_text(readme, encoding="utf-8")
    zip_path = out / "anchor-project-context.zip"
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for filename in ["README.md", "00-INDEX.md", *parts]:
            archive.write(out / filename, arcname=filename)

    print(f"Repository: {root}")
    print(f"HEAD: {head} ({branch})")
    print(f"Embedded {embedded} source/config files in {len(parts)} Markdown part(s).")
    print(f"Skipped {len(skipped)} oversized/unreadable text files; listed {len(other_files)} non-text files.")
    print(f"Export folder: {out}")
    print(f"ZIP to upload: {zip_path}")
    print("IMPORTANT: Review the export for sensitive content before uploading.")


if __name__ == "__main__":
    main()
