package opentree.plugins;

import opentree.TaxonomyExplorer;
import opentree.tnrs.TNRSAdapter;
import opentree.tnrs.TNRSMatch;
import opentree.tnrs.TNRSMatchSet;
import opentree.tnrs.TNRSQuery;
import opentree.tnrs.adapters.iplant.TNRSAdapteriPlant;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;

public class TNRS extends ServerPlugin {

	@Description ("Return information on potential matches to a search query")
	@PluginTarget (GraphDatabaseService.class)
	public String doTNRSForNames(@Source GraphDatabaseService graphDb,
            @Description("A comma-delimited string of names to be queried upon")
			@Parameter(name = "queryString") String queryString) {
	    
//		String response = "{\"test\":\"response\"}";
        String[] searchStrings = queryString.split("\\s*\\,\\s*");

        TaxonomyExplorer taxonomy = new TaxonomyExplorer();
        taxonomy.setDbService(graphDb);
        TNRSAdapter iplant = new TNRSAdapteriPlant();
        TNRSQuery tnrs = new TNRSQuery(taxonomy);
        TNRSMatchSet matches = tnrs.getMatches(searchStrings, iplant);

        String response = "{[\"matches\":";
        boolean first = true;
        for (TNRSMatch m : matches) {
            if (first)
                first = false;
            else
                response += ",";
            response += "\"" + m.toString() + "\"";
        }
		
        return response;
	}
	
/*	@Description ("Return a JSON with the node id given a name")
	@PluginTarget (GraphDatabaseService.class)
	public String getNodeIDJSONFromName(@Source GraphDatabaseService graphDb,
			@Description("Name of node to find.")
			@Parameter( name = "nodename", optional= true ) String nodename ){
		String retst = "";
		System.out.println(nodename);
		IndexHits<Node> hits = graphDb.index().forNodes("taxNamedNodes").get("name",nodename);
		try{
			Node firstNode = hits.next();
			hits.close();
			if(firstNode == null){
				retst = "[]";
			}else{
				retst="[{\"nodeid\":"+firstNode.getId()+"}]";
			}
		}catch(java.lang.Exception jle){
			retst = "[]";
		}
		return retst;
	} */
}
