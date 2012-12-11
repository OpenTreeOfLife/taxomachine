package opentree;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class Taxonomy {

    GraphDatabaseAgent graphDb;
    public final static float DEFAULT_FUZZYMATCH_IDENTITY = (float) 0.70;
    final static String LIFE_NODE_NAME = "life";
    public final TaxonomyContext ALLTAXA = new TaxonomyContext(ContextDescription.ALLTAXA, this);

    public Taxonomy(GraphDatabaseAgent gdb) {
        graphDb = gdb;
    }
   
    public Node getLifeNode() {
        
        Node lifeNode;
        try {
            lifeNode = ALLTAXA.findTaxNodesByName(LIFE_NODE_NAME).get(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Could not get life node");
            lifeNode = null;
        }

        return lifeNode;
    }
    
    /**
     * Return a TaxonomyContext object for this taxonomy, as defined by the passed ContextDescription `cd`
     * @param contextDesc
     * @return
     */
    public TaxonomyContext getContext(ContextDescription contextDesc) {
        return new TaxonomyContext(contextDesc, this);
    }
    
    /**
     * Returns a TaxonomyContext object for the ContextDescription indicated by `id`
     * @param id
     * @return
     */
    public TaxonomyContext getContextByName(String name) {

        for (ContextDescription cd : ContextDescription.values()) {
//            System.out.println("comparing " + name + " to " + cd.name);
            if (cd.name.equals(name)) {
//                System.out.println("success");
                return this.getContext(cd);
            }
        }

        // if we didn't find one
        return null;
    }

    public Taxon getTaxon(Node node) {
        return new Taxon(node, this);
    }
    
    /**
     * Just get the recognized taxon node that is associated with a given synonym node. Deprecated since the synonym nodes are now only accessible via
     * traversals that must pass through the associated taxon nodes anyway, but left in case it is becomes useful.
     * 
     * @param synonymNode
     * @return taxNode
     */
    @Deprecated
    public Node getTaxNodeForSynNode(Node synonymNode) {

        TraversalDescription synonymTraversal = Traversal.description()
                .relationships(RelType.SYNONYMOF, Direction.OUTGOING);

        Node taxNode = null;
        for (Node tn : synonymTraversal.traverse(synonymNode).nodes())
            taxNode = tn;

        return taxNode;
    }
    
    /**
     * Finds the directed internodal distance between `n1` and `n2` along relationships of type `relType` by tracing the paths to the LICA
     * of n1 and n2. Note that this method uses direction to find the LICA, and will not behave properly if the outgoing paths along the
     * specified relType from n1 and n2 do not intersect.
     * @param n1
     * @param n2
     * @param relType
     * @return distance
     */
    public int getInternodalDistThroughMRCA(Node n1, Node n2, RelationshipType relType) {

        System.out.println("Node 1: " + n1.getProperty("name") + " " + n1.getId() + ", Node 2: " + n2.getProperty("name") + " " + n2.getId());

        TraversalDescription hierarchy = Traversal.description()
                .depthFirst()
                .relationships(relType, Direction.OUTGOING);

        Iterable<Node> firstPath = hierarchy.traverse(n1).nodes();
        Iterable<Node> secondPath = hierarchy.traverse(n2).nodes();

        int i = 0;
        boolean matched = false;
        count: for (Node n : firstPath) {

            int j = 0;
            for (Node m : secondPath) {
                if (n.getId() == m.getId())
                    matched = true;
                j++;
            }

            if (matched) {
                i += j;
                break count;
            }

            i++;
        }

        return i;
    }
    
    // wrappers for relevant underlying database methods
    
    public Node createNode() {
        return graphDb.createNode();
    }
    
    public Transaction beginTx() {
        return graphDb.beginTx();
    }
    
    public Node getNodeById(Long arg0) {
        return graphDb.getNodeById(arg0);
    }
}
