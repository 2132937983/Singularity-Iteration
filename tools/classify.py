import argparse,json,glob,os,collections
from pathlib import Path
CUR = str(Path(__file__).resolve().parents[1] / 'src/main/resources/assets/mio_icif')
parser = argparse.ArgumentParser(description='Classify current block models against original assets.')
parser.add_argument('--baseline', required=True, type=Path, metavar='ASSETS_DIR',
                    help='Original assets/mio_icif directory containing models and textures.')
ORG = parser.parse_args().baseline.expanduser().resolve()
def load(p): return json.load(open(p,encoding='utf-8-sig'))
restorable=[];new=[];texmiss=[]
for f in glob.glob(CUR+'/models/block/**/*.json',recursive=True):
    try: d=load(f)
    except Exception as e: print('bad',f,e); continue
    if len(d.get('elements',[]))<=1: continue
    rel=os.path.relpath(f,CUR)
    o=os.path.join(ORG,rel)
    if not os.path.exists(o): new.append(rel); continue
    od=load(o)
    for t in od.get('textures',{}).values():
        if t.startswith('#'): continue
        ns,p=t.split(':') if ':' in t else ('minecraft',t)
        if ns=='mio_icif' and not os.path.exists(os.path.join(CUR,'textures',p+'.png')): texmiss.append((rel,t))
    restorable.append((rel,len(od.get('elements',[])),od.get('parent')))
if __name__=='__main__':
    print('restorable',len(restorable),'new',len(new),'texmiss',len(texmiss))
    print(collections.Counter((p,n) for _,n,p in restorable).most_common(10))
    print(new); print(texmiss[:10])
