#!/usr/bin/env python3
import hashlib,json,os,re,shutil,stat,sys,zipfile
from pathlib import Path,PurePosixPath
from datetime import datetime,timezone
MAX_COMPRESSED=50*1024*1024
MAX_EXPANDED=250*1024*1024
MAX_ENTRIES=5000
MAX_PATH=240
ALLOW={'.md','.txt','.json','.toml','.lock','.rs','.kt','.kts','.java','.xml','.gradle','.properties','.yml','.yaml','.mmd','.sh','.py','.h','.c','.cpp','.swift','.plist','.png','.jpg','.jpeg','.webp','.svg'}
FORBID={'.jks','.keystore','.p12','.pfx','.pem','.key','.mobileprovision','.zip','.7z','.rar','.apk','.aab','.so','.dll','.exe'}
SECRETS=[re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),re.compile(r'gh[pousr]_[A-Za-z0-9]{20,}'),re.compile(r'AKIA[0-9A-Z]{16}')]
def fail(msg):
    print('ZIP REJECTED: '+msg,file=sys.stderr)
    raise SystemExit(2)
def main():
    archive=Path(sys.argv[1]); staging=Path(sys.argv[2]); output=Path(sys.argv[3])
    if not archive.is_file() or archive.stat().st_size>MAX_COMPRESSED: fail('archive missing or exceeds compressed-size limit')
    if not re.fullmatch(r'[a-z0-9][a-z0-9-]{1,62}\.zip',archive.name): fail('invalid archive filename')
    project=archive.stem.split('-phase-')[0]
    if staging.exists(): shutil.rmtree(staging)
    staging.mkdir(parents=True); total=0; seen=set(); entries=[]
    with zipfile.ZipFile(archive) as z:
        infos=z.infolist()
        if not infos or len(infos)>MAX_ENTRIES: fail('entry count limit')
        for info in infos:
            raw=info.filename; norm=raw.replace(chr(92),'/'); path=PurePosixPath(norm)
            mode=(info.external_attr>>16)&0xffff; kind=stat.S_IFMT(mode)
            if chr(0) in raw or path.is_absolute() or re.match(r'^[A-Za-z]:',norm) or '..' in path.parts: fail('unsafe archive path')
            if len(norm.encode())>MAX_PATH or stat.S_ISLNK(mode) or (kind and not (stat.S_ISREG(mode) or stat.S_ISDIR(mode))): fail('path, symlink or special-file violation')
            key=str(path).casefold()
            if key in seen: fail('duplicate normalized path')
            seen.add(key)
            if '.git' in path.parts: continue
            ext=Path(path.name).suffix.lower()
            if ext in FORBID or (ext and ext not in ALLOW): fail('forbidden extension '+ext)
            total+=info.file_size
            if total>MAX_EXPANDED: fail('expanded-size limit')
            entries.append(info)
        roots={PurePosixPath(i.filename.replace(chr(92),'/')).parts[0] for i in entries if PurePosixPath(i.filename.replace(chr(92),'/')).parts}
        root=next(iter(roots)) if len(roots)==1 else None
        for info in entries:
            path=PurePosixPath(info.filename.replace(chr(92),'/'))
            parts=path.parts[1:] if root and len(path.parts)>1 else path.parts
            if not parts: continue
            target=staging.joinpath(*parts); resolved=target.resolve()
            if staging.resolve() not in resolved.parents and resolved!=staging.resolve(): fail('path escapes staging')
            if info.is_dir(): target.mkdir(parents=True,exist_ok=True); continue
            target.parent.mkdir(parents=True,exist_ok=True); data=z.read(info); sample=data.decode('utf-8','ignore')
            if any(pattern.search(sample) for pattern in SECRETS): fail('possible secret material')
            with target.open('xb') as handle: handle.write(data)
    tree=[(p.relative_to(staging).as_posix(),hashlib.sha256(p.read_bytes()).hexdigest()) for p in sorted(staging.rglob('*')) if p.is_file()]
    tree_hash=hashlib.sha256(json.dumps(tree,separators=(',',':')).encode()).hexdigest()
    match=re.search(r'-phase-(\d+)',archive.stem); phase=match.group(1) if match else '00'
    provenance={'source_zip':archive.name,'source_sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'workflow_run_id':os.getenv('GITHUB_RUN_ID','local'),'timestamp_utc':datetime.now(timezone.utc).isoformat(),'phase_tag':phase,'normalized_tree_sha256':tree_hash}
    (staging/'phase-provenance.json').write_text(json.dumps(provenance,indent=2)+'\n')
    with output.open('a') as handle:
        handle.write(f'project_name={project}\nphase_tag={phase}\nsource_zip={archive.name}\ntree_hash={tree_hash}\n')
    print(f'ZIP ACCEPTED: {len(tree)} files; tree {tree_hash}')
if __name__=='__main__': main()
