#!/usr/bin/env python3
"""
Dump triples in N-Triples for all IRIs listed in an input file, by querying a SPARQL endpoint.
- It reads IRIs (one per line) from --iris.
- It sends batched CONSTRUCT queries with VALUES to the endpoint.
- It writes the union of all results to --out .nt file.

Example:
  python dump_triples_from_iris.py \
    --endpoint https://dbpedia.org/sparql \
    --iris iris.txt \
    --out output.nt \
    --batch-size 50

Requires: requests
  pip install requests
"""
import argparse
import sys
import time
import itertools
import requests
from pathlib import Path

def chunks(iterable, size):
    it = iter(iterable)
    while True:
        batch = list(itertools.islice(it, size))
        if not batch:
            return
        yield batch

def build_construct_query(iris, graph=None, predicate=None):
    """
    Build a SPARQL CONSTRUCT that returns all triples about the given IRIs.
    - If `predicate` is provided, restrict to that predicate (?s predicate ?o).
    - If `graph` is provided, query FROM that named graph.
    """
    values = " ".join(f"<{iri}>" for iri in iris)
    graph_clause = f"FROM <{graph}>\n" if graph else ""
    if predicate:
        triple_pattern = f"?s <{predicate}> ?o .\nFILTER(?s IN ({values}))"
    else:
        triple_pattern = f"VALUES ?s {{ {values} }}\n?s ?p ?o ."
    query = f"""
CONSTRUCT {{
  ?s ?p ?o .
}}
{graph_clause}
WHERE {{
  {triple_pattern}
}}
"""
    return query.strip()

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--endpoint", required=True, help="SPARQL endpoint URL")
    ap.add_argument("--iris", required=True, help="Path to file with IRIs, one per line")
    ap.add_argument("--out", required=True, help="Output N-Triples file")
    ap.add_argument("--batch-size", type=int, default=50, help="Number of IRIs per SPARQL query (default: 50)")
    ap.add_argument("--graph", default=None, help="Optional named graph IRI to query with FROM")
    ap.add_argument("--predicate", default=None, help="Optional predicate IRI to restrict triples (?s predicate ?o)")
    ap.add_argument("--sleep", type=float, default=0.0, help="Seconds to sleep between requests (default: 0)")
    ap.add_argument("--timeout", type=float, default=120.0, help="HTTP timeout seconds per request")
    args = ap.parse_args()

    iris_path = Path(args.iris)
    if not iris_path.exists():
        print(f"IRIs file not found: {iris_path}", file=sys.stderr)
        sys.exit(1)

    all_iris = [line.strip() for line in iris_path.read_text(encoding="utf-8").splitlines() if line.strip()]
    if not all_iris:
        print("No IRIs found in the input file.", file=sys.stderr)
        sys.exit(1)

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    session = requests.Session()
    headers = {
        "Accept": "application/n-triples, text/plain;q=0.9, */*;q=0.1"
    }

    total = 0
    with out_path.open("wb") as out_f:
        for batch in chunks(all_iris, args.batch_size):
            q = build_construct_query(batch, graph=args.graph, predicate=args.predicate)
            data = {"query": q}
            try:
                resp = session.post(args.endpoint, data=data, headers=headers, timeout=args.timeout)
                resp.raise_for_status()
            except Exception as e:
                print("Request failed for batch starting with", batch[0], "->", e, file=sys.stderr)
                # continue to next batch; you can also sys.exit(2) if you prefer hard fail
                continue
            payload = resp.content
            out_f.write(payload)
            if not payload.endswith(b"\n"):
                out_f.write(b"\n")
            batch_triples_guess = payload.count(b"\n")
            total += batch_triples_guess
            if args.sleep > 0:
                time.sleep(args.sleep)

    print(f"Done. Wrote ~{total} lines to {out_path} (note: some lines may be empty or comments depending on endpoint serialization).")

if __name__ == "__main__":
    main()
