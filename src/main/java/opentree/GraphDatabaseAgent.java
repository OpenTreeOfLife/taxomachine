package opentree;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class GraphDatabaseAgent {

    private static EmbeddedGraphDatabase embeddedGraphDb;
    private static GraphDatabaseService graphDbService;
    private static boolean embedded;

    public GraphDatabaseAgent(GraphDatabaseService gdbs) {
        graphDbService = gdbs;
        embedded = false;
    }

    public GraphDatabaseAgent(EmbeddedGraphDatabase egdb) {
        embeddedGraphDb = egdb;
        embedded = true;
    }

    public GraphDatabaseAgent(String graphDbName) {
        embeddedGraphDb = new EmbeddedGraphDatabase(graphDbName);
        embedded = true;
    }

    public Index<Node> getNodeIndex(String indexName) {
        if (embedded)
            return embeddedGraphDb.index().forNodes(indexName);
        else
            return graphDbService.index().forNodes(indexName);
    }

    public Index<Relationship> getRelIndex(String indexName) {
        if (embedded)
            return embeddedGraphDb.index().forRelationships(indexName);
        else
            return graphDbService.index().forRelationships(indexName);
    }
    
    public Node createNode() {
        if (embedded)
            return embeddedGraphDb.createNode();
        else
            return graphDbService.createNode();
    }

    public Transaction beginTx() {
        if (embedded)
            return embeddedGraphDb.beginTx();
        else
            return graphDbService.beginTx();
    }

    public Node getNodeById(Long arg0) {
        if (embedded)
            return embeddedGraphDb.getNodeById(arg0);
        else
            return graphDbService.getNodeById(arg0);
    }

    public void shutdownDb() {
        registerShutdownHook();
    }

    protected static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                if (embedded)
                    embeddedGraphDb.shutdown();
                else
                    graphDbService.shutdown();
            }
        });
    }
}
