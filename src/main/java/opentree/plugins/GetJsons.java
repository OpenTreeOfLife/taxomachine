package opentree.plugins;

import java.util.ArrayList;
import java.util.HashMap;

import opentree.GraphDatabaseAgent;
import opentree.Taxon;
import opentree.TaxonomyBrowser;
import opentree.tnrs.MultipleHitsException;
import opentree.tnrs.TNRSMatch;
import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSQuery;
import opentree.tnrs.TNRSResults;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.OpentreeRepresentationConverter;

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

    @Description("Return a JSON with node ids given a name")
    @PluginTarget(GraphDatabaseService.class)
    public Object getNodeIDJSONFromName(@Source GraphDatabaseService graphDb,
            @Description("Name of node to find.") @Parameter(name = "nodename", optional = true) String nodename) {

        HashMap<String,String> results = new HashMap<String, String>();

        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        TaxonomyBrowser taxonomy = new TaxonomyBrowser(gdb);
        TNRSQuery tnrs = new TNRSQuery(taxonomy);
        
        TNRSNameResult matches = tnrs.getExactMatches(nodename).iterator().next();

        for (TNRSMatch m : matches)
            results.put("nodeid",String.valueOf(m.getMatchedNode().getId()));

        if (results.size() < 1)
            results.put("error", "Could not find any taxon by that name");

        return OpentreeRepresentationConverter.convert(results);
    }
}
