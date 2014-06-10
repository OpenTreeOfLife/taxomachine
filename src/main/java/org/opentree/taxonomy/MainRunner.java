package org.opentree.taxonomy;

import jade.tree.JadeTree;
import jade.tree.TreePrinter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
//import java.util.NoSuchElementException;





import java.util.Map;




//import org.apache.log4j.Logger;
import java.io.FileNotFoundException;

import org.apache.log4j.PropertyConfigurator;
//import org.forester.io.parsers.PhylogenyParser;
//import org.forester.io.parsers.util.ParserUtils;
//import org.forester.phylogeny.Phylogeny;
//import org.forester.phylogeny.PhylogenyMethods;
//import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.opentree.taxonomy.contexts.ContextNotFoundException;
import org.opentree.taxonomy.contexts.TaxonomyContext;
import org.opentree.tnrs.MultipleHitsException;
import org.opentree.tnrs.TNRSMatch;
import org.opentree.tnrs.TNRSNameResult;
import org.opentree.tnrs.TNRSResults;
import org.opentree.tnrs.queries.MultiNameContextQuery;
import org.opentree.tnrs.queries.SimpleQuery;

public class MainRunner {

    private static GraphDatabaseAgent taxdb;

    public void taxonomyLoadParser(String[] args) throws FileNotFoundException, IOException {

        String graphname = "";
        String synonymfile = "";
        if (args[0].equals("inittax") || args[0].equals("addtax")) {
            if (args.length != 4) {
                System.out
                        .println("arguments should be: sourcename filename graphdbfolder\n or (for the inittax command only) you can also use:\n source-properties-file filename graphdbfolder");
                return;
            } else {
                graphname = args[3];
            }
        } else if (args[0].equals("inittaxsyn") || args[0].equals("addtaxsyn") || args[0].equals("loadtaxsyn")) {
            if (args.length != 5) {
                System.out.println("arguments should be: sourcename filename synonymfile graphdbfolder");
                return;
            } else {
                synonymfile = args[3];
                graphname = args[4];
            }
        }
        String sourcename = args[1];
        String filename = args[2];

        taxdb = new GraphDatabaseAgent(graphname);
        TaxonomyLoaderPreottol tld = new TaxonomyLoaderPreottol(taxdb);
        TaxonomyLoaderOTT tlo = new TaxonomyLoaderOTT(taxdb);

        // currently we are assuming that we always want to add taxonomies to the root of the taxonomy
        // (i.e. the life node), but this will have to be changed to add taxonomies that are more
        // specific, such as Hibbett's fungal stuff
        Node lifeNode = tld.getLifeNode();
        System.out.println("life node: " + lifeNode);
        String incomingRootNodeId = null;
        if (lifeNode != null)
            incomingRootNodeId = String.valueOf(lifeNode.getId());

        
        // ===================================== deprecated methods from days of preottol
        
        if (args[0].equals("inittax")) {
            System.out.println("initializing taxonomy from " + filename + " to " + graphname);
            if (new File(sourcename).exists()) {
                System.out.println("Sourcename \"" + sourcename + "\" is a file. This will be read as a properties file describing this source");
                tld.initializeTaxonomyIntoGraph(sourcename, filename, synonymfile);
            } else {
                System.out.println("Sourcename \"" + sourcename + "\" is not a filepath. It will be treated as the source\'s name");
                tld.initializeTaxonomyIntoGraph(sourcename, filename, synonymfile);
            }
            System.out.println("verifying taxonomy");
            tld.verifyLoadedTaxonomy(sourcename);
        } else if (args[0].equals("addtax")) {
            System.out.println("adding taxonomy from " + filename + " to " + graphname);
            // tl.addAdditionalTaxonomyToGraph(sourcename, incomingRootNodeId ,filename, synonymfile);
            tld.addDisconnectedTaxonomyToGraph(sourcename, filename, synonymfile);
            System.out.println("verifying taxonomy");
            tld.verifyLoadedTaxonomy(sourcename);
        } else if (args[0].equals("inittaxsyn")) {
            System.out.println("initializing taxonomy from " + filename + " and synonym file " + synonymfile + " to " + graphname);
            if (new File(sourcename).exists()) {
                System.out.println("Sourcename \"" + sourcename + "\" is a file. This will be read as a properties file describing this source");
                tld.initializeTaxonomyIntoGraph(sourcename, filename, synonymfile);
            } else {
                System.out.println("Sourcename \"" + sourcename + "\" is not a filepath. It will be treated as the source\'s name");
                tld.initializeTaxonomyIntoGraph(sourcename, filename, synonymfile);
            }
            System.out.println("verifying taxonomy");
            tld.verifyLoadedTaxonomy(sourcename);
        } else if (args[0].equals("addtaxsyn")) {
            System.out.println("adding taxonomy from " + filename + "and synonym file " + synonymfile + " to " + graphname);
            // tl.addAdditionalTaxonomyToGraph(sourcename, incomingRootNodeId, filename,synonymfile);
            tld.addDisconnectedTaxonomyToGraph(sourcename, filename, synonymfile);
            System.out.println("verifying taxonomy");
            tld.verifyLoadedTaxonomy(sourcename);
            

        // ===================================== current method using ott taxonomy from smasher
            
        } else if (args[0].equals("loadtaxsyn")) { 
            System.out.println("loading taxonomy from " + filename + " and synonym file " + synonymfile + " to " + graphname);
            //this will create the ott relationships
            tlo.setAddSynonyms(true);
            tlo.setCreateOTTIdIndexes(true);
            tlo.setbuildPreferredIndexes(true);
            tlo.loadOTTIntoGraph(sourcename, filename, synonymfile);
//            System.out.println("verifying taxonomy");
//            tlo.verifyLoadedTaxonomy(sourcename);

        
        // ================= other
            
        } else {
            System.err.println("\nERROR: not a known command");
            taxdb.shutdownDb();
            printHelp();
            System.exit(1);
        }
        taxdb.shutdownDb();
    }

    public void taxonomyQueryParser(String[] args) {

        if (args[0].equals("getsubtree")) {
            if (args.length != 3) {
                System.out.println("arguments should be: graphdbfolder \"nameslist\"");
                return;
            }
        } else if (args[0].equals("checktree")) {
            if (args.length != 4) {
                System.out.println("arguments should be: treefile focalgroup graphdbfolder");
                return;
            }
        } else if (args[0].equals("comptaxgraph")) {
            if (args.length != 4) {
                System.out.println("arguments should be: comptaxgraph query graphdbfolder outfile");
                return;
            }
        } else if (args[0].equals("comptaxtree")) {
            if (args.length != 3) {
                System.out.println("arguments should be: comptaxtree query graphdbfolder");
                return;
            }
        } else if (args[0].equals("comptaxlist")) {
            if (args.length != 3) {
                System.out.println("arguments should be: comptaxlist query graphdbfolder");
                return;
            }
        } else if (args[0].equals("makeottol")) {
            if (args.length != 2) {
                System.out.println("arguments should be: graphdbfolder");
                return;
            }
        } else if (args[0].equals("dumpottol") || args[0].equals("makeottolnamedump")) {
            if (args.length != 3) {
                System.out.println("arguments should be: graphdbfolder outfile");
                return;
            }
        } else if (args[0].equals("makecontexts")) {
            if (args.length != 2) {
                System.out.println("arguments should be: graphdbfolder");
                return;
            }
        } else if (args[0].equals("makegenusindexes")) {
            if (args.length != 2) {
                System.out.println("arguments should be: graphdbfolder");
                return;
            }
        } else if (args.length != 3) {
            System.out.println("arguments should be: query graphdbfolder");
            return;
        }

        TaxonomySynthesizer te = null;
        SimpleQuery tnrs = null;
        Taxon taxon = null;

        if (args[0].equals("getsubtree")) {
            String graphname = args[1];
            String nameString = args[2];

            taxdb = new GraphDatabaseAgent(graphname);
            Taxonomy taxonomy = new Taxonomy(taxdb);
//            tnrs = new SimpleQuery(taxonomy);

            String[] names = nameString.split(",");
            LinkedList<Node> tNodes = new LinkedList<Node>();

            for (String taxName : names) {
                System.out.println("Searching for " + taxName);
                for (Node n : taxonomy.ALLTAXA.findTaxNodesByName(taxName)) {
                    System.out.println("adding " + n.getProperty("name") + " to taxon set");
                    tNodes.add(n);
                }
            }

            TaxonSet taxa = new TaxonSet(tNodes, taxonomy);
            JadeTree subTree = taxa.getPrefTaxSubtree();

            TreePrinter tp = new TreePrinter();
            System.out.println(tp.printNH(subTree));

        } else if (args[0].equals("comptaxtree")) {

            String query = args[1];
            String graphname = args[2];

            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            tnrs = new SimpleQuery(te);
            try {
                taxon = te.getTaxon(tnrs.matchExact(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There is more than one taxon with the name " + query);
            }

            System.out.println("constructing a comprehensive tax tree of " + query);
            taxon.buildTaxonomyTree();

        } else if (args[0].equals("comptaxlist")) {

            String query = args[1];
            String graphname = args[2];

            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            tnrs = new SimpleQuery(te);
            try {
                taxon = te.getTaxon(tnrs.matchExact(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There was more than one match for that name");
            }

            System.out.println("constructing a comprehensive tax tree of " + query);
            taxon.buildTreemachineTaxonList();

        } else if (args[0].equals("comptaxgraph")) {
            String query = args[1];
            String graphname = args[2];
            String outname = args[3];

            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            tnrs = new SimpleQuery(te);
            try {
                taxon = te.getTaxon(tnrs.matchExact(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There was more than one match for that name");
            }

            System.out.println("exporting the subgraph for clade " + query);
            taxon.exportGraphForClade(outname);

        } else if (args[0].equals("jsgraph")) {
            String query = args[1];
            String graphname = args[2];

            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            tnrs = new SimpleQuery(te);
            try {
                taxon = te.getTaxon(tnrs.matchExact(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There was more than one match for that name");
            }

            System.out.println("constructing json graph data for " + query);
            taxon.constructJSONGraph();

        } else if (args[0].equals("checktree")) {
            System.out.println("ERROR: this option is deprecated. use `tnrstree` option instead");
            System.out.println("\ttnrstree <treefile> <graphdbfolder> (check if the taxonomy graph contains names in treefile)");
            /*
             * String query = args[1]; String focalgroup = args[2]; String graphname = args[3]; te = new TaxonomyExplorer(graphname);
             * System.out.println("checking the names of " + query + " against the taxonomy graph"); te.checkNamesInTree(query,focalgroup);
             */

        } else if (args[0].equals("makeottol")) {
            String graphname = args[1];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("making ottol relationships");
            te.makePreferredOTTOLRelationshipsConflicts();
            te.makePreferredOTTOLRelationshipsNOConflicts();

        } else if (args[0].equals("makeottolnamedump")) {
            String graphname = args[1];
            String outfile = args[2];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("dumping names to: " + outfile);

            // testing
            te.OTTOLNameDump(te.ALLTAXA.findPrefTaxNodesByName("Viburnum").get(0), outfile);

            // te.makeOTTOLNameDump(te.ALLTAXA.getRootNode(), outfile);

        } else if (args[0].equals("dumpottol")) {
            String graphname = args[1];
            String outfile = args[2];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("dumping ottol relationships");
            te.dumpPreferredOTTOLRelationships(outfile);
            System.out.println("dumping ottol synonym relationships");
            te.dumpPreferredOTTOLSynonymRelationships(outfile + ".synonyms");
        } else if (args[0].equals("makecontexts")) {
            String graphname = args[1];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("building context-specific indexes");
            te.makeContexts();
        } else if (args[0].equals("makegenusindexes")) {
            String graphname = args[1];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("making species indexes by genus");
            te.makeGenericIndexes();

            
        } else if (args[0].equals("checknames")) {
            String sourcename = args[1];
            String graphname = args[2];
            taxdb = new GraphDatabaseAgent(graphname);
            te = new TaxonomySynthesizer(taxdb);
            System.out.println("checking names from source: " + sourcename);
            te.findEquivalentNamedNodes(sourcename);
        } else {
            System.err.println("\nERROR: not a known command\n");
            printHelp();
            System.exit(1);
        }
        taxdb.shutdownDb();
    }

    /**
     * Intends to be used to compare names that are in a file with the format id parentid name to the ottol names and will output the mappings
     * 
     * @param args
     */
    public void compareNames(String args[]) {
        if (args.length != 4) {
            System.out.println("arguments should be: infile outfile graphdbfolder");
            System.exit(0);
        }
        String filename = args[1];
        String outfilename = args[2];
        String graphname = args[3];
        taxdb = new GraphDatabaseAgent(graphname);
        System.out.println("checking the names from " + filename);
        TaxonomyComparator tc = new TaxonomyComparator();
        tc.compareDontAddNamesToOTTOL(filename, outfilename, taxdb);
        taxdb.shutdownDb();
    }

    public void parseGraftByComp(String args[]) {
        if (args.length != 3) {
            System.out.println("arguments should be: graphdbfolderdom sourcenametocomp");
            System.exit(0);
        }
        System.out.println(args);
        String graphdomname = args[1];
        String sourcename = args[2];
        TaxonomyComparator tc = new TaxonomyComparator();
        GraphDatabaseAgent inga = new GraphDatabaseAgent(graphdomname);
        System.out.println("setting database: " + graphdomname);
        System.out.println("comparing source: " + sourcename);
        tc.compareGraftTaxonomyToDominant(inga, sourcename);
    }

    public void recalculateMRCAS(String args[]) {
        if (args.length != 2) {
            System.out.println("arguments should be: graphdbfolder");
            System.exit(0);
        }
        System.out.println(args);
        String graphdbname = args[1];
        GraphDatabaseAgent inga = new GraphDatabaseAgent(graphdbname);
        System.out.println("setting database: " + graphdbname);
        TaxonomyLoaderPreottol tld = new TaxonomyLoaderPreottol(inga);
        Node dbnode = tld.getLifeNode();
        System.out.println("removing mrcas");
        tld.removeMRCAs(dbnode);
        System.out.println("adding mrcas");
        tld.initMrcaForTipsAndPO(dbnode);
        System.out.println("verifying taxonomy");
        tld.verifyMainTaxonomy();
    }

    public void parseTNRSRequest(String args[]) {
        if (args[0].equals("tnrsbasic")) {
            if (args.length != 3 && args.length != 4) {
                System.out.println("arguments should be: namestring graphdbfolder [context]");
                return;
            }
        } else if (args[0].equals("tnrstree")) {
            if (args.length != 3 && args.length != 4) {
                System.out.println("arguments should be: treefile graphdbfolder [context]");
                return;
            }
        }

        String graphName = args[2];

        String contextName = null;
        if (args.length == 4) {
            contextName = args[3];
        }

        taxdb = new GraphDatabaseAgent(graphName);
        Taxonomy taxonomy = new Taxonomy(taxdb);

        System.out.println("Looking for " + contextName);
        TaxonomyContext context = null;
        try {
        	context = taxonomy.getContextByName(contextName);
        } catch (ContextNotFoundException cnfx) {
            cnfx.printStackTrace();
        }

        System.out.println("Found " + context.getDescription().name);

        MultiNameContextQuery tnrs = new MultiNameContextQuery(taxonomy);
        // TNRSAdapteriPlant iplant = new TNRSAdapteriPlant();
        TNRSResults results = null;

        if (args[0].equals("tnrsbasic")) {
            String[] searchStrings = args[1].split("\\s*\\,\\s*");

/*            HashSet<String> names = Utils.stringArrayToHashset(searchStrings);

            for (int i = 0; i < searchStrings.length; i++) {
                System.out.println(searchStrings[i]);
            } */
            
            Map<Object, String> idNameMap = new HashMap<Object, String>();
    		for (String name : searchStrings) {
    			idNameMap.put(name, name);
    		}
    		
            results = tnrs.
            		setSearchStrings(idNameMap).
            		setContext(context).
            		setAutomaticContextInference(false).
            		runQuery().
            		getResults();

        } else if (args[0].equals("tnrstree")) {

        	System.out.println("\n\nnot implemented\n\n");
//      	System.exit(0);
/*            // TODO: for files containing multiple trees, make sure to do TNRS just once
            // read in the treefile
            final File treefile = new File(args[1]);
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

            // TODO: use MRCA of tree as query context
            // TODO: use tree structure to help differentiate homonyms
            String[] tipNames = phys[0].getAllExternalNodeNames();

            // TNRSNameScrubber scrubber = new TNRSNameScrubber(searchStrings);
            String[] cleanedNames = TNRSNameScrubber.scrubBasic(tipNames);

            HashSet<String> names = Utils.stringArrayToHashset(cleanedNames);
            // scrubber.review(); // print old and cleaned names
            
            Map<Object, String> idNameMap = new HashMap<Object, String>();
    		for (String name : names) {
    			idNameMap.put(name, name);
    		}
            
            results = tnrs.
            		setSearchStrings(idNameMap).
            		setContext(context).
            		setAutomaticContextInference(false).
            		runQuery().
            		getResults(); */
        }

        for (TNRSNameResult nameResult : results) {
            System.out.println(nameResult.getId());
            for (TNRSMatch m : nameResult) {
                System.out.println("\t" + m.toString());
            }
        }

        System.out.println("\nNames that could not be matched:");
        for (Object id: results.getUnmatchedNameIds()) {
            System.out.println(id);
        }
        taxdb.shutdownDb(); 
    }

    public static void printHelp() {
        System.out.println("==========================");
        System.out.println("usage: taxomachine command options");
        System.out.println("");
        System.out.println("commands");
        System.out.println("---taxonomy---");
        System.out.println("\tinittax <sourcename> <filename> <graphdbfolder> (initializes the tax graph with a tax list)");
        System.out.println("\taddtax <sourcename> <filename> <graphdbfolder> (adds a tax list into the tax graph)");
        System.out.println("\tinittaxsyn <sourcename> <filename> <synonymfile> <graphdbfolder> (initializes the tax graph with a list and synonym file)");
        System.out.println("\tloadtaxsyn <sourcename> <filename> <synonymfile> <graphdbfolder> (load ott from the files created in opentree)");
        System.out.println("\taddtaxsyn <sourcename> <filename> <synonymfile> <graphdbfolder> (adds a tax list and synonym file)");
        System.out.println("\tupdatetax <filename> <sourcename> <graphdbfolder> (updates a specific source taxonomy)");
        System.out.println("\tmakeottol <graphdbfolder> (creates the preferred ottol branches)");
        System.out.println("\tdumpottol <graphdbfolder> <filename> (just dumps the ottol branches to a file to be ingested elsewhere)");
        System.out.println("\tmakeottolnamedump <graphdbfolder> <filename> (dumps the recognized ottol names in a format consistent with phylotastic treestores)");
        System.out.println("\tgraftbycomp <graphdbfolder_dom> <sourcename> (graphs an addedtaxonomy into main using the comparator)");
        System.out.println("\trecalculatemrcas <graphdbfolder> (deletes the mrca and nested mrcas and recalculates them)");
        System.out.println("\tmakecontexts <graphdbfolder> (build context-specific indexes; requires that makeottol has already been run)");
        System.out.println("\tmakegenusindexes <graphdbfolder> (build indexes of species for each genus; requires that makeottol has already been run)");
        System.out.println("\tchecknames <sourcename> <graphdbfolder>");
        System.out.println("\tcomparenames <filename> <outfile> <graphdbfolder> (compare the names from a file to the ottol names and output the mappings of names)");
        
        System.out.println("\n---taxquery---");
        System.out.println("\tcomptaxtree <name> <graphdbfolder> (construct a comprehensive tax newick)");
        System.out.println("\tcomptaxlist <name> <graphdbfolder> (construct a comprehensive tax list in the input format expected by treemachine/taxomachine)");
        System.out.println("\tcomptaxgraph <name> <graphdbfolder> <outdotfile> (construct a comprehensive taxonomy in dot)");
        System.out.println("\tjsgraph <name> <graphdbfolder> (constructs a json file from tax graph)");
        System.out.println("\tchecktree <filename> <focalgroup> <graphdbfolder> (checks names in tree against tax graph)");
        System.out.println("\tgetsubtree <graphdbfolder> \"<nameslist>\" (find the subgraph for the specified taxa)");
        
        System.out.println("\n---taxonomic name resolution services---");
        System.out.println("\ttnrsbasic <querynames> <graphdbfolder> [contextname] (check if the taxonomy graph contains comma-delimited names)");
        System.out.println("\ttnrstree <treefile> <graphdbfolder> [contextname] (check if the taxonomy graph contains names in treefile)\n");

        System.out.println("\n---System properties that affect taxomachine behavior ---");
        System.out.println("\t-Dopentree.taxomachine.num.transactions=# Sets the maximum # of transactions that will be buffered.\n\t\t\tDefault: 10000. Use higher for better performance, and lower #s for less memory usage.");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        PropertyConfigurator.configure(System.getProperties());
        System.out.println("\ntaxomachine version alpha.alpha.prealpha");
        // read the max # of database transactions to be buffered from a system property
        String numTransactionsProperty = System.getProperty("opentree.taxomachine.num.transactions");
        if (numTransactionsProperty != null) {
            try {
                int ntp = Integer.parseInt(numTransactionsProperty);
                if (ntp < 1) {
                    throw new NumberFormatException();
                }
                TaxonomyLoaderOTT.transaction_iter = ntp;
                TaxonomySynthesizer.transaction_iter = ntp;
                System.err.println("\n# transactions read from properties =" + ntp);
            } catch (NumberFormatException nfe) {
                System.err.println("\nExpected a positive number for the opentree.taxomachine.num.transactions property, but got \"" + numTransactionsProperty + "\"\nExiting...\n");
                System.exit(1);
            }
        }
        
        try {
            if (args.length == 0 || args[0].equals("help")) {
                printHelp();
                System.exit(0);
            } else if (args.length < 2) {
                System.err.println("\nERROR: expecting multiple arguments\n");
                printHelp();
                System.exit(1);
            } else {
                MainRunner mr = new MainRunner();

                // temporary function to play with TNRS name cleaning from trees
                /*
                 * if (args[0].matches("testScrub")) { mr.testSrcub(args); System.exit(0); }
                 */

                if (args[0].equals("inittax")
                        || args[0].equals("addtax")
                        || args[0].equals("inittaxsyn")
                        || args[0].equals("addtaxsyn")
                        || args[0].equals("loadtaxsyn")) {
                    mr.taxonomyLoadParser(args);
                } else if (args[0].equals("comptaxtree")
                        || args[0].equals("comptaxlist")
                        || args[0].equals("comptaxgraph")
                        || args[0].equals("findcycles")
                        || args[0].equals("jsgraph")
                        || args[0].equals("checktree")
                        || args[0].equals("makeottol")
                        || args[0].equals("makeottolnamedump")
                        || args[0].equals("dumpottol")
                        || args[0].equals("makecontexts")
                        || args[0].equals("makegenusindexes")
                        || args[0].equals("checknames")
                        || args[0].equals("getsubtree")) {
                    mr.taxonomyQueryParser(args);
                } else if (args[0].equals("recalculatemrcas")) {
                    mr.recalculateMRCAS(args);
                } else if (args[0].equals("graftbycomp")) {
                    mr.parseGraftByComp(args);
                } else if (args[0].matches("tnrsbasic|tnrstree")) {
                    mr.parseTNRSRequest(args);
                    // TEMP
                } else if (args[0].equals("addlifenode")) {
                    TaxonomySynthesizer ts = new TaxonomySynthesizer(new GraphDatabaseAgent(args[1]));
                    Node lifeNode = ts.getLifeNode();
                    System.out.println("lifenode: " + lifeNode.toString() + " " + lifeNode.getProperty("name"));
                    ts.addToPreferredIndexesAtomicTX(lifeNode, ts.ALLTAXA);
                } else if (args[0].equals("comparenames")) {
                    mr.compareNames(args);
                } else {
                    System.err.println("Unrecognized command \"" + args[0] + "\"");
                    printHelp();
                    System.exit(1);
                }
            }
        } catch (Throwable x) {
            System.err.println("\nExiting due to an exception:\n " + x.getMessage() + "\nStack trace:\n");
            x.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
