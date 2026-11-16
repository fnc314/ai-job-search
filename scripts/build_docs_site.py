#!/usr/bin/env python3
"""Builds and assembles the public documentation site for GitHub Pages.

Aggregates:
- Landing page with Material 3 design tokens.
- Root README.md converted to styled HTML.
- Generated Dokka HTML documentation from root dokka/.
- Generated Mise task docs from docs/mise/.
- CNAME and .nojekyll for GitHub Pages custom domain routing.
"""

from __future__ import annotations

import argparse
import html
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path


def get_current_git_version(repo_root: Path) -> str:
    """Detect current highest semantic version from git tags or fallback."""
    try:
        proc = subprocess.run(
            ["git", "tag", "-l", "version/*"],
            cwd=repo_root,
            capture_output=True,
            text=True,
            check=True,
        )
        tags = [line.strip().removeprefix("version/") for line in proc.stdout.splitlines() if line.strip()]
        if not tags:
            return "version/2.0.0"
        # Sort semantic versions
        def parse_semver(v: str) -> tuple[int, ...]:
            parts = v.split(".")
            nums = []
            for p in parts:
                digits = re.match(r"^\d+", p)
                nums.append(int(digits.group(0)) if digits else 0)
            return tuple(nums)
        tags.sort(key=parse_semver)
        return f"version/{tags[-1]}"
    except Exception:
        return "version/2.0.0"


def convert_markdown_to_simple_html(markdown_text: str) -> str:
    """Converts a subset of markdown (headings, code blocks, lists, links, paragraphs) into valid HTML."""
    lines = markdown_text.splitlines()
    html_lines: list[str] = []
    in_code_block = False
    code_lang = ""
    in_list = False

    for line in lines:
        if line.startswith("```"):
            if in_code_block:
                html_lines.append("</code></pre>")
                in_code_block = False
            else:
                if in_list:
                    html_lines.append("</ul>")
                    in_list = False
                code_lang = line[3:].strip()
                html_lines.append(f'<pre><code class="language-{html.escape(code_lang)}">')
                in_code_block = True
            continue

        if in_code_block:
            html_lines.append(html.escape(line))
            continue

        # Lists
        if line.strip().startswith("- ") or line.strip().startswith("* "):
            if not in_list:
                html_lines.append("<ul>")
                in_list = True
            item_text = line.strip()[2:]
            html_lines.append(f"<li>{format_inline_markdown(item_text)}</li>")
            continue
        else:
            if in_list and line.strip() == "":
                html_lines.append("</ul>")
                in_list = False

        # Headings
        if line.startswith("# "):
            html_lines.append(f"<h1>{format_inline_markdown(line[2:])}</h1>")
        elif line.startswith("## "):
            html_lines.append(f"<h2>{format_inline_markdown(line[3:])}</h2>")
        elif line.startswith("### "):
            html_lines.append(f"<h3>{format_inline_markdown(line[4:])}</h3>")
        elif line.startswith("#### "):
            html_lines.append(f"<h4>{format_inline_markdown(line[5:])}</h4>")
        elif line.strip() == "":
            html_lines.append("<br/>")
        else:
            html_lines.append(f"<p>{format_inline_markdown(line)}</p>")

    if in_code_block:
        html_lines.append("</code></pre>")
    if in_list:
        html_lines.append("</ul>")

    return "\n".join(html_lines)


def format_inline_markdown(text: str) -> str:
    """Formats bold, italics, inline code, and links in a line of text."""
    # Escape basic HTML first
    escaped = html.escape(text)

    # Inline code: `code`
    escaped = re.sub(r"`([^`]+)`", r"<code>\1</code>", escaped)

    # Bold: **text**
    escaped = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", escaped)

    # Italics: *text*
    escaped = re.sub(r"\*([^*]+)\*", r"<em>\1</em>", escaped)

    # Markdown links: [text](url)
    escaped = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', escaped)

    return escaped


def wrap_with_material_layout(
    title: str,
    body_content: str,
    base_prefix: str,
    version: str,
    current_nav: str = "",
) -> str:
    """Wraps body HTML in the standard Material 3 themed frame."""
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{html.escape(title)} - AI Job Search & Application OS</title>
  <link rel="icon" type="image/svg+xml" href="{base_prefix}favicon.svg">
  <link rel="alternate icon" type="image/x-icon" href="{base_prefix}favicon.ico">
  <link rel="apple-touch-icon" href="{base_prefix}apple-touch-icon.png">
  <link rel="stylesheet" href="{base_prefix}css/material3-tokens.css">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inconsolata:wght@400;600;700&display=swap" rel="stylesheet">
  <style>
    * {{ box-sizing: border-box; margin: 0; padding: 0; }}
    body {{
      font-family: var(--md-sys-font-body);
      background-color: var(--md-sys-color-background);
      color: var(--md-sys-color-on-background);
      line-height: 1.6;
    }}
    header.app-bar {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 2rem;
      background-color: var(--md-sys-color-surface-container);
      border-bottom: 1px solid var(--md-sys-color-outline-variant);
      position: sticky;
      top: 0;
      z-index: 100;
    }}
    .brand {{
      display: flex;
      align-items: center;
      gap: 0.75rem;
      font-family: var(--md-sys-font-heading);
      font-weight: 700;
      font-size: 1.25rem;
      color: var(--md-sys-color-primary);
      text-decoration: none;
    }}
    .badge {{
      display: inline-block;
      padding: 0.2rem 0.6rem;
      border-radius: var(--md-sys-shape-corner-full);
      font-size: 0.75rem;
      font-weight: 600;
      font-family: var(--md-sys-font-code);
      background-color: var(--md-sys-color-primary-container);
      color: var(--md-sys-color-on-primary-container);
    }}
    nav.app-nav {{
      display: flex;
      align-items: center;
      gap: 1.25rem;
    }}
    nav.app-nav a {{
      color: var(--md-sys-color-on-surface);
      text-decoration: none;
      font-weight: 500;
      font-size: 0.95rem;
    }}
    nav.app-nav a:hover, nav.app-nav a.active {{
      color: var(--md-sys-color-primary);
    }}
    .theme-toggle {{
      background: none;
      border: 1px solid var(--md-sys-color-outline-variant);
      color: var(--md-sys-color-on-surface);
      padding: 0.4rem 0.8rem;
      border-radius: var(--md-sys-shape-corner-small);
      cursor: pointer;
      font-family: var(--md-sys-font-code);
      font-size: 0.85rem;
    }}
    main.doc-content {{
      max-width: 980px;
      margin: 2rem auto 4rem;
      padding: 2.5rem;
      background-color: var(--md-sys-color-surface-container-lowest);
      border: 1px solid var(--md-sys-color-outline-variant);
      border-radius: var(--md-sys-shape-corner-medium);
      box-shadow: var(--md-sys-elevation-1);
    }}
    main.doc-content h1, main.doc-content h2, main.doc-content h3 {{
      font-family: var(--md-sys-font-heading);
      color: var(--md-sys-color-primary);
      margin-top: 1.5rem;
      margin-bottom: 0.75rem;
    }}
    main.doc-content h1 {{ font-size: 2.2rem; border-bottom: 1px solid var(--md-sys-color-outline-variant); padding-bottom: 0.5rem; }}
    main.doc-content h2 {{ font-size: 1.6rem; border-bottom: 1px solid var(--md-sys-color-outline-variant); padding-bottom: 0.35rem; }}
    main.doc-content h3 {{ font-size: 1.25rem; }}
    main.doc-content p {{ margin-bottom: 1rem; color: var(--md-sys-color-on-surface); }}
    main.doc-content ul {{ margin-left: 2rem; margin-bottom: 1rem; }}
    main.doc-content li {{ margin-bottom: 0.35rem; }}
    main.doc-content a {{ color: var(--md-sys-color-primary); text-decoration: underline; }}
    main.doc-content pre {{
      background-color: var(--md-sys-color-surface-container);
      padding: 1rem;
      border-radius: var(--md-sys-shape-corner-small);
      border: 1px solid var(--md-sys-color-outline-variant);
      overflow-x: auto;
      margin: 1rem 0;
      font-family: var(--md-sys-font-code);
      font-size: 0.9rem;
    }}
    main.doc-content code {{
      font-family: var(--md-sys-font-code);
      background-color: var(--md-sys-color-surface-container);
      padding: 0.15rem 0.35rem;
      border-radius: 4px;
      font-size: 0.9em;
    }}
    main.doc-content pre code {{
      background: none;
      padding: 0;
    }}
    footer.app-footer {{
      text-align: center;
      padding: 2.5rem 1rem;
      background-color: var(--md-sys-color-surface-container-low);
      border-top: 1px solid var(--md-sys-color-outline-variant);
      font-size: 0.9rem;
      color: var(--md-sys-color-on-surface-variant);
    }}
    footer.app-footer a {{
      color: var(--md-sys-color-primary);
      text-decoration: none;
    }}
  </style>
</head>
<body>
  <header class="app-bar">
    <a href="{base_prefix}index.html" class="brand">
      <span>AI Job Search & Application OS</span>
      <span class="badge">{html.escape(version)}</span>
    </a>
    <nav class="app-nav">
      <a href="{base_prefix}readme/index.html" class="{'active' if current_nav == 'readme' else ''}">Overview</a>
      <a href="{base_prefix}dokka/index.html" class="{'active' if current_nav == 'dokka' else ''}">API Docs (Dokka)</a>
      <a href="{base_prefix}tasks/index.html" class="{'active' if current_nav == 'tasks' else ''}">Task Catalog</a>
      <a href="https://github.com/fnc314/ai-job-search" target="_blank" rel="noopener">GitHub</a>
      <button class="theme-toggle" onclick="toggleTheme()">🌓 Theme</button>
    </nav>
  </header>

  <main class="doc-content">
    {body_content}
  </main>

  <footer class="app-footer">
    <p>
      Designed and engineered by <a href="https://www.fnc314.dev" target="_blank" rel="noopener">Franco N. Colaizzi</a> ·
      <a href="https://github.com/fnc314/ai-job-search" target="_blank" rel="noopener">ai-job-search</a> ·
      Published on <a href="https://ai-job-tracker.fnc314.dev">ai-job-tracker.fnc314.dev</a>
    </p>
  </footer>

  <script>
    function toggleTheme() {{
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('theme', next);
    }}
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {{
      document.documentElement.setAttribute('data-theme', savedTheme);
    }}
  </script>
</body>
</html>
"""


def build_site(output_dir: Path, domain: str) -> None:
    repo_root = Path(__file__).resolve().parent.parent
    output_dir = output_dir.resolve()
    print(f"📦 Building public documentation site into {output_dir}...")

    # Ensure output directory exists and is clean
    if output_dir.exists():
        shutil.rmtree(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    version = get_current_git_version(repo_root)

    # 1. Copy CSS material tokens and favicon assets
    css_dest = output_dir / "css"
    css_dest.mkdir(parents=True, exist_ok=True)
    css_src = repo_root / "docs" / "site" / "css" / "material3-tokens.css"
    if css_src.exists():
        shutil.copy2(css_src, css_dest / "material3-tokens.css")
    else:
        print(f"⚠️ Warning: {css_src} not found.")

    for asset_name in ["favicon.svg", "favicon.ico", "apple-touch-icon.png"]:
        src_asset = repo_root / "docs" / "site" / asset_name
        if not src_asset.exists():
            src_asset = repo_root / "frontend" / "apps" / "web" / "src" / "webMain" / "resources" / asset_name
        if src_asset.exists():
            shutil.copy2(src_asset, output_dir / asset_name)
            print(f"✓ Copied {asset_name} to root")

    # 2. Render Landing Page
    landing_template = repo_root / "docs" / "site" / "templates" / "landing.html"
    if landing_template.exists():
        landing_content = landing_template.read_text(encoding="utf-8")
        landing_content = landing_content.replace("{{VERSION}}", version)
        (output_dir / "index.html").write_text(landing_content, encoding="utf-8")
        print("✓ Rendered index.html (landing page)")
    else:
        print(f"❌ Error: {landing_template} not found.")

    # 3. Render Root README.md
    readme_path = repo_root / "README.md"
    if readme_path.exists():
        readme_md = readme_path.read_text(encoding="utf-8")
        readme_html = convert_markdown_to_simple_html(readme_md)
        wrapped_readme = wrap_with_material_layout(
            title="System Architecture & Overview",
            body_content=readme_html,
            base_prefix="../",
            version=version,
            current_nav="readme",
        )
        readme_dest = output_dir / "readme"
        readme_dest.mkdir(parents=True, exist_ok=True)
        (readme_dest / "index.html").write_text(wrapped_readme, encoding="utf-8")
        print("✓ Rendered readme/index.html")

    # 4. Copy Dokka HTML
    dokka_src = repo_root / "dokka"
    if dokka_src.exists() and (dokka_src / "index.html").exists():
        dokka_dest = output_dir / "dokka"
        shutil.copytree(dokka_src, dokka_dest)
        print("✓ Copied aggregated Dokka documentation to dokka/")
    else:
        print(f"⚠️ Warning: root dokka/ directory not found or empty at {dokka_src}.")
        # Create placeholder
        dokka_dest = output_dir / "dokka"
        dokka_dest.mkdir(parents=True, exist_ok=True)
        placeholder = wrap_with_material_layout(
            title="Dokka Documentation",
            body_content="<h1>Dokka Documentation</h1><p>Run <code>mise run docs:generate</code> to populate the full multiplatform KDocs.</p>",
            base_prefix="../",
            version=version,
            current_nav="dokka",
        )
        (dokka_dest / "index.html").write_text(placeholder, encoding="utf-8")

    # 5. Render Mise Task Reference
    tasks_src = repo_root / "docs" / "mise"
    tasks_dest = output_dir / "tasks"
    tasks_dest.mkdir(parents=True, exist_ok=True)
    task_files = sorted(tasks_src.glob("*.md")) if tasks_src.exists() else []

    task_index_items = []
    for tf in task_files:
        content = tf.read_text(encoding="utf-8")
        html_body = convert_markdown_to_simple_html(content)
        task_out = wrap_with_material_layout(
            title=f"Task: {tf.stem}",
            body_content=html_body,
            base_prefix="../",
            version=version,
            current_nav="tasks",
        )
        out_file = tasks_dest / f"{tf.stem}.html"
        out_file.write_text(task_out, encoding="utf-8")
        task_index_items.append(f'<li><a href="{tf.stem}.html"><strong>{tf.stem}</strong></a></li>')

    task_catalog_body = f"""<h1>Mise Task Catalog</h1>
<p>The workspace uses <strong>Mise</strong> for unified task automation across all subprojects, build tools, tests, and cloud deployments.</p>
<h2>All Registered Tasks ({len(task_files)})</h2>
<ul>
{''.join(task_index_items)}
</ul>
"""
    task_index_html = wrap_with_material_layout(
        title="Task Catalog",
        body_content=task_catalog_body,
        base_prefix="../",
        version=version,
        current_nav="tasks",
    )
    (tasks_dest / "index.html").write_text(task_index_html, encoding="utf-8")
    print(f"✓ Rendered {len(task_files)} task doc pages into tasks/")

    # 6. Inject CNAME & .nojekyll
    if domain:
        (output_dir / "CNAME").write_text(f"{domain}\n", encoding="utf-8")
        print(f"✓ Configured CNAME with {domain}")

    (output_dir / ".nojekyll").touch()
    print("✓ Created .nojekyll")

    print(f"\n🎉 Successfully assembled public site at {output_dir}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Assemble documentation site for GitHub Pages.")
    parser.add_argument(
        "--output",
        default="build/gh-pages",
        help="Target output directory (default: build/gh-pages)",
    )
    parser.add_argument(
        "--domain",
        default="ai-job-tracker.fnc314.dev",
        help="Custom CNAME domain (default: ai-job-tracker.fnc314.dev)",
    )
    args = parser.parse_args()
    build_site(Path(args.output), args.domain)


if __name__ == "__main__":
    main()
