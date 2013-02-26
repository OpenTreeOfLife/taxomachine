package opentree;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

/**
 * This class provides access to methods specific to the taxonomic database itself. Initialization requires a GraphDatabaseAgent object,
 * through which the Taxonomy object will interact with the underlying Neo4J database object.
 * 
 * Most of the interaction with the taxonomy is defined in other class files that make use of or inherit the Taxonomy class
 * (e.g. TaxonomyComparator, TaxonomySynthesizer, TaxonomyLoader, Taxon, etc.). Only the most general methods are defined here.
 * The most frequent use of the Taxonomy class itself should probably be to access objects of of the TaxonomyContext class which
 * are iun turn used to access ALL the node indexes. For more information on node indexes and TaxonomyContext objects, refer
 * to the documentation in the TaxonomyContext class file.
 * 
 * @author cody hinchliff and stephen smith
 *
 */
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
     * Return a TaxonomyContext object for this taxonomy, as defined by the passed ContextDescription `contextDesc`.
     * TaxonomyContext objects are used for accessing ALL node indexes. For more information on how to use these objects,
     * refer to the documentation in the TaxonomyContext class file.
     * @param contextDesc
     * @return TaxonomyContext
     */
    public TaxonomyContext getContext(ContextDescription contextDesc) {
        return new TaxonomyContext(contextDesc, this);
    }
    
    /**
     * Returns a TaxonomyContext object for the ContextDescription with name matching `name`
     * TaxonomyContext objects are used for accessing ALL node indexes. For more information on how to use these objects,
     * refer to the documentation in the TaxonomyContext class file.
     * @param name
     * @return TaxonomyContext
     */
    public TaxonomyContext getContextByName(String name) {

        for (ContextDescription cd : ContextDescription.values()) {
            if (cd.name.equals(name)) {
                return this.getContext(cd);
            }
        }

        // if we didn't find one
        // this might ought to throw an exception instead...
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
    
    /** Just a wrapper for the underlying database method defined in the GraphDatabaseAgent class
     * 
     * @return Transaction
     */
    public Node createNode() {
        return graphDb.createNode();
    }
    
    /** Just a wrapper for the underlying database method defined in the GraphDatabaseAgent class
     * 
     * @return Transaction
     */
    public Transaction beginTx() {
        return graphDb.beginTx();
    }
    
    /** Just a wrapper for the underlying database method defined in the GraphDatabaseAgent class
     *  
     * @return Node
     */
    public Node getNodeById(Long arg0) {
        return graphDb.getNodeById(arg0);
    }
}
