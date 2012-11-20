package opentree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opentree.Taxonomy.NodeIndex;
import opentree.Taxonomy.RelTypes;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.Traversal;

public final class BarrierNodes {

    private static final int LARGE = 100000000;
    private static Taxonomy taxonomy;
    
    /**
     * Key is node name and value is governing nomenclature.
     */
    private static final HashMap<String,String> barrierNamesMap = new HashMap<String,String>() {
        private static final long serialVersionUID = 1L;
        {
            put("Fungi","ICBN");
            put("Viridiplantae","ICBN");
            put("Bacteria","ICNB");
            put("Metazoa","ICZN");
            put("Alveolata","ICZN");
            put("Rhodophyta","ICBN");
            put("Glaucocystophyceae","ICBN");
            put("Haptophyceae","ICBN");
            put("Choanoflagellida","ICZN");
            //maybe add Protostomia, see Heterochaeta in ncbi
        }
    };

    public BarrierNodes(Taxonomy t) {
        taxonomy = t;
    }
    
    /**
     * Finds the set of nodes matching the barrier names. Because names may be homonyms, we have to use the ones that are closest to the root.
     * 
     * @return barrierNodes
     */
    public ArrayList<Node> getBarrierNodes() {

        Node lifen = taxonomy.getLifeNode();

        // traverse from each barrier node to life and pick the closest one
        PathFinder<Path> tfinder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.TAXCHILDOF, Direction.OUTGOING), 10000);
        ArrayList<Node> barnodes = new ArrayList<Node>();

        for (String itns : barrierNamesMap.keySet()) {
            IndexHits<Node> hits = NodeIndex.TAXON_BY_NAME.get("name", itns);
            int bestcount = LARGE;
            Node bestitem = null;
            try {
                for (Node node : hits) {
                    Path tpath = tfinder.findSinglePath(node, lifen);
                    int pl = tpath.length();
                    if (pl < bestcount) {
                        bestcount = pl;
                        bestitem = node;
                    }
                }
            } finally {
                hits.close();
            }
            System.out.println("Found barrier: " + itns + " " + bestitem.getId());
            barnodes.add(bestitem);
        }
        
        return barnodes;
    }

    /**
     * Just returns the set of barrier node names.
     * @return barrierNames
     */
    public Set<String> getBarrierNodeNames() {
        Set<String> bn = barrierNamesMap.keySet();
        HashSet<String> barrierNames = new HashSet<String>();
        for (String name : bn) {
            barrierNames.add(name);
        }
        return barrierNames;
    }

    /**
     * Returns the mapping of barrier node names (keys) to their governing nomenclature (values).
     * @return barrierNodeMap
     */
    public Map<String,String> getBarrierNodeMap() {
        return barrierNamesMap;
    }
    
    /**
     * Checks whether the passed argument `name` is a perfect match to a barrier node name.
     * @return isMatch
     */
    public boolean containsName(String name) {
        return barrierNamesMap.keySet().contains(name);
    }
}
