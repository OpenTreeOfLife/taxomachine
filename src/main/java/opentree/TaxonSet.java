package opentree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import opentree.Taxonomy.RelTypes;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class TaxonSet implements Iterable<Node> {

    private final LinkedList<Node> taxa;
    private Taxon lica;
    
    public TaxonSet (List<Node> inTaxa) {
        taxa = (LinkedList<Node>)inTaxa;
        lica = null;
    }

    public TaxonSet (Node[] inTaxa) {
        taxa = new LinkedList<Node>();
        for (int i = 0; i < inTaxa.length; i++)
            taxa.add(inTaxa[i]);
        lica = null;
    }
    
    public Taxon getLICA() {
        boolean usePreferredRels = true;
        return getLICA(usePreferredRels);
    }
    
    public Taxon getLICA(boolean usePreferredRels) {
       
        Taxonomy.RelTypes relType;
        if (usePreferredRels)
            relType = RelTypes.PREFTAXCHILDOF;
        else
            relType = RelTypes.TAXCHILDOF;
        
        if (hasLICA())
            return new Taxon(lica.getNode());
        
        Node licaNode;
        if (taxa.size() == 0) {
            throw new java.lang.InternalError("Attempt to find the mrca of zero taxa");

        } else if (taxa.size() == 1) {
            // if there is only one taxon, then it is the mrca
            licaNode = taxa.peek();
        
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
        lica = new Taxon(licaNode);
        return new Taxon(licaNode);
    }
    
    public boolean hasLICA() {
        return lica != null;
    }
    
    public int size() {
        return taxa.size();
    }
    
    @Override
    public Iterator<Node> iterator() {
        return taxa.iterator();
    }
}
