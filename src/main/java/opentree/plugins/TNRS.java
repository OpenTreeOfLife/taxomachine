package opentree.plugins;

import java.util.ArrayList;

import opentree.TaxonomyExplorer;
import opentree.tnrs.TNRSMatch;
import opentree.tnrs.TNRSMatchSet;
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
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.TNRSResultsToRepresentationConverter;

public class TNRS extends ServerPlugin {
	
	@Description ("Return information on potential matches to a search query")
	@PluginTarget (GraphDatabaseService.class)
	public Representation doTNRSForNames(
			@Source GraphDatabaseService graphDb,
            @Description("A comma-delimited string of names to be queried upon")
			@Parameter(name = "queryString") String queryString) {
	    
        String[] searchStrings = queryString.split("\\s*\\,\\s*");

        TaxonomyExplorer taxonomy = new TaxonomyExplorer();
        taxonomy.setDbService(graphDb);

        int nSearchStrings = searchStrings.length;
        ArrayList<TNRSNameResult> results = new ArrayList<TNRSNameResult>();
        
        // Need to instantiate the TNRS object using all names
        // Get the context and the MRCA of the names, and get all the results
        
        for (int i = 0; i < nSearchStrings; i++) {
            
            // then for each name, just get those results
            // would be useful to be able to return the results return
            // as an iterator of TNRSNameResult objects
            
        	TNRSNameResult r = new TNRSNameResult();
        	r.queried_name = searchStrings[i];

        	TNRSQuery tnrs = new TNRSQuery(taxonomy);
            r.matches = tnrs.getAllMatches(r.queried_name);
            
            results.add(r);
        }
        
        taxonomy.shutdownDB();
        return TNRSResultsToRepresentationConverter.convert(results);
	} 
}
