#!/usr/bin/env python3
"""Export an Anchor (Android/KMP) repository into ONE shareable Markdown file.

From the project root, run:
    py export-context.py

The output is ./anchor-project-context.md. Upload that one file to ChatGPT.
Source files are never changed. Build caches, generated exports, and common
credentials are excluded; nevertheless REVIEW the output before sharing it.

Options:
    py export-context.py --output project-handover.md
    py export-context.py --max-file-mb 5
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
from pathlib import Path

DEFAULT_OUTPUT = "anchor-project-context.md"
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
    ".html", ".css", ".js", ".ts", ".tsx", ".svg", ".csv",
    ".gitignore", ".gitattributes", ".editorconfig", ".pro", ".rules",
    ".conf", ".cfg", ".ini", ".graphql", ".mdx", ".c", ".cpp",
    ".hpp", ".def",
}
TEXT_NAMES = {
    "gradlew", "gradlew.bat", "Dockerfile", "Makefile", "LICENSE",
    "NOTICE", "Gemfile", "Podfile", "README", ".gitignore",
    ".gitattributes", ".editorconfig",
}
SECRET_EXTENSIONS = {
    ".keystore", ".jks", ".p12", ".pfx", ".pem", ".key",
    ".mobileprovision", ".cer", ".p8",
}
SECRET_NAMES = {
    "local.properties", "google-services.json", "googleservice-info.plist",
    "id_rsa", "id_ed25519",
}
SECRET_VALUE_PATTERN = re.compile(
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


def scan(root: Path, output: Path):
    source_files = []
    other_files = []
    for base, dirs, filenames in os.walk(root, followlinks=False):
        dirs[:] = sorted(
            d for d in dirs
            if d not in EXCLUDED_DIRS and not (Path(base) / d).is_symlink()
        )
        for filename in sorted(filenames):
            path = Path(base) / filename
            if path.is_symlink() or path.resolve() in {output, output.with_suffix(".md.tmp")} or is_secret(path):
                continue
            relative = path.relative_to(root).as_posix()
            try:
                size = path.stat().st_size
            except OSError:
                continue
            if path.suffix.lower() in TEXT_EXTENSIONS or filename in TEXT_NAMES:
                source_files.append((relative, path, size))
            else:
                other_files.append((relative, size))
    return source_files, other_files


def safe_fence(content: str) -> str:
    """Choose a fence longer than every tilde sequence in the source text."""
    longest = max((len(m.group()) for m in re.finditer(r"~{4,}", content)), default=0)
    return "~" * max(4, longest + 1)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", default=DEFAULT_OUTPUT,
                        help=f"Output Markdown filename (default: {DEFAULT_OUTPUT})")
    parser.add_argument("--max-file-mb", type=float, default=5.0,
                        help="Skip individual text files larger than this (default: 5 MB)")
    args = parser.parse_args()
    if args.max_file_mb <= 0:
        parser.error("--max-file-mb must be positive")

    root = Path.cwd().resolve()
    output = (root / args.output).resolve()
    if output.suffix.lower() != ".md" or output.parent != root:
        parser.error("--output must be a Markdown filename inside the repository root")
    max_bytes = int(args.max_file_mb * 1024 * 1024)
    source_files, other_files = scan(root, output)

    files_to_write = []
    skipped = []
    for relative, path, size in source_files:
        if size > max_bytes:
            skipped.append((relative, "too large"))
            continue
        try:
            raw = path.read_bytes()
            if b"\0" in raw:
                skipped.append((relative, "binary data"))
                continue
            content = raw.decode("utf-8-sig", errors="replace")
            content = SECRET_VALUE_PATTERN.sub(r"\1[REDACTED — check original locally]", content)
            files_to_write.append((relative, content, size))
        except OSError as error:
            skipped.append((relative, f"read error: {error}"))

    manifest = [
        "# Anchor — complete source context\n\n",
        f"Repository: `{root.name}`  \n",
 f"Git branch: `{git(root, 'branch', '--show-current')}`  \n",
        f"HEAD: `{git(root, 'rev-parse', '--short', 'HEAD')}`\n\n",
        "## Git status (snapshot only)\n\n",
    ]
    status = git(root, "status", "--short") or "(clean)"
    manifest.extend(["~~~~text\n", status, "\n~~~~\n\n"])
    manifest.extend(["## Recent commits\n\n", "~~~~text\n",
                     git(root, "log", "-12", "--oneline"), "\n~~~~\n\n"])
    manifest.append("## Embedded source/configuration files\n\n")
    manifest.extend(f"- `{rel}` ({size:,} bytes)\n" for rel, _, size in files_to_write)
    manifest.append("\n## Other resources (listed, not embedded)\n\n")
    manifest.extend(f"- `{rel}` ({size:,} bytes)\n" for rel, size in other_files)
    if skipped:
        manifest.append("\n## Text files not embedded\n\n")
        manifest.extend(f"- `{rel}`: {reason}\n" for rel, reason in skipped)
    manifest.append(
        "\n> This is a source snapshot, not a runnable checkout. Generated files, "
        "binary assets, build outputs, and obvious credentials are excluded. "
        "Review the file for sensitive information before sharing.\n\n"
        "---\n\n# Source files\n\n"
    )

    temp_output = output.with_suffix(".md.tmp")
    try:
        with temp_output.open("w", encoding="utf-8", newline="\n") as stream:
            stream.writelines(manifest)
            for relative, content, _size in files_to_write:
                fence = safe_fence(content)
                stream.write(f"## `{relative}`\n\n{fence}text\n")
                stream.write(content)
                if not content.endswith("\n"):
                    stream.write("\n")
                stream.write(f"{fence}\n\n")
        temp_output.replace(output)
    except BaseException:
        temp_output.unlink(missing_ok=True)
        raise

    print(f"Created: {output}")
    print(f"Embedded {len(files_to_write)} source files; listed {len(other_files)} binary/other files.")
    print(f"Skipped {len(skipped)} oversized or unreadable source files.")
    print(f"File size: {output.stat().st_size:,} bytes")
    print("Upload this ONE Markdown file to your next ChatGPT conversation.")
    print("Check the file for private data before sharing.")


if __name__ == "__main__":
    main()
