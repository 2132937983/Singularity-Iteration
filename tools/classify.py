import json,glob,os,collections
CUR='/home/claude/work/src/src/main/resources/assets/mio_icif'
ORG='/home/claude/orig/src/main/resources/assets/mio_icif'
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
