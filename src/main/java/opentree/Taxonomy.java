package opentree;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public /*abstract */ class Taxonomy {

/*    private static EmbeddedGraphDatabase embeddedGraphDb;
    private static GraphDatabaseService graphDbService;
    private static boolean usingService; */

    private static GraphDatabaseAgent graphDb;
    public final static float DEFAULT_FUZZYMATCH_IDENTITY = (float) 0.70;
    final static String LIFE_NODE_NAME = "life";

    public Taxonomy(GraphDatabaseAgent t) {
        graphDb = t;
    }
    /*
    public TaxonomyBase(String graphname) {
        embeddedGraphDb = new EmbeddedGraphDatabase( graphname );
        usingService = false;
    }

    public TaxonomyBase(EmbeddedGraphDatabase graphdb ) {
        embeddedGraphDb = graphdb;
        usingService = false;
    }
    
    public TaxonomyBase(GraphDatabaseService graphdb ) {
        graphDbService = graphdb;
        usingService = true;
    } */

//    protected static Index<Relationship> sourceRelIndex;

/*    protected static Index<Node> taxNodeIndex;
    protected static Index<Node> prefTaxNodeIndex;
    protected static Index<Node> prefSynNodeIndex;
    protected static Index<Node> synNodeIndex;
    protected static Index<Node> taxSourceIndex; */

    /**
     * A wrapper for simplifying access to node indexes.
     * @author cody
     *
     */
    public static enum NodeIndex implements Index<Node> {
        TAXON_BY_NAME ("taxNodes"),
        SYNONYM_BY_NAME ("synNodes"),
        PREFERRED_TAXON_BY_NAME ("prefTaxNodes"),
        PREFERRED_SYNONYM_BY_NAME ("prefSynNodes"),
        TAX_SOURCES ("taxSources");
        
        public final Index<Node> index;
        NodeIndex (String indexName) {
            this.index = graphDb.getNodeIndex(indexName);
            //            if (usingService) {
//                this.index = graphDbService.index().forNodes(indexName);
//            } else {
//                this.index = embeddedGraphDb.index().forNodes(indexName);
//            }
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
        public void add(Node arg0, String arg1, Object arg2) {
            index.add(arg0, arg1, arg2);
        }

        @Override
        public void delete() {
            index.delete();
        }

        @Override
        public Node putIfAbsent(Node arg0, String arg1, Object arg2) {
            return index.putIfAbsent(arg0, arg1, arg2);
        }

        @Override
        public void remove(Node arg0) {
            index.remove(arg0);
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

/*    protected static enum RelIndex {
        SOURCE_TYPE ("taxSources");
        
        public final Index<Relationship> index;
        RelIndex (String indexName) {
            this.index = graphDb.index().forRelationships(indexName);;
        }
        
        public void add (Relationship r, String property, String key) {
            index.add(r, property, key);
        }
    } */
    
    protected static enum RelTypes implements RelationshipType {
        TAXCHILDOF, // standard rel for tax db, from node to parent
        SYNONYMOF, // relationship for synonyms
        METADATAFOR, // relationship connecting a metadata node to the root of a taxonomy
        PREFTAXCHILDOF// relationship type for preferred relationships
    }
        
    public Node getLifeNode() {
        IndexHits<Node> r = NodeIndex.TAXON_BY_NAME.get("name", LIFE_NODE_NAME);
        r.close();
        return r.getSingle();
    }
    
/*    public EmbeddedGraphDatabase getGraphDb() {
        if (usingService == false)
            return embeddedGraphDb;
        else
            return null;
    }
    
    public GraphDatabaseService getGraphDbService() {
        if (usingService)
            return graphDbService;
        else
            return null;
    } 
    
    public boolean isUsingService() {
        return usingService;
    } */
    
    // wrappers for relevant underlying database methods
    
    public Node createNode() {
        return graphDb.createNode();
        /*        if (usingService)
            return taxonomy.createNode();
        else
            return embeddedGraphDb.createNode();        */
    }
    
    public Transaction beginTx() {
        return graphDb.beginTx();
        /*        if (usingService)
            return graphDbService.beginTx();
        else
            return embeddedGraphDb.beginTx(); */
    }
    
    public Node getNodeById(Long arg0) {
        return graphDb.getNodeById(arg0);
/*        if (usingService)
            return graphDbService.getNodeById(arg0);
        else
            return embeddedGraphDb.getNodeById(arg0); */
    }
}
