package opentree;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class Taxonomy {

    private static GraphDatabaseAgent graphDb;
    public final static float DEFAULT_FUZZYMATCH_IDENTITY = (float) 0.70;
    final static String LIFE_NODE_NAME = "life";

    public Taxonomy(GraphDatabaseAgent t) {
        graphDb = t;
    }

    /**
     * A wrapper for simplifying access to node indexes.
     * @author cody
     *
     */
    public static enum NodeIndex implements Index<Node> {
        TAXON_BY_NAME ("taxNodes"), // all taxon nodes
        SYNONYM_BY_NAME ("synNodes"), // all synonym nodes
        PREFERRED_TAXON_BY_NAME ("prefTaxNodes"), // taxon nodes with preferred relationships
        PREFERRED_SYNONYM_BY_NAME ("prefSynNodes"), // synonym nodes attached to preferred taxon nodes
        TAX_SOURCES ("taxSources"), // ?
        TAX_STATUS ("taxStatus"),
        TAX_RANK ("taxRank");
        
        public final Index<Node> index;
        NodeIndex (String indexName) {
            this.index = graphDb.getNodeIndex(indexName);
        }

        @Override
        public IndexHits<Node> get(String arg0, Object arg1) {
            return index.get(arg0,  arg1);
        }

        @Override
        public Class<Node> getEntityType() {
            return index.getEntityType();
        }

        @Override
        public GraphDatabaseService getGraphDatabase() {
            return index.getGraphDatabase();
        }

        @Override
        public String getName() {
            return index.getName();
        }

        @Override
        public boolean isWriteable() {
            return index.isWriteable();
        }

        @Override
        public IndexHits<Node> query(Object arg0) {
            return index.query(arg0);
        }

        @Override
        public IndexHits<Node> query(String arg0, Object arg1) {
            return index.query(arg0, arg1);
        }

        @Override
        public void add(Node node, String key, Object value) {
            index.add(node, key, value);
        }

        @Override
        public void delete() {
            index.delete();
        }

        @Override
        public Node putIfAbsent(Node node, String key, Object value) {
            return index.putIfAbsent(node, key, value);
        }

        @Override
        public void remove(Node node) {
            index.remove(node);
        }

        @Override
        public void remove(Node arg0, String arg1) {
            index.remove(arg0, arg1);
        }

        @Override
        public void remove(Node arg0, String arg1, Object arg2) {
            index.remove(arg0, arg1, arg2);
        }
    }

    protected static enum RelIndex implements Index<Relationship> {
        SOURCE_TYPE ("taxSources");
        
        public final Index<Relationship> index;
        RelIndex (String indexName) {
            this.index = graphDb.getRelIndex(indexName);
        }

        @Override
        public IndexHits<Relationship> get(String arg0, Object arg1) {
            return index.get(arg0, arg1);
        }

        @Override
        public Class<Relationship> getEntityType() {
            return index.getEntityType();
        }

        @Override
        public GraphDatabaseService getGraphDatabase() {
            return index.getGraphDatabase();
        }

        @Override
        public String getName() {
            return index.getName();
        }

        @Override
        public boolean isWriteable() {
            return index.isWriteable();
        }

        @Override
        public IndexHits<Relationship> query(Object arg0) {
            return index.query(arg0);
        }

        @Override
        public IndexHits<Relationship> query(String arg0, Object arg1) {
            return index.query(arg0, arg1);
        }

        @Override
        public void add(Relationship rel, String key, Object value) {
            index.add(rel, key, value);
        }
        
        @Override
        public void delete() {
            index.delete();
        }

        @Override
        public Relationship putIfAbsent(Relationship arg0, String arg1, Object arg2) {
            return index.putIfAbsent(arg0, arg1, arg2);
        }

        @Override
        public void remove(Relationship arg0) {
            index.remove(arg0);
        }

        @Override
        public void remove(Relationship arg0, String arg1) {
            index.remove(arg0, arg1);
        }

        @Override
        public void remove(Relationship arg0, String arg1, Object arg2) {
            index.remove(arg0, arg1, arg2);
        }
    }
    
    public static enum RelTypes implements RelationshipType {
        TAXCHILDOF, // standard rel for tax db, from node to parent
        SYNONYMOF, // relationship for synonyms
        METADATAFOR, // relationship connecting a metadata node to the root of a taxonomy
        PREFTAXCHILDOF// relationship type for preferred relationships
    }
    
    // general taxonomy access methods
    
    public Node getLifeNode() {
        IndexHits<Node> r = NodeIndex.TAXON_BY_NAME.get("name", LIFE_NODE_NAME);
        
        r.close();
        return r.getSingle();
    }
    
    /**
     * Just get the recognized taxon node that is associated with a given synonym node.
     * 
     * @param synonymNode
     * @return taxNode
     */
    public Node getTaxNodeForSynNode(Node synonymNode) {

        TraversalDescription synonymTraversal = Traversal.description()
                .relationships(RelTypes.SYNONYMOF, Direction.OUTGOING);

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
