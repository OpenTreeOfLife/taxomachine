package opentree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class TaxonSet implements Iterable<Taxon> {

    private final HashSet<Taxon> taxa;
    private final Taxonomy taxonomy;
    private Taxon lica;
    
    /**
     * Assumes all taxa are coming from the same taxonomy (since we only expect to ever be working with one taxonomy)
     * @param inTaxa
     */
    public TaxonSet (Set<Taxon> inTaxa) {
        lica = null;
        taxa = (HashSet<Taxon>) inTaxa;

        if (taxa.size() > 0)
            taxonomy = taxa.iterator().next().getTaxonomy();
        else
            taxonomy = null;
    }
    
    public TaxonSet (LinkedList<Node> inNodes, Taxonomy taxonomy) {
        lica = null;
        this.taxonomy = taxonomy;

        taxa = new HashSet<Taxon>();
        for (Node n : inNodes)
            taxa.add(taxonomy.getTaxon(n));
    }
    
    public Taxon getLICA() {
        boolean usePreferredRels = true;
        return getLICA(usePreferredRels);
    }
    
    public Taxon getLICA(boolean usePreferredRels) {
       
        RelType relType;
        if (usePreferredRels)
            relType = RelType.PREFTAXCHILDOF;
        else
            relType = RelType.TAXCHILDOF;
        
        if (hasLICA())
            return lica;
        
        Node licaNode;
        if (taxa.size() == 0) {
            throw new java.lang.InternalError("Attempt to find the LICA of zero taxa");

        } else if (taxa.size() == 1) {
            // if there is only one taxon, then it is the mrca
            licaNode = taxa.iterator().next().getNode();
        
        } else {
            // find the mrca of all taxa
            LinkedList<Node> temptaxa = (LinkedList<Node>) taxa.clone();
            Node firstNode = temptaxa.poll();
            TraversalDescription hierarchy = Traversal.description()
                    .depthFirst()
                    .relationships( relType, Direction.OUTGOING );

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
            licaNode = path.get(i);
        }

        // keep an internal copy of the mrca, we may need it later
        lica = taxonomy.getTaxon(licaNode);
        return lica;
    }
    
    public boolean hasLICA() {
        return lica != null;
    }
    
    public int size() {
        return taxa.size();
    }
    
    @Override
    public Iterator<Taxon> iterator() {
        return taxa.iterator();
    }
}
