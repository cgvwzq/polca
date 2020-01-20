import networkx as NX
import sys
import networkx.algorithms.isomorphism as iso

# convert dot files to the graph analysis package's native graph object:
G1 = NX.drawing.nx_pydot.read_dot(sys.argv[1])
G2 = NX.drawing.nx_pydot.read_dot(sys.argv[2])

em = iso.categorical_multiedge_match(['label'], [''])

# returns 'True'/'False'
print(NX.is_isomorphic(G1, G2, edge_match=em))
