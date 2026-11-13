#!/usr/bin/env python3
from pathlib import Path
required=['README.md','AGENTS.md','PRINCIPLES.md','SECURITY.md','CONTRIBUTING.md','CODEOWNERS','docs/product/vision.md','docs/product/personas.md','docs/product/scope.md','docs/product/roadmap.md','docs/architecture/overview.md','docs/architecture/data-model.md','docs/architecture/ffi-contract.md','core/Cargo.toml','android/app/build.gradle.kts','ios/README.md','.github/workflows/pipeline.yml']
missing=[p for p in required if not Path(p).is_file()]
assert not missing,missing
assert len(list(Path('.agent/agents').glob('*.md')))==7
assert len(list(Path('.agent/skills').glob('*.md')))==10
assert not list(Path('.').rglob('*.html'))
print('PocketForge foundation inventory passed.')
