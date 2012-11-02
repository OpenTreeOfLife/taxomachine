package opentree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import opentree.TaxonomyBase.RelTypes;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class TaxonSet implements Iterable<Node> {

    ArrayList<Node> path;
    LinkedList<Node> taxa;
    TraversalDescription hierarchy;
    Node mrca;
    
    public TaxonSet (List<Node> inTaxa) {
        taxa = (LinkedList<Node>)inTaxa;
        path = new ArrayList<Node>();
        hierarchy = Traversal.description()
                .depthFirst()
                .relationships( RelTypes.TAXCHILDOF, Direction.OUTGOING );
        mrca = null;
    }
    
    public Node getMRCA() {
        
        int i = 0;
        if (taxa.size() < 2) {
            throw new java.lang.IllegalArgumentException("Attempt to find the MRCA of fewer than two taxa");
        }
        
        LinkedList<Node> temptaxa = (LinkedList<Node>) taxa.clone();
        Node firstNode = temptaxa.poll();

//        System.out.println("Start node is " + firstNode.getProperty("name")); //////////////// debug
//        System.out.println("Start path contains"); ///////////////// debug

        // get the start path
        for (Node m : hierarchy.traverse(firstNode).nodes()) {
//            System.out.println(m.getProperty("name")); /////////////////// debug
            path.add(m);
        }

        for (Node curTax : temptaxa) {
            for (Node n : hierarchy.traverse(curTax).nodes()) {
                if (path.contains(n)) {
                    int j = path.indexOf(n);
                    if (i < j)
                        i = j;
//                    System.out.println(n.getProperty("name")); /////////////////// debug
                    break;
                }
            }
        }
        mrca = path.get(i);
        return mrca;
    }

    public int getDistToMRCA(Node n) {

        if (hasMRCA() == false)
            getMRCA();
        
        int d = 0;
        // calculate the distance of n to the mrca
        // positive distance is outgoing only (for taxa that are contained by the ingroup),
        // negative distance is total incoming/outgoing; used if the taxon is not within the ingroup

        return d;
    }
    
    public boolean hasMRCA() {
        return mrca != null;
    }
    
    public Iterator<Node> iterator() {
        return taxa.iterator();
    }
}
