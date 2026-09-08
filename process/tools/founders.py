"""Set every imported gene's founder table from its rarity tier.

    python process/tools/founders.py [--check]

WHY THIS EXISTS. Eighty-five magical genes at "a few percent each" is not a few
percent. Twenty of them took the share of wild horses showing some magical gene
from about a half to 0.86, because the shares compound - and the test that
caught it (ShowcaseGenotypesTest) is asserting a design decision, not an
arbitrary band: a wild-caught horse is meant to be an ordinary horse.

So the founder rate is not a per-gene judgement call. It is derived here, from
the tier the gene declares, and the whole set is budgeted together:

    COMMON 0.12%   UNCOMMON 0.08%   RARE 0.04%   EPIC 0.02%   LEGENDARY 0.01%

Eighty-five genes averaging around 0.06% put roughly one wild horse in twenty
carrying any of them at all - which is what "magical" ought to mean, and which
leaves the breed files free to set whatever pool a magical BREED wants. A breed
names its own gene pools; it does not inherit the wild rate.

DOMINANT vs RECESSIVE. Where the heterozygote expresses, the budget goes almost
all on it (the homozygote is then the square, and rare). Where only the
homozygote expresses, the budget goes entirely on the homozygote and the
carrier gets ZERO - deliberately, and against the Hardy-Weinberg shape. A
population of invisible carriers is a population where nothing is ever seen and
the gene may as well not exist; if a recessive magical gene is in the world at
all, some wild horse should be showing it.
"""
import glob
import io
import json
import os
import sys

GENES = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(
    os.path.abspath(__file__)))), 'common', 'src', 'main', 'resources',
    'horsegenetics', 'genes')

# Share of founder horses that should EXPRESS the gene, as a percentage.
BUDGET = {
    'COMMON': 0.12,
    'UNCOMMON': 0.08,
    'RARE': 0.04,
    'EPIC': 0.02,
    'LEGENDARY': 0.01,
    'MYTHIC': 0.005,
}

# The two genes that predate this import and carry hand-set tables.
SKIP = {'suntouched.json', 'waterborn.json', 'index.json'}


def combinations(tokens):
    out = []
    for i, a in enumerate(tokens):
        for b in tokens[i:]:
            out.append(a + '/' + b)
    return out


def claimed(spec, expression, combos):
    """Which combinations this expression takes - mirrors GeneSpecParser."""
    when = expression.get('when')
    if when is None:
        return None                       # the catch-all
    if isinstance(when, list):
        out = []
        for c in when:
            if c in combos:
                out.append(c)
            else:
                a, b = c.split('/')
                out.append(b + '/' + a)
        return out
    out = []
    for c in combos:
        t = c.split('/')
        if all(t.count(tok) == int(n) for tok, n in when.items()):
            out.append(c)
    return out


def expressing(spec):
    """The combinations that show something, and the ones that do not."""
    tokens = [a['token'] for a in spec['alleles']]
    combos = combinations(tokens)
    shows, hides, fallback = [], [], None
    taken = set()
    for e in spec['expressions']:
        owned = claimed(spec, e, combos)
        if owned is None:
            fallback = e
            continue
        taken.update(owned)
        (hides if e.get('wildType') else shows).extend(owned)
    if fallback is not None:
        rest = [c for c in combos if c not in taken]
        (hides if fallback.get('wildType') else shows).extend(rest)
    return combos, shows, hides


def main(check_only):
    total = 0.0
    rows = []
    for path in sorted(glob.glob(os.path.join(GENES, '*.json'))):
        name = os.path.basename(path)
        if name in SKIP:
            continue
        spec = json.load(io.open(path, encoding='utf-8'))
        tokens = [a['token'] for a in spec['alleles']]
        combos, shows, hides = expressing(spec)
        budget = BUDGET.get(spec.get('rarity', 'UNCOMMON'), 0.08)

        variant, baseline = tokens[0], tokens[-1]
        het = variant + '/' + baseline
        hom = variant + '/' + variant
        founders = {c: 0.0 for c in combos}

        if het in shows:
            # Dominant: nearly all the budget on the heterozygote, a twentieth
            # on the homozygote so the pair is reachable without breeding for it.
            founders[het] = budget * 0.95
            founders[hom] = budget * 0.05
        else:
            # Recessive: all of it on the homozygote, none on the carrier.
            founders[hom] = budget
        # Any other expressing combination (a third allele's own outcome) takes
        # a share of the same budget rather than adding to it.
        extra = [c for c in shows if c not in (het, hom)]
        for c in extra:
            founders[c] = budget * 0.4 / len(extra)

        spent = sum(founders.values())
        founders[baseline + '/' + baseline] = round(100.0 - spent, 6)
        spec['founders'] = {c: round(v, 6) for c, v in founders.items() if v > 0}

        total += spent
        rows.append((name, spec.get('rarity', 'UNCOMMON'), round(spent, 4)))
        if not check_only:
            io.open(path, 'w', encoding='utf-8').write(json.dumps(spec, indent=2) + '\n')

    for name, rarity, spent in rows:
        print('  %-24s %-10s %.3f%%' % (name, rarity, spent))
    print('%d genes, %.2f%% of founders express one of them in total' % (len(rows), total))
    print('about %.1f%% of wild horses carry any of them' % (100 * (1 - (1 - total / 100) ** 1),))


if __name__ == '__main__':
    main('--check' in sys.argv)
