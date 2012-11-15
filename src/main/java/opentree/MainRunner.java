package opentree;

import java.io.File;
import java.io.IOException;

import opentree.tnrs.MultipleHitsException;
import opentree.tnrs.TNRSMatch;
import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSQuery;
import opentree.tnrs.TNRSResults;

//import org.apache.log4j.Logger;
import java.io.FileNotFoundException;
import org.apache.log4j.PropertyConfigurator;

import org.forester.io.parsers.PhylogenyParser;
import org.forester.io.parsers.util.ParserUtils;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyMethods;
import org.neo4j.graphdb.Node;


public class MainRunner {
    
    private static GraphDatabaseAgent taxdb;

	public void taxonomyLoadParser(String [] args) throws FileNotFoundException, IOException {
		String graphname = "";
		String synonymfile = "";
		if (args[0].equals("inittax") || args[0].equals("addtax")) {
			if (args.length != 4) {
				System.out.println("arguments should be: sourcename filename graphdbfolder\n or (for the inittax command only) you can also use:\n source-properties-file filename graphdbfolder");
				return;
			} else {
				graphname = args[3];
			}
		} else if (args[0].equals("inittaxsyn") || args[0].equals("addtaxsyn")) {
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
		TaxonomyLoader tl = new TaxonomyLoader(taxdb);

        // currently we are assuming that we always want to add taxonomies to the root of the taxonomy
		// (i.e. the life node), but this will have to be changed to add taxonomies that are more
		// specific, such as Hibbett's fungal stuff
		Node lifeNode = tl.getLifeNode();
		String incomingRootNodeId = null;
		if (lifeNode != null)
		    incomingRootNodeId = String.valueOf(lifeNode.getId());
		
		if (args[0].equals("inittax")) {
			System.out.println("initializing taxonomy from " + filename + " to " + graphname);
			if (new File(sourcename).exists()) {
				System.out.println("Sourcename \"" + sourcename + "\" is a file. This will be read as a properties file describing this source");
				tl.initializeTaxonomyIntoGraphProperty(sourcename, filename, synonymfile);
			}
			else {
				System.out.println("Sourcename \"" + sourcename + "\" is not a filepath. It will be treated as the source\'s name");
				tl.initializeTaxonomyIntoGraphName(sourcename, filename, synonymfile);
			}
		} else if(args[0].equals("addtax")) {
			System.out.println("adding taxonomy from " + filename + " to "+ graphname);
			tl.addAdditionalTaxonomyToGraph(sourcename, incomingRootNodeId ,filename, synonymfile);

		} else if (args[0].equals("inittaxsyn")) {
			System.out.println("initializing taxonomy from " + filename + " and synonym file " + synonymfile + " to " + graphname);
			if (new File(sourcename).exists()) {
				System.out.println("Sourcename \"" + sourcename + "\" is a file. This will be read as a properties file describing this source");
				tl.initializeTaxonomyIntoGraphProperty(sourcename, filename, synonymfile);
			}
			else {
				System.out.println("Sourcename \"" + sourcename + "\" is not a filepath. It will be treated as the source\'s name");
				tl.initializeTaxonomyIntoGraphName(sourcename, filename, synonymfile);
			}
		} else if (args[0].equals("addtaxsyn")) {
			System.out.println("adding taxonomy from " + filename + "and synonym file " + synonymfile + " to " + graphname);
			tl.addAdditionalTaxonomyToGraph(sourcename, incomingRootNodeId, filename,synonymfile);

		} else {
			System.err.println("\nERROR: not a known command");
			taxdb.shutdownDb();
			printHelp();
			System.exit(1);
		}
		taxdb.shutdownDb();
	}
	
	public void taxonomyQueryParser(String [] args) {
		
	    if (args[0].equals("checktree")) {
			if (args.length != 4) {
				System.out.println("arguments should be: treefile focalgroup graphdbfolder");
				return;
			}
		} else if (args[0].equals("comptaxgraph")) {
			if (args.length != 4) {
				System.out.println("arguments should be: comptaxgraph query graphdbfolder outfile");
				return;
			}
		} else if (args[0].equals("makeottol")) {
			if (args.length != 2) {
				System.out.println("arguments should be: graphdbfolder");
				return;
			}
		} else if (args.length != 3) {
			System.out.println("arguments should be: query graphdbfolder");
			return;
		}
		
		TaxonomySynthesizer te = null;
        TNRSQuery tnrs = null;
        Taxon taxon = null;

		if (args[0].equals("comptaxtree")) {
			String query = args[1];
            String graphname = args[2];

            taxdb = new GraphDatabaseAgent(graphname);
            te =  new TaxonomySynthesizer(taxdb);
            tnrs = new TNRSQuery(te);
            try {
                taxon = new Taxon(tnrs.getExactMatches(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There was more than one match for that name");
            }

            System.out.println("constructing a comprehensive tax tree of " + query);
    		taxon.buildTaxonomyTree();

		} else if (args[0].equals("comptaxgraph")) {
			String query = args[1];
			String graphname = args[2];
			String outname = args[3];
			
            taxdb = new GraphDatabaseAgent(graphname);
            te =  new TaxonomySynthesizer(taxdb);
            tnrs = new TNRSQuery(te);
            try {
                taxon = new Taxon(tnrs.getExactMatches(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There was more than one match for that name");
            }

            System.out.println("exporting the subgraph for clade " + query);
            taxon.exportGraphForClade(outname);
            
		} else if (args[0].equals("findcycles")) {
			String query = args[1];
			String graphname = args[2];
			
            taxdb = new GraphDatabaseAgent(graphname);
            te =  new TaxonomySynthesizer(taxdb);

            System.out.println("finding taxonomic cycles for " + query);
			te.findTaxonomyCycles(query);

		} else if (args[0].equals("jsgraph")) {
			String query = args[1];
			String graphname = args[2];
			
            taxdb = new GraphDatabaseAgent(graphname);
            te =  new TaxonomySynthesizer(taxdb);
            tnrs = new TNRSQuery(te);
            try {
                taxon = new Taxon(tnrs.getExactMatches(query).getSingleMatch().getMatchedNode());
            } catch (MultipleHitsException ex) {
                System.out.println("There was more than one match for that name");
            }

			System.out.println("constructing json graph data for " + query);
			taxon.constructJSONGraph();

		} else if (args[0].equals("checktree")) {
		    System.out.println("ERROR: this option is deprecated. use `tnrstree` option instead");
	        System.out.println("\ttnrstree <treefile> <graphdbfolder> (check if the taxonomy graph contains names in treefile)");
/*			String query = args[1];
			String focalgroup = args[2];
			String graphname = args[3];
			te =  new TaxonomyExplorer(graphname);
			System.out.println("checking the names of " + query + " against the taxonomy graph");
			te.checkNamesInTree(query,focalgroup); */

		} else if (args[0].equals("makeottol")) {
			String graphname = args[1];
            taxdb = new GraphDatabaseAgent(graphname);
            te =  new TaxonomySynthesizer(taxdb);
			System.out.println("making ottol relationships");
			te.makePreferredOTTOLRelationshipsConflicts();
			te.makePreferredOTTOLRelationshipsNOConflicts();

		} else {
			System.err.println("\nERROR: not a known command\n");
			printHelp();
			System.exit(1);
		}

		taxdb.shutdownDb();
	}

    public void parseTNRSRequest(String args[]) {
        
        if (args[0].equals("tnrsbasic")) {
            if (args.length != 3) {
                System.out.println("arguments should be: namestring graphdbfolder");
                return;
            }
        } else if (args[0].equals("tnrstree")) {
            if (args.length != 3) {
                System.out.println("arguments should be: treefile graphdbfolder");
                return;
            }
        }

        String graphName = args[2];
        taxdb = new GraphDatabaseAgent(graphName);
        TaxonomySynthesizer taxonomy = new TaxonomySynthesizer(taxdb);
        TNRSQuery tnrs = new TNRSQuery(taxonomy);
//        TNRSAdapteriPlant iplant = new TNRSAdapteriPlant();
        TNRSResults results = (TNRSResults)null;
        
        if (args[0].compareTo("tnrsbasic") == 0) {
            
            String[] searchStrings = args[1].split("\\s*\\,\\s*");
            for (int i = 0; i < searchStrings.length; i++)
                System.out.println(searchStrings[i]);
            results = tnrs.getAllMatches(searchStrings);
            
        } else if (args[0].compareTo("tnrstree") == 0) {

            /*
            String treeString = "";
            try {
            
                FileInputStream ins = new FileInputStream(args[1]);
                BufferedReader infile = new BufferedReader(new InputStreamReader(ins));
                treeString += infile.readLine();
                infile.close();
            
            } catch (FileNotFoundException e) {
                System.out.println("The specified treefile could not be found.");
                System.exit(0);
            } catch (IOException e) {
                System.out.println("There was a problem reading the treefile.");
                e.printStackTrace();
            }
            
            TreeReader treeReader = new TreeReader();
            JadeTree tree = treeReader.readTree(treeString);

            // get external node names from jade tree
            String[] treeTipNames = new String[tree.getExternalNodeCount()];
            for (int i = 0; i < tree.getExternalNodeCount(); i++) {
                treeTipNames[i] = (tree.getExternalNode(i).getName());
            } */
            

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

	    System.out.println("hello");
            
            // TODO: use MRCA of tree as query context
            // TODO: use tree structure to help differentiate homonyms
            String[] tipNames = phys[0].getAllExternalNodeNames();
            for (int i = 0; i < tipNames.length; i++ )
                System.out.println(tipNames[i]);
            
            // search for the names
            results = tnrs.getAllMatches(phys[0].getAllExternalNodeNames());
          
        }
        
        for (TNRSNameResult nameResult : results) {
            System.out.println(nameResult.getQueriedName());
            for (TNRSMatch m : nameResult) {
                System.out.println("\t" + m.toString());
            }
        }

        System.out.println("\nNames that could not be matched:");
        for (String name : results.getUnmatchedNames()) {
            System.out.println(name);
        }

    }
	
	public static void printHelp(){
		System.out.println("==========================");
		System.out.println("usage: taxomachine command options");
		System.out.println("");
		System.out.println("commands");
		System.out.println("---taxonomy---");
		System.out.println("\tinittax <sourcename> <filename> <graphdbfolder> (initializes the tax graph with a tax list)");
		System.out.println("\taddtax <sourcename> <filename> <graphdbfolder> (adds a tax list into the tax graph)");
		System.out.println("\tinittaxsyn <sourcename> <filename> <synonymfile> <graphdbfolder> (initializes the tax graph with a list and synonym file)");
		System.out.println("\taddtaxsyn <sourcename> <filename> <synonymfile> <graphdbfolder> (adds a tax list and synonym file)");
		System.out.println("\tupdatetax <filename> <sourcename> <graphdbfolder> (updates a specific source taxonomy)");
		System.out.println("\tmakeottol <graphdbfolder> (creates the preferred ottol branches)");
		System.out.println("\n---taxquery---");
		System.out.println("\tcomptaxtree <name> <graphdbfolder> (construct a comprehensive tax newick)");
		System.out.println("\tcomptaxgraph <name> <graphdbfolder> <outdotfile> (construct a comprehensive taxonomy in dot)");
		System.out.println("\tfindcycles <name> <graphdbfolder> (find cycles in tax graph)");
		System.out.println("\tjsgraph <name> <graphdbfolder> (constructs a json file from tax graph)");
		System.out.println("\tchecktree <filename> <focalgroup> <graphdbfolder> (checks names in tree against tax graph)");
        System.out.println("\n---taxonomic name resolution services---");
        System.out.println("\ttnrsbasic <querynames> <graphdbfolder> (check if the taxonomy graph contains comma-delimited names)");
        System.out.println("\ttnrstree <treefile> <graphdbfolder> (check if the taxonomy graph contains names in treefile)");

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure(System.getProperties());
		System.out.println("taxomachine version alpha.alpha.prealpha");
		try {
			if (args.length == 0 || args[0].equals("help")) {
				printHelp();
				System.exit(0);
			} else if (args.length < 2) {
				System.err.println("\nERROR: expecting multiple arguments\n");
				printHelp();
				System.exit(1);
			} else {
				System.out.println("\nThings will happen here!\n");
				MainRunner mr = new MainRunner();
				
				if (args[0].equals("inittax")
						|| args[0].equals("addtax")
						|| args[0].equals("inittaxsyn")
						|| args[0].equals("addtaxsyn")) {
					mr.taxonomyLoadParser(args);
				} else if (args[0].equals("comptaxtree")
						|| args[0].equals("comptaxgraph")
						|| args[0].equals("findcycles")
						|| args[0].equals("jsgraph") 
						|| args[0].equals("checktree")
						|| args[0].equals("makeottol")) {
					mr.taxonomyQueryParser(args);
				} else if (args[0].compareTo("tnrsbasic") == 0 || args[0].compareTo("tnrstree") == 0) {
					mr.parseTNRSRequest(args);
				}else {
					System.err.println("Unrecognized command \"" + args[0] + "\"");
					printHelp();
					System.exit(1);
				}
			}
		}
		catch (Throwable x) {
			System.err.println("\nExiting due to an exception:\n " + x.getMessage() + "\nStack trace:\n");
			x.printStackTrace(System.err);
			System.exit(2);
		}
	}
}
