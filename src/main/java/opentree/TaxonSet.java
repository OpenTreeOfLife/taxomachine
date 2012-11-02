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

    private final LinkedList<Node> taxa;
    private Taxon mrca;
    
    public TaxonSet (List<Node> inTaxa) {
        taxa = (LinkedList<Node>)inTaxa;
        path = new ArrayList<Node>();
        hierarchy = Traversal.description()
                .depthFirst()
                .relationships( RelTypes.TAXCHILDOF, Direction.OUTGOING );
        mrca = null;
    }
    
    public Node getMRCA() {
        
    public Taxon getMRCA() {
       
        if (hasMRCA()) {
            return new Taxon(mrca.getNode());
        }
        
        Node mrcaNode;
        if (taxa.size() == 0) {
            throw new java.lang.InternalError("Attempt to find the mrca of zero taxa");

        } else if (taxa.size() == 1) {
            // if there is only one taxon, then it is the mrca
            mrcaNode = taxa.peek();
        
        } else {
            // find the mrca of all taxa
            LinkedList<Node> temptaxa = (LinkedList<Node>) taxa.clone();
            Node firstNode = temptaxa.poll();
            TraversalDescription hierarchy = Traversal.description()
                    .depthFirst()
                    .relationships( RelTypes.TAXCHILDOF, Direction.OUTGOING );

            // first get the full path to root from an arbitrary taxon in the set
            ArrayList<Node> path = new ArrayList<Node>();
            for (Node m : hierarchy.traverse(firstNode).nodes())
                path.add(m);

            // compare paths from all other taxa to find the mrca
            int i = 0;
            for (Node curTax : temptaxa) {
                for (Node n : hierarchy.traverse(curTax).nodes()) {
                    if (path.contains(n)) {
                        int j = path.indexOf(n);
                        if (i < j)
                            i = j;
                        break;
                    }
                }
            }
            mrcaNode = path.get(i);
        }

        // keep an internal copy of the mrca, we may need it later
        mrca = new Taxon(mrcaNode);
        return new Taxon(mrcaNode);
    }

    public int getDistToMRCA(Node n) {

        if (hasMRCA() == false)
            getMRCA();
        
        int d = 0;
        // TODO: calculate the distance of n to the mrca
        // positive distance is outgoing only (for taxa that are contained by the ingroup),
        // negative distance is total incoming/outgoing; used if the taxon is not within the ingroup

        return d;
    }
    
    public boolean hasMRCA() {
        return mrca != null;
    }
    
    public int size() {
        return taxa.size();
    }
    
    @Override
    public Iterator<Node> iterator() {
        return taxa.iterator();
    }
}
