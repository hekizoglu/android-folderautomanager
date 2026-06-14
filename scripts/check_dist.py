import re
from collections import Counter

f = 'app/src/main/java/com/armutlu/apporganizer/domain/usecase/classify/AppClassifier.kt'
content = open(f, encoding='utf-8').read()

pkgs = re.findall(r'"([^"]+)"\s+to\s+Category\.CAT_', content)
seen = {}
dups = []
for p in pkgs:
    if p in seen:
        dups.append(p)
    seen[p] = True
print(f'Toplam: {len(pkgs)}, Benzersiz: {len(seen)}, Duplicate: {len(dups)}')

cats = re.findall(r'"[^"]+"\s+to\s+Category\.(CAT_\w+)', content)
c = Counter(cats)
for cat, count in sorted(c.items(), key=lambda x: -x[1]):
    print(f'  {cat}: {count}')
