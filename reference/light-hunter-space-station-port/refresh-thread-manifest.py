import hashlib
import json
from pathlib import Path

reference = Path(__file__).resolve().parent
root = reference.parents[1]
manifest = json.loads((reference / "THREAD_MODIFIER_MANIFEST.json").read_text(encoding="utf-8"))
source = root / manifest["adaptedSource"]["destination"]
manifest["adaptedSource"]["sha256"] = hashlib.sha256(source.read_bytes()).hexdigest()
data = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
(reference / "THREAD_MODIFIER_MANIFEST.json").write_text(data, encoding="utf-8")
(root / "src/main/resources/META-INF/light-hunter-space-station-port/THREAD_MODIFIER_MANIFEST.json").write_text(
    data, encoding="utf-8")
