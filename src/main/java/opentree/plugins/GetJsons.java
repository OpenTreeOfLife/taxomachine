package opentree.plugins;

import java.util.ArrayList;

import opentree.Taxon;
import opentree.TaxonomyExplorer;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;

public class GetJsons extends ServerPlugin {

    @Description("Return a JSON with alternative TAXONOMIC relationships noted and returned")
    @PluginTarget(Node.class)
    public String getConflictTaxJsonAltRel(@Source Node source,
            @Description("The dominant source.") @Parameter(name = "domsource", optional = true) String domsource,
            @Description("The list of alternative relationships to prefer.") @Parameter(name = "altrels", optional = true) Long[] altrels,
            @Description("A new relationship nub.") @Parameter(name = "nubrel", optional = true) Long nubrel) {
        String retst = "";
//        TaxonomyExplorer te = new TaxonomyExplorer();
        Taxon taxon;

        if (nubrel != null) {
            Relationship rel = source.getGraphDatabase().getRelationshipById(nubrel);
            ArrayList<Long> rels = new ArrayList<Long>();
            if (altrels != null)
                for (int i = 0; i < altrels.length; i++) {
                    rels.add(altrels[i]);
                }
            taxon = new Taxon(rel.getEndNode());
            retst = taxon.constructJSONAltRels((String) rel.getProperty("source"), rels);

        } else {
            ArrayList<Long> rels = new ArrayList<Long>();
            if (altrels != null)
                for (int i = 0; i < altrels.length; i++) {
                    rels.add(altrels[i]);
                }
            taxon = new Taxon(source);
            retst = taxon.constructJSONAltRels(domsource, rels);
        }
        return retst;
    }

    @Description("Return a JSON with the node id given a name")
    @PluginTarget(GraphDatabaseService.class)
    public String getNodeIDJSONFromName(@Source GraphDatabaseService graphDb,
            @Description("Name of node to find.") @Parameter(name = "nodename", optional = true) String nodename) {
        String retst = "";
        System.out.println(nodename);
        IndexHits<Node> hits = graphDb.index().forNodes("taxNamedNodes").get("name", nodename);
        try {
            Node firstNode = hits.next();
            hits.close();
            if (firstNode == null) {
                retst = "[]";
            } else {
                retst = "[{\"nodeid\":" + firstNode.getId() + "}]";
            }
        } catch (java.lang.Exception jle) {
            retst = "[]";
        }
        return retst;
    }
}
