package opentree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import opentree.TaxonomyBase.RelTypes;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;

public final class BarrierNodes extends TaxonomyBase {

    static final HashSet<String> barrierNames = new HashSet<String>() {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        {
            add("Fungi");
            add("Bacteria");
            add("Viridiplantae");
            add("Metazoa");
        }
    };
    static final int LARGE = 100000000;

    public BarrierNodes(EmbeddedGraphDatabase gdb) {
        graphDb = gdb;
        taxNodeIndex = graphDb.index().forNodes("taxNodes");
    }

    public ArrayList<Node> getBarrierNodes() {

        // some of the barrier names may be homonyms, so we need to choose the ones that are closest to the root
        // first get the life node
        IndexHits<Node> hitsl = taxNodeIndex.get("name", "life");
        if (hitsl.size() != 1) {
            System.out.println("There is a problem with getting the life node");
            System.exit(0);
        }

        Node lifen = null;
        try {
            lifen = hitsl.getSingle();
        } finally {
            hitsl.close();
        }

        // now traverse from each barrier node to life and pick the closest one
        PathFinder<Path> tfinder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.TAXCHILDOF, Direction.OUTGOING), 10000);
        ArrayList<Node> barnodes = new ArrayList<Node>();

        for (String itns : barrierNames) {
            IndexHits<Node> hits = taxNodeIndex.get("name", itns);
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

    public HashSet<String> getBarrierNodeNames() {
        return barrierNames;
    }

    public boolean contains(String name) {
        return barrierNames.contains(name);
    }
}
