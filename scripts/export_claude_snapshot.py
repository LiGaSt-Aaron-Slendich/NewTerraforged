"""Parse Claude share browser snapshot into JSON + TXT export."""
import json
import re
from pathlib import Path

SNAPSHOT = Path(
    r"C:\Users\Апро\.cursor\browser-logs\snapshot-2026-06-17T21-24-29-339Z-l71qao.log"
)
OUT_DIR = Path(__file__).resolve().parents[1] / "docs" / "claude-imports"
SOURCE_URL = "https://claude.ai/share/bc573447-7f92-46fc-b469-896ea5edd41f"


def main() -> None:
    text = SNAPSHOT.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    messages: list[dict] = []
    i = 0
    while i < len(lines):
        m = re.search(r'name: "(You said:|Claude responded:)(.*)"', lines[i])
        if m:
            role = "user" if m.group(1).startswith("You") else "assistant"
            title = m.group(2).strip()
            body_parts: list[str] = []
            j = i + 1
            while j < len(lines):
                if "role: heading" in lines[j]:
                    break
                nm = re.search(r"^\s+name: (.+)$", lines[j])
                if nm:
                    val = nm.group(1).strip('"')
                    skip = (
                        not val
                        or val == "Claude"
                        or val.startswith("You said:")
                        or val.startswith("Claude responded:")
                    )
                    if not skip and val not in body_parts:
                        body_parts.append(val)
                j += 1
            messages.append({"role": role, "title": title, "content": body_parts})
            i = j
            continue
        i += 1

    files = [
        f.rstrip('"').strip()
        for f in dict.fromkeys(re.findall(r"Наступний файл: (.+)", text))
    ]

    meta = {
        "source_url": SOURCE_URL,
        "title": "Ruthenium / Megastructure Generator — tile system and procedural cities",
        "project": "Окремий Minecraft mod (не NewTerraForged)",
        "language": "uk",
        "message_count": len(messages),
        "generated_files_queue": files,
        "export_note": (
            "Partial export from Claude share page accessibility snapshot. "
            "Attachments and full code blocks are hidden in shared chats."
        ),
    }

    export = {"meta": meta, "messages": messages}
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    json_path = OUT_DIR / "bc573447-megastructure-chat.json"
    json_path.write_text(json.dumps(export, ensure_ascii=False, indent=2), encoding="utf-8")

    txt_lines = [
        "=" * 72,
        "CLAUDE CHAT EXPORT",
        f"URL: {meta['source_url']}",
        f"Topic: {meta['title']}",
        f"Messages extracted: {len(messages)}",
        "=" * 72,
        "",
    ]
    for idx, msg in enumerate(messages, 1):
        who = "USER" if msg["role"] == "user" else "CLAUDE"
        txt_lines.append(f"--- [{idx}] {who} ---")
        if msg["title"]:
            txt_lines.append(msg["title"])
        txt_lines.extend(msg["content"])
        txt_lines.append("")

    txt_lines.extend(["=" * 72, "GENERATED / PLANNED FILES (in order mentioned)"])
    txt_lines.extend(f"  - {f}" for f in files)
    txt_lines.append("=" * 72)

    txt_path = OUT_DIR / "bc573447-megastructure-chat.txt"
    txt_path.write_text("\n".join(txt_lines), encoding="utf-8")

    print(f"Wrote {json_path}")
    print(f"Wrote {txt_path}")
    print(f"messages={len(messages)} files={len(files)}")


if __name__ == "__main__":
    main()
