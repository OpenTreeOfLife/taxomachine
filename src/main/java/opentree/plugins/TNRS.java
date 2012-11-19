package opentree.plugins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import opentree.GraphDatabaseAgent;
import opentree.Taxonomy;
import opentree.tnrs.TNRSNameScrubber;
import opentree.tnrs.TNRSQuery;
import opentree.tnrs.TNRSResults;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
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
	public Representation doTNRSForNames (
			@Source GraphDatabaseService graphDb,
            @Description("A comma-delimited string of names to be queried against the taxonomy db")
			@Parameter(name = "queryString") String queryString) {
	    
        String[] searchStrings = queryString.split("\\s*\\,\\s*");

        GraphDatabaseAgent taxService = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(taxService);
 
        TNRSQuery tnrs = new TNRSQuery(taxonomy);
        TNRSResults results = tnrs.getAllMatches(searchStrings);
        
        taxService.shutdownDb();
        return OpentreeRepresentationConverter.convert(results);
	} 
	
	@Description ("Return information on potential matches to a search query")
    @PluginTarget (GraphDatabaseService.class)
    public Representation doTNRSForTrees (
            @Source GraphDatabaseService graphDb,
            @Description("A string containing tree(s) as readable by the forester library underlying the Archaeopteryx tree viewer")
            @Parameter(name = "treeString") String treeString) throws IOException {
        
        // Write tree string to temp file for ParserUtils. This is a hack, it would be better to just feed the parser
	    // the treeString directly, but none of the appropriate methods seem to accept strings
        String tempFileName = "/tmp/tempTree"; // need to use timestamp to avoid overwriting on asynchronous requests
        FileWriter fstream = new FileWriter(tempFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(treeString);
        out.close();
        
        // read in the treefile
        final File treefile = new File(tempFileName);

        PhylogenyParser parser = null;
        try {
            parser = ParserUtils.createParserDependingOnFileType(treefile, true);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        Phylogeny[] phys = null;

        try {
            phys = PhylogenyMethods.readPhylogenies(parser, treefile);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        
        String[] tipNames = TNRSNameScrubber.scrubNames(phys[0].getAllExternalNodeNames());

        // search for the names
        GraphDatabaseAgent taxService = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(taxService);
        TNRSQuery tnrs = new TNRSQuery(taxonomy);
        TNRSResults results = tnrs.getAllMatches(tipNames);
        
        taxService.shutdownDb();
        return OpentreeRepresentationConverter.convert(results);
    } 
}
