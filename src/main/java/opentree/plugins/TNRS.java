package opentree.plugins;

import opentree.TaxonomyExplorer;
import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSAdapter;
import opentree.tnrs.TNRSQuery;
import opentree.tnrs.adapters.iplant.TNRSAdapteriPlant;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;

public class TNRS extends ServerPlugin {

	@Description ("Return information on potential matches to a search query")
	@PluginTarget (GraphDatabaseService.class)
	public TNRSNameResult[] doTNRSForNames(@Source GraphDatabaseService graphDb,
            @Description("A comma-delimited string of names to be queried upon")
			@Parameter(name = "queryString") String queryString) {
	    
        String[] searchStrings = queryString.split("\\s*\\,\\s*");

        TaxonomyExplorer taxonomy = new TaxonomyExplorer();
        taxonomy.setDbService(graphDb);
        TNRSAdapter iplant = new TNRSAdapteriPlant();

        int nSearchStrings = searchStrings.length;
        TNRSNameResult[] results = new TNRSNameResult[nSearchStrings];
        
        for (int i = 0; i < nSearchStrings; i++) {
        	results[i].queried_name = searchStrings[i];

        	// TNRSQuery expects an array of names, so we create a length-1 array for each name
        	String[] queriedName = new String[1];
        	queriedName[1] = searchStrings[i];

        	TNRSQuery tnrs = new TNRSQuery(taxonomy);
            results[i].matches = tnrs.getMatches(queriedName, iplant);
        }

/*        String response = "{\"results\":[";
        boolean first = true;
        for (int i = 0; i < searchStrings.length; i++) {
            String[] queriedName = new String[1]; // TODO: should perhaps change this to accept only a single string instead of an array.
            queriedName[0] = searchStrings[i];
            if (first)
                first = false;
            else
                response += ",";

            response += "{\"queried_name\":\"" + queriedName[0] + "\",\"matches\":[";
            TNRSQuery tnrs = new TNRSQuery(taxonomy);
            TNRSMatchSet matches = tnrs.getMatches(queriedName, iplant);

            boolean first2 = true;
            for (TNRSMatch m : matches) {
                if (first2)
                    first2 = false;
                else
                    response += ",";                
                response += "{";
                
                response += "\"matched_node_id\":" + m.getMatchedNodeId() + ",";
                response += "\"matched_node_name\":\"" + m.getMatchedNodeName() + "\",";
                response += "\"synonym_node_id\":" + m.getSynonymNodeId() + "\",";
                response += "\"synonym_node_name\":\"" + m.getSynonymNodeName() + "\",";
                response += "\"source\":\"" + m.getSource() + "\",";
                response += "\"is_perfect_match\":\"" + m.getIsPerfectMatch() + "\",";
                response += "\"is_approximate_node\":\"" + m.getIsApproximate() + "\",";
                response += "\"is_synonym\":\"" + m.getIsSynonym() + "\",";
                response += "\"is_homonym\":\"" + m.getIsHomonym() + "\",";
                response += "\"score\":" + m.getScore();
                
                // TODO: add addtional data for external matches, or maybe just a URI for these pointing to the original source

                response += "}";
            }
            
            response += "]}";
        }
        response += "]}"; */
        
        taxonomy.shutdownDB();		
        return results;
	}
}
