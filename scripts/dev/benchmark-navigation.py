"""Measure repeatable local retrieval work, excluding model reasoning and tool transport."""

import json
import statistics
import subprocess
import time
from pathlib import Path

root = Path(__file__).resolve().parents[2]
target = root / 'src/main/java/com/xyp/gtnotgood/loader/LateMixinsLoader.java'


def measure(scoped):
    start = time.perf_counter()
    search = ['rg', '-n', 'getMixins', str(target) if scoped else 'src/main/java']
    result = subprocess.run(search, cwd=root, capture_output=True, check=True)
    lines = target.read_text(encoding='utf-8').splitlines()
    context = '\n'.join(lines[60:105] if scoped else lines)
    assert 'public List<String> getMixins(Set<String> loadedMods)' in context
    assert 'loadedMods.contains(ModList.Railcraft.getID())' in context
    return (time.perf_counter() - start) * 1000, len(result.stdout) + len(context.encode())


data = {'scope': 'Locate late Mixin registration and read Railcraft guard; same verified answer',
        'limitations': 'Warm local subprocess/retrieval only; not LLM end-to-end or Serena comparison'}
samples = {False: [], True: []}
for _ in range(5):
    for scoped in (False, True):
        samples[scoped].append(measure(scoped))
for scoped, name in ((False, 'broad_search_full_file'), (True, 'skill_guided_scoped_read')):
    data[name] = {'milliseconds': [round(x[0], 3) for x in samples[scoped]],
                  'median_ms': round(statistics.median(x[0] for x in samples[scoped]), 3),
                  'retrieved_bytes': samples[scoped][0][1]}
output = root / '.gradle/codex-navigation-benchmark.json'
output.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
print(output.read_text(encoding='utf-8'))
