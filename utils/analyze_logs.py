import sys

my_list = []
try:
    with open(sys.argv[1]) as f:
        lines = f.readlines()
        for line in lines:
            if "Found" in line:
                one = line.split("Found inconsistency:")
                # print(one[1].replace(" ",""))
                explanations = one[1].replace(" ","").split("],[")
                for e in explanations:
                    triples = e.replace("[","").replace("]","").replace(" ","").split(',')
                    for t in triples:
                        my_list.append(t.replace("\n",""))
    print("All: ")
    for l in my_list:
        print(l)
    print("\n")
    print("Unique: ")
    setL = set(my_list)
    for l in setL:
        print(l)
    print("\n")
    print("len(Unique): ")
    print(len(setL))


except IndexError:
    raise SystemExit(f"Usage: {sys.argv[0]} <log file name>")