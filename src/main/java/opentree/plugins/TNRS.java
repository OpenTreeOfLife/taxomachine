package opentree.plugins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import opentree.ContextDescription;
import opentree.ContextGroup;
import opentree.ContextNotFoundException;
import opentree.GraphDatabaseAgent;
import opentree.Taxonomy;
import opentree.TaxonomyContext;
import opentree.tnrs.ContextResult;
import opentree.tnrs.TNRSNameScrubber;
import opentree.tnrs.TNRSResults;
import opentree.tnrs.queries.MultiNameContextQuery;
import opentree.utils.Utils;

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

    @Description("Find the least inclusive taxonomic context defined for the provided set of taxon names")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getContextForNames(
            @Source GraphDatabaseService graphDb,
            @Description("A comma-delimited string of taxon names to be queried against the taxonomy db") @Parameter(name = "queryString") String queryString) {

        String[] searchStrings = queryString.split("\\s*\\,\\s*");
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);

        MultiNameContextQuery tnrs = new MultiNameContextQuery(taxonomy);
        HashSet<String> names = Utils.stringArrayToHashset(searchStrings);
        
        // this hashset will hold ambiguous names (i.e. synonyms)
        HashSet<String> namesNotMatched = (HashSet<String>) tnrs.setSearchStrings(names).inferContextAndReturnAmbiguousNames();
        TaxonomyContext inferredContext = tnrs.getContext();

        // create a container to hold the results
        ContextResult contextResult = new ContextResult(inferredContext, namesNotMatched);
        
        gdb.shutdownDb();
        
        // convert the results to JSON and return them
        return OpentreeRepresentationConverter.convert(contextResult);
    	
    }
	
    @Description("Return information on potential matches to a search query")
    @PluginTarget(GraphDatabaseService.class)
    public Representation doTNRSForNames(
            @Source GraphDatabaseService graphDb,
            @Description("A comma-delimited string of taxon names to be queried against the taxonomy db") @Parameter(name = "queryString") String queryString,
            @Description("The name of the taxonomic context to be searched") @Parameter(name = "contextName", optional = true) String contextName) throws ContextNotFoundException {

        String[] searchStrings = queryString.split("\\s*\\,\\s*");
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);
        
        // attempt to get the named context, will throw exception if a name is supplied but no corresponding context can be found
        TaxonomyContext context = null;
        boolean useAutoInference = true;
        if (contextName != null) {
        	context = taxonomy.getContextByName(contextName);
        	useAutoInference = false;
        }

        MultiNameContextQuery tnrs = new MultiNameContextQuery(taxonomy);
        HashSet<String> names = Utils.stringArrayToHashset(searchStrings);
        TNRSResults results = tnrs.setSearchStrings(names).setContext(context).setAutomaticContextInference(useAutoInference).getTNRSResultsForSetNames();

        gdb.shutdownDb();
        return OpentreeRepresentationConverter.convert(results);
    }

    @Description("Return information on potential matches to a search query")
    @PluginTarget(GraphDatabaseService.class)
    public Representation doTNRSForTrees(
            @Source GraphDatabaseService graphDb,
            @Description("A string containing tree(s) in a format readable by the forester library")
                @Parameter(name = "treeString") String treeString/*,
            @Description("The name of the taxonomic context to use. May be omitted if not known")
                @Parameter(name = "contextName", optional = true) String contextName*/) throws IOException, ContextNotFoundException {

        // Write tree string to temp file for ParserUtils. This is a hack, it would be better to just feed the parser
        // the treeString directly, but all the appropriate methods seem to want files
        String tempFileName = "/tmp/tempTree"; // TODO: need to use timestamp to avoid overwriting on asynchronous requests
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

        // clean the names extracted from the treefile
        String[] cleanNames = TNRSNameScrubber.scrubBasic(phys[0].getAllExternalNodeNames());
        
        // connect to taxonomy db
        GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
        Taxonomy taxonomy = new Taxonomy(gdb);

        // attempt to get the named context, will throw exception if a name is supplied but no corresponding context can be found
//        TaxonomyContext context = taxonomy.getContextByName(contextName);

        // do TNRS
        MultiNameContextQuery tnrs = new MultiNameContextQuery(taxonomy);
        HashSet<String> names = Utils.stringArrayToHashset(cleanNames);
        TNRSResults results = tnrs.setSearchStrings(names).getTNRSResultsForSetNames();

        gdb.shutdownDb();
        return OpentreeRepresentationConverter.convert(results);
    }

    @Description("Return information on available taxonomic contexts")
    @PluginTarget(GraphDatabaseService.class)
    public Representation getContextsJSON(
            @Source GraphDatabaseService graphDb) throws IOException {
                
        // format is: HashMap<groupName, ArrayList<contextName>>
        HashMap<String, ArrayList<String>> contexts = new HashMap<String, ArrayList<String>>();

        for (ContextGroup group : ContextGroup.values()) {

            // get all the contexts for each context group
            ArrayList<String> subcontexts = new ArrayList<String>();
            for (ContextDescription cd : group.getDescriptions()) {
                subcontexts.add(cd.name);
            }
            contexts.put(group.toString(), subcontexts);
        }
        
        return OpentreeRepresentationConverter.convert(contexts);
    }
}
