"""Land a batch of finished gene files.

    python process/tools/land.py <gene-name> [<gene-name> ...]

For each gene named on the command line it:
  1. checks common/.../horsegenetics/genes/<slug>.json exists and is valid JSON,
  2. adds it to that folder's index.json, sorted, if it is not there already,
  3. flips its row in process/index.csv from "no" to "yes",
  4. deletes the gene's section out of its source markdown, and deletes the
     markdown outright once nothing but whitespace is left.

Step 4 is the one worth explaining: the source files are the to-do list, and a
run this long needs a to-do list that shrinks. What is still in the markdown is
what is still to do, regardless of what any summary claims.

Nothing here is clever. It exists so that "landing a gene" is one atomic thing
rather than four places to forget.
"""
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
GENES = os.path.join(ROOT, 'common', 'src', 'main', 'resources', 'horsegenetics', 'genes')
INDEX = os.path.join(GENES, 'index.json')
CSV = os.path.join(ROOT, 'process', 'index.csv')
PROCESS = os.path.join(ROOT, 'process')


def slug(name):
    return re.sub(r'[^a-z0-9]+', '_', name.lower()).strip('_')


def read_csv():
    with io.open(CSV, encoding='utf-8') as f:
        return [line.rstrip('\n') for line in f]


def strip_section(md_path, heading):
    """Remove the '# <heading>' section, up to the next top-level heading."""
    if not os.path.exists(md_path):
        return False
    with io.open(md_path, encoding='utf-8') as f:
        lines = f.readlines()
    out, dropping, dropped = [], False, False
    for line in lines:
        if line.startswith('# '):
            title = line[2:].strip()
            # The sources head a gene several ways: "Rex Marking", "Glow
            # Mutation", "Pavonem (Pavo) Gene", "G-01 - STARS". Match on the
            # name appearing in the heading rather than on the whole heading.
            dropping = heading.lower() in title.lower()
            if dropping:
                dropped = True
                continue
        if line.startswith('### G-') and heading.lower() in line.lower():
            dropping = True
            dropped = True
            continue
        if dropping and line.startswith('### G-'):
            dropping = False
        if not dropping:
            out.append(line)
    if dropped:
        with io.open(md_path, 'w', encoding='utf-8') as f:
            f.writelines(out)
    return dropped


def main(names):
    with io.open(INDEX, encoding='utf-8') as f:
        index = json.load(f)
    rows = read_csv()
    header, body = rows[0], rows[1:]

    for name in names:
        file = slug(name) + '.json'
        path = os.path.join(GENES, file)
        if not os.path.exists(path):
            print('MISSING  ' + file)
            continue
        with io.open(path, encoding='utf-8') as f:
            json.load(f)                     # a syntax error should stop here, loudly
        if file not in index:
            index.append(file)

        hit = False
        for i, row in enumerate(body):
            cells = row.split(',')
            if cells[0].strip().lower() == name.lower():
                cells[-1] = 'yes'
                body[i] = ','.join(cells)
                hit = True
                break
        if not hit:
            print('NO CSV ROW  ' + name)

        cut = False
        for md in sorted(os.listdir(PROCESS)):
            if md.endswith('.md') and strip_section(os.path.join(PROCESS, md), name):
                cut = True
                break
        if not cut:
            # Some sources are one gene with no markdown heading at all -
            # anglerinfo.md opens with the word ANGLER and goes straight in.
            # Landing the gene empties the whole file.
            for md in sorted(os.listdir(PROCESS)):
                p2 = os.path.join(PROCESS, md)
                if not md.endswith('.md') or slug(name) not in slug(md):
                    continue
                if '\n# ' not in io.open(p2, encoding='utf-8').read():
                    io.open(p2, 'w', encoding='utf-8').write('')
                    cut = True
                    break
        if not cut:
            print('NOT CUT FROM ANY SOURCE  ' + name)
        print('landed   ' + file)

    with io.open(INDEX, 'w', encoding='utf-8') as f:
        f.write(json.dumps(sorted(index), indent=2) + '\n')
    with io.open(CSV, 'w', encoding='utf-8') as f:
        f.write('\n'.join([header] + body) + '\n')

    # A source file with nothing but whitespace left has no work left in it.
    for md in sorted(os.listdir(PROCESS)):
        p = os.path.join(PROCESS, md)
        if md.endswith('.md') and not io.open(p, encoding='utf-8').read().strip():
            os.remove(p)
            print('removed  process/' + md + ' - nothing left in it')

    done = sum(1 for r in body if r.strip().endswith('yes'))
    print('%d of %d genes landed' % (done, len(body)))


if __name__ == '__main__':
    main(sys.argv[1:])
