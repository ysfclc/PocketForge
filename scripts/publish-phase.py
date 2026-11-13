#!/usr/bin/env python3
import os,subprocess,sys,shutil
from pathlib import Path
def run(args,cwd=None,env=None,capture=False):
    return subprocess.run(args,cwd=cwd,env=env,check=True,text=True,capture_output=capture)
def main():
    staging=Path(sys.argv[1]).resolve(); repo=os.environ['TARGET_REPOSITORY']; token=os.environ['GH_TOKEN']
    tree_hash=os.environ['TREE_HASH']; phase=os.getenv('PHASE_TAG','00'); project=os.environ['PROJECT_NAME']
    base=os.getenv('TARGET_DEFAULT_BRANCH','main'); branch=f'keystone/phase-{phase}-{tree_hash[:12]}'
    work=Path('/tmp/pocketforge-phase-publish'); env=os.environ.copy(); env['GIT_TERMINAL_PROMPT']='0'
    if work.exists(): shutil.rmtree(work)
    run(['gh','auth','setup-git','--hostname','github.com'],env=env)
    run(['git','clone','--no-checkout',f'https://github.com/{repo}.git',str(work)],env=env)
    run(['git','fetch','origin',base],work,env)
    existing=run(['git','ls-remote','--heads','origin',f'refs/heads/{branch}'],work,env,True).stdout.strip()
    if existing:
        run(['git','fetch','origin',branch],work,env); run(['git','checkout','-B',branch,f'origin/{branch}'],work,env)
        changed=False
    else:
        run(['git','checkout','-b',branch,f'origin/{base}'],work,env); changed=True
    run(['git','config','user.name','github-actions[bot]'],work)
    run(['git','config','user.email','41898282+github-actions[bot]@users.noreply.github.com'],work)
    if changed:
        for item in staging.iterdir():
            dest=work/item.name
            if item.is_dir(): shutil.copytree(item,dest,dirs_exist_ok=True)
            else: shutil.copy2(item,dest)
        run(['git','add','-A'],work)
        changed=subprocess.run(['git','diff','--cached','--quiet'],cwd=work).returncode!=0
    if changed:
        run(['git','commit','-m',f'Ingest {project} phase {phase}','-m',f'Tree SHA-256: {tree_hash}\nSource ZIP: {os.getenv("SOURCE_ZIP_NAME","unknown")}\nWorkflow run: {os.getenv("GITHUB_RUN_ID","unknown")}'],work)
        run(['git','push','origin',f'HEAD:refs/heads/{branch}'],work,env)
    commit=run(['git','rev-parse','HEAD'],work,capture=True).stdout.strip()
    diff=subprocess.run(['git','diff','--quiet',f'origin/{base}...HEAD'],cwd=work).returncode!=0
    view=subprocess.run(['gh','pr','view',branch,'--repo',repo,'--json','url'],env=env,text=True,capture_output=True)
    if diff and view.returncode:
        run(['gh','pr','create','--repo',repo,'--head',branch,'--base',base,'--title',f'Ingest phase {phase}: {project}','--body',f'Keystone ingestion. Tree hash: {tree_hash}. Review before merge.'],work,env)
    with open(os.environ['GITHUB_OUTPUT'],'a') as handle: handle.write(f'target_commit={commit}\n')
    print(f'{repo}@{commit}; changed={changed}; diff={diff}')
if __name__=='__main__': main()
