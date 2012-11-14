package opentree.plugins;

import opentree.GraphDatabaseAgent;
import opentree.TaxonomySynthesizer;
import opentree.tnrs.TNRSQuery;
import opentree.tnrs.TNRSResults;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.OpentreeRepresentationConverter;

public class TNRS extends ServerPlugin {
	
	@Description ("Return information on potential matches to a search query")
	@PluginTarget (GraphDatabaseService.class)
	public Representation doTNRSForNames(
			@Source GraphDatabaseService graphDb,
            @Description("A comma-delimited string of names to be queried against the taxonomy db")
			@Parameter(name = "queryString") String queryString) {
	    
        String[] searchStrings = queryString.split("\\s*\\,\\s*");

        GraphDatabaseAgent taxService = new GraphDatabaseAgent(graphDb);
        TaxonomySynthesizer taxonomy = new TaxonomySynthesizer(taxService);
 
        TNRSQuery tnrs = new TNRSQuery(taxonomy);
        TNRSResults results = tnrs.getAllMatches(searchStrings);
        
        taxService.shutdownDb();
        return OpentreeRepresentationConverter.convert(results);
	} 
}
