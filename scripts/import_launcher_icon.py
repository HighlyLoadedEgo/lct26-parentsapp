#!/usr/bin/env python3
"""Package the raccoon/jar artwork as launcher resources. Requires Pillow."""

import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw, __version__ as pillow_version

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
DOCS = ROOT / "docs/design/assets"
SOURCE = DOCS / "sources/launcher-raccoon-jar"
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def record(path):
    data = path.read_bytes()
    entry = {"path": path.relative_to(ROOT).as_posix(), "bytes": len(data),
             "sha256": hashlib.sha256(data).hexdigest()}
    if path.suffix in (".webp", ".png"):
        with Image.open(path) as image:
            rgba = image.convert("RGBA")
            entry.update(width=rgba.width, height=rgba.height,
                         rgba_sha256=hashlib.sha256(rgba.tobytes()).hexdigest())
    return entry


def main():
    original = Image.open(SOURCE / "adaptive-source.png").convert("RGB")
    assert original.width == original.height
    # The imagegen-adapted master includes natural scenery in the adaptive overscan.
    # Resample each density directly from it: no edge extrusion or intermediate resize.
    inset = original.width // 6
    viewport = original.crop((inset, inset, original.width - inset, original.height - inset))
    outputs = []
    for density, scale in DENSITIES.items():
        directory = RES / f"mipmap-{density}"
        directory.mkdir(exist_ok=True)
        adaptive = directory / "ic_launcher_art.webp"
        original.resize((round(108 * scale),) * 2, Image.Resampling.LANCZOS).save(
            adaptive, lossless=True, exact=True, method=6)
        outputs.append(record(adaptive))
        for name, circular in [("ic_launcher", False), ("ic_launcher_round", True)]:
            icon = viewport.convert("RGBA")
            mask = Image.new("L", icon.size)
            draw = ImageDraw.Draw(mask)
            bounds = (0, 0, icon.width - 1, icon.height - 1)
            if circular:
                draw.ellipse(bounds, fill=255)
            else:
                draw.rounded_rectangle(bounds, radius=icon.width / 5, fill=255)
            icon.putalpha(mask)
            output = directory / f"{name}.webp"
            icon.resize((round(48 * scale),) * 2, Image.Resampling.LANCZOS).save(
                output, lossless=True, exact=True, method=6)
            outputs.append(record(output))

    ns = "http://schemas.android.com/apk/res/android"
    ET.register_namespace("android", ns)
    attr = lambda name: f"{{{ns}}}{name}"
    vector = ET.Element("vector", {attr("width"): "108dp", attr("height"): "108dp",
                                   attr("viewportWidth"): "108", attr("viewportHeight"): "108"})
    for path in ET.parse(SOURCE / "monochrome.svg").getroot():
        attributes = {attr("pathData"): path.attrib["d"],
                      attr("fillColor"): "#FFFFFFFF" if path.attrib["fill"] == "white" else "#00000000"}
        if "stroke" in path.attrib:
            attributes.update({attr("strokeColor"): "#FFFFFFFF", attr("strokeWidth"): "4",
                               attr("strokeLineCap"): "round", attr("strokeLineJoin"): "round"})
        ET.SubElement(vector, "path", attributes)
    ET.indent(vector)
    monochrome = RES / "drawable/ic_launcher_monochrome.xml"
    ET.ElementTree(vector).write(monochrome, encoding="utf-8", xml_declaration=True)
    outputs.append(record(monochrome))
    for name in ("ic_launcher", "ic_launcher_round"):
        adaptive = RES / f"mipmap-anydpi-v26/{name}.xml"
        adaptive.write_text('''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Keep the raccoon and garden together in one opaque scene. -->
    <background android:drawable="@mipmap/ic_launcher_art" />
    <foreground android:drawable="@android:color/transparent" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
''')
        outputs.append(record(adaptive))

    manifest_path = DOCS / "manifest.json"
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else {}
    manifest["launcher_icons"] = {
        "requested_on": "2026-09-29",
        "source_method": "built_in_imagegen_composition_adaptation", "source": record(SOURCE / "adaptive-source.png"),
        "reference_sources": [record(SOURCE / "source.png")],
        "user_attachment": "tg_image_4196015632.png",
        "prompt": "docs/design/assets/sources/launcher-raccoon-jar/prompt.md",
        "monochrome_source": record(SOURCE / "monochrome.svg"),
        "conversion": {"pillow_version": pillow_version, "format": "lossless exact WebP",
                       "resampling": "Lanczos", "adaptive_canvas_dp": 108,
                       "padding": "none; natural garden scenery fills the source",
                       "resampling_passes_per_output": 1,
                       "legacy_icon_dp": 48, "legacy_viewport_dp": 72,
                       "layers": "opaque composite background, transparent foreground",
                       "monochrome": "jar and paw coin SVG reused from LCTApp; SVG paths copied to vector XML"},
        "outputs": outputs,
    }
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n")

    # Simulate the nominal Android viewport for visual review, without running the app.
    preview = Image.new("RGB", (760, 390), "#f1f3f5")
    labels = ImageDraw.Draw(preview)
    for x, name, label in [(30, "ic_launcher", "Rounded square"),
                           (410, "ic_launcher_round", "Circle")]:
        icon = Image.open(RES / f"mipmap-xxxhdpi/{name}.webp").convert("RGBA")
        preview.paste(icon, (x, 42), icon)
        small = Image.open(RES / f"mipmap-mdpi/{name}.webp").convert("RGBA")
        preview.paste(small, (x + 230, 110), small)
        labels.text((x, 16), label, fill="#20252a")
        labels.text((x + 230, 170), "48 px", fill="#20252a")
    labels.text((30, 280), "Simulated launcher masks; not a device screenshot", fill="#20252a")
    preview.save(SOURCE / "preview.png")
    print(f"Exported {len(outputs)} launcher resources from {original.size} source.")


if __name__ == "__main__":
    main()
