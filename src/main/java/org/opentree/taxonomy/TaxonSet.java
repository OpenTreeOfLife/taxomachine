package org.opentree.taxonomy;

import jade.tree.JadeNode;
import jade.tree.JadeTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.constants.TaxonomyRelType;

public class TaxonSet implements Iterable<Taxon> {

    private final HashSet<Taxon> taxa;
    private final Taxonomy taxonomy;
    private Taxon lica;
    
    // for building subtrees
    private int nodeIndex;
    private HashSet<Long> taxonIds;

    /**
     * Assumes all taxa are coming from the same taxonomy (since we only expect to ever be working with one taxonomy)
     * @param inTaxa
     */
    public TaxonSet (Iterable<Taxon> inTaxa) {
        lica = null;
        taxa = new HashSet<Taxon>();
        for (Taxon t : inTaxa) {
        	taxa.add(t);
        }

        if (taxa.size() > 0)
            taxonomy = taxa.iterator().next().getTaxonomy();
        else
            taxonomy = null;
    }
    
    public TaxonSet (Iterable<Node> inNodes, Taxonomy taxonomy) {
        lica = null;
        this.taxonomy = taxonomy;

        taxa = new HashSet<Taxon>();
        for (Node n : inNodes) {
            taxa.add(taxonomy.getTaxon(n));
        }
    }
    
    public Taxon getLICA() {
        boolean usePreferredRels = true;
        return getLICA(usePreferredRels);
    }
    
    public Taxon getLICA(boolean usePreferredRels) {
       
        TaxonomyRelType relType;
        if (usePreferredRels)
            relType = TaxonomyRelType.PREFTAXCHILDOF;
        else
            relType = TaxonomyRelType.TAXCHILDOF;
        
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
//            HashSet<Taxon> temptaxa = (LinkedList<Node>) taxa.clone();
	    Iterator<Taxon> taxIter = taxa.iterator();
            Node firstNode = taxIter.next().getNode();
            TraversalDescription hierarchy = Traversal.description()
                    .depthFirst()
                    .relationships( relType, Direction.OUTGOING );

            // first get the full path to root from an arbitrary taxon in the set
            ArrayList<Node> path = new ArrayList<Node>();
            for (Node m : hierarchy.traverse(firstNode).nodes())
                path.add(m);

            // compare paths from all other taxa to find the mrca
            int i = 0;
            for (Taxon curTax : taxa) {
                for (Node n : hierarchy.traverse(curTax.getNode()).nodes()) {
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
    
    private JadeNode makeSubtree(Taxon taxNode) {
        
        final double DEF_BRLEN = 1.0;  

        TraversalDescription prefChildTraversal = Traversal.description()
                .breadthFirst()
                .relationships(TaxonomyRelType.PREFTAXCHILDOF, Direction.INCOMING).evaluator(Evaluators.toDepth(1));
        
        // this will hold immediate children of taxNode that contain taxa from this taxon set
        HashSet<Node> heavyChildren = new HashSet<Node>();

        // record all the children of this node which themselves contain taxa from this taxon set
        for (Node childNode : prefChildTraversal.traverse(taxNode.getNode()).nodes()) {
            
            if (childNode.getId() == taxNode.getNode().getId())
                continue;

            // get ids of all eventual descendants of this child node
            HashSet<Long> descendantIds = new HashSet<Long>();

            long[] dTipIds = (long[]) childNode.getProperty("mrca");
            for (int i = 0; i < dTipIds.length; i++) descendantIds.add(dTipIds[i]);

            long[] dInternalIds = (long[]) childNode.getProperty("nested_mrca");
            for (int i = 0; i < dInternalIds.length; i++) descendantIds.add(dInternalIds[i]);

            findDescendants:
            for (long aid : descendantIds) {
                for (long tid : taxonIds) {
                    if (tid == aid) {
                        heavyChildren.add(childNode);
                        break findDescendants;
                    }
                }
            }            
        }

        if (heavyChildren.size() > 1) {

            // this node represents a branching event, i.e. an internal node; make the node and add its children
            JadeNode treeNode = new JadeNode(DEF_BRLEN, taxNode.getName(), null);
            treeNode.assocObject("number", nodeIndex++);

            for (Node heavyChild : heavyChildren) {
                treeNode.addChild(makeSubtree(new Taxon(heavyChild, taxonomy)));
            }

            return treeNode;

        } else if (heavyChildren.size() == 1) {

            // this is a "knuckle" on a lineage containing downstream nodes; continue tracing this lineage
            return makeSubtree(new Taxon(heavyChildren.iterator().next(), taxonomy));

        } else {
            
            // this should be a tip node
            JadeNode tipNode = new JadeNode(DEF_BRLEN, taxNode.getName(), null);
            tipNode.assocObject("number", nodeIndex++);
            return tipNode;
        }
    }
    
    public JadeTree getPrefTaxSubtree() {
                
        // get the lica if there is not one
        if (!hasLICA())
            getLICA();

        // make a set of taxon ids in this taxon set
        taxonIds = new HashSet<Long>();
        for (Taxon t : taxa) {
            System.out.println("adding " + String.valueOf(t.getNode().getId()) + " to taxid hashset");
            taxonIds.add(t.getNode().getId());
        }
        
        nodeIndex = 0;
        return new JadeTree(makeSubtree(lica));

    }
    
    public boolean hasLICA() {
        return lica != null;
    }
    
    public int size() {
        return taxa.size();
    }
    
    public Iterator<Taxon> iterator() {
        return taxa.iterator();
    }
}
