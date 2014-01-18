package org.opentree.taxonomy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.contexts.ContextDescription;
import org.opentree.taxonomy.contexts.TaxonomyContext;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;
import org.opentree.tnrs.MultipleHitsException;
import org.opentree.tnrs.queries.MultiNameContextQuery;

/**
 * TaxonomySynthesize functions
 * ============================
 * make ottol preferred relationships
 * make contexts
 * add preferred index values
 *
 */

public class TaxonomySynthesizer extends Taxonomy {

    public static int transaction_iter = 10000;

    private static final TraversalDescription PREFTAXCHILDOF_TRAVERSAL = Traversal.description().breadthFirst().
            relationships(RelType.PREFTAXCHILDOF, Direction.INCOMING);

    private static final TraversalDescription TAXCHILDOF_TRAVERSAL = Traversal.description().breadthFirst().
            relationships(RelType.TAXCHILDOF, Direction.INCOMING);
    
	private final Index<Node> prefSpeciesByGenus = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_SPECIES_BY_GENUS);

    public TaxonomySynthesizer(GraphDatabaseAgent t) {
        super(t);
    }

    /**
     * Dump all the name data in a format consistent with Phylotastic treestore requirements
     */
    public void OTTOLNameDump(Node rootNode, String outFileName) {
        
        // create file access variables
        File outFileMain = null;
        File outFileMetadata = null;
        FileWriter fwMain = null;
        FileWriter fwMetadata = null;
        BufferedWriter bwMain = null;
        BufferedWriter bwMetadata = null;

        // initialize files
        try {

            outFileMain = new File(outFileName);
            outFileMetadata = new File(outFileName+".metadata");
 
            if (!outFileMain.exists()) { outFileMain.createNewFile(); }
            if (!outFileMetadata.exists()) { outFileMetadata.createNewFile(); }

            fwMain = new FileWriter(outFileMain.getAbsoluteFile());
            fwMetadata = new FileWriter(outFileMetadata.getAbsoluteFile());

            bwMain = new BufferedWriter(fwMain);
            bwMetadata = new BufferedWriter(fwMetadata);

            System.out.println("Test: writing names from " + rootNode.getProperty("name") + " to " + outFileName + ", " + outFileName + ".metadata");
 
        } catch (IOException e) {
            e.printStackTrace();
        }        
        
        String sourceJSON = "\"externalSources\":{";
        // first need to get list of sources, currently including 'nodeid' source
        Index<Node> taxSources = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXONOMY_SOURCES);
        IndexHits<Node> sourceNodes = taxSources.query("source", "*");
        boolean first0 = true;
        for (Node metadataNode : sourceNodes) {
            String sourceName = String.valueOf(metadataNode.getProperty("source"));
            String author = String.valueOf(metadataNode.getProperty("author"));
            String uri = String.valueOf(metadataNode.getProperty("uri"));
            String urlPrefix = String.valueOf(metadataNode.getProperty("urlprefix"));
            String weburl = String.valueOf(metadataNode.getProperty("weburl"));
            if (first0) {
                first0 = false;
            } else {
                sourceJSON += ",";
            }
            sourceJSON += "\"" + sourceName + "\":{\"author\":\"" + author + "\",\"uri\":\"" + uri + "\",\"urlprefix\":\"" + urlPrefix + "\",\"weburl\":\"" + weburl + "\"}";
        }
        sourceNodes.close();
        
        String metadata = "{\"metadata\":{\"version\":\"0\",\"treestoreMetadata\":{\"treestoreShortName\":\"ottol\",\"treestoreLongName\":\"Open Tree of Life\",\"weburl\":\"\",\"urlPrefix\":\"\"},";
        metadata += sourceJSON + "}";

        // write the namedump metadata and source metadata
        try {
            bwMain.write(metadata);
            bwMetadata.write(metadata + "}}");
            bwMetadata.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        

        // will contain data for all taxon nodes with their sources
        HashMap<Node, HashMap<String, String>> nodeSourceMap = new HashMap<Node, HashMap<String, String>>();
        
        for (Node n : PREFTAXCHILDOF_TRAVERSAL.traverse(rootNode).nodes()) {
//            System.out.println("name: " + n.getProperty("name") + "; id: " + String.valueOf(n.getId()));
            
            // source name : source UID
            HashMap<String, String> sourceIdMap = new HashMap<String, String>();
            
            for (Relationship l : n.getRelationships(Direction.OUTGOING, RelType.TAXCHILDOF)) {
//                System.out.println("start node: " + String.valueOf(l.getStartNode().getId()) + "; end node: " + String.valueOf(l.getEndNode().getId()));

                String sourceName = "";
                if (l.hasProperty("source")) {
                    sourceName = String.valueOf(l.getProperty("source"));
                }

                String taxUId = "";
                if (l.hasProperty("childid")) {
                    taxUId = String.valueOf(l.getProperty("childid"));
                }
                
                if (sourceName != "") {
                    sourceIdMap.put(sourceName, taxUId);
                }
            }
            
            // add source info for this taxon to the map
            nodeSourceMap.put(n, sourceIdMap);
            
        }

        try {
            bwMain.write(",\"names\":[");
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        boolean first = true;
        for (Entry<Node, HashMap<String, String>> nameData : nodeSourceMap.entrySet()) {
            Node taxNode = nameData.getKey();
            String taxName = String.valueOf(taxNode.getProperty("name"));
            HashMap<String, String> nameIds = nameData.getValue();
            
            String treestoreId = "";
            String sourceIdString = "";
            boolean first2 = true;
            for (Entry<String, String> source : nameIds.entrySet()) {
                String sourceName = source.getKey();
                String id = source.getValue();

                // set the treestore id using an id we recognize
                if (treestoreId == "") {
                    if (id != "") {
                        treestoreId = sourceName + ":" + id;
                    }
                }
                                
                if (first2) { first2 = false; } else { sourceIdString += ","; }                
                sourceIdString += "\"" + sourceName + "\":\"" + id + "\"";
            }
            
            // for those things that we don't have UIDs for (i.e. gbif names), we are currently spoofing them
            // using the neo4j node ids. this should be unnecessary once we have external UIDs for all our names
            if (treestoreId == "") {
                treestoreId = "nodeid:" + String.valueOf(taxNode.getId());
            }
            
            String nameString = "";
            if (first) { first = false; } else { nameString += ","; }
            nameString += "{\"name\":\"" + taxName + "\",\"treestoreId\":\"" + treestoreId + "\",\"sourceIds\":{" + sourceIdString + "}}";

            try {
                bwMain.write(nameString);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        try {
            bwMain.write("]}}");
            bwMain.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    

    /*
     * TODO: THIS SHOULD BE REPLACED BY TNRS CHECK TREE FUNCTION public void checkNamesInTree(String treefilename,String focalgroup){ PathFinder <Path> pf =
     * GraphAlgoFactory.shortestPath(Traversal.pathExpanderForTypes(RelTypes.TAXCHILDOF, Direction.OUTGOING), 100); IndexHits<Node> foundNodes =
     * findTaxNodeByName(focalgroup); Node focalnode = null; if (foundNodes.size() < 1){ System.out.println("name '" + focalgroup + "' not found. quitting.");
     * return; } else if (foundNodes.size() > 1) { System.out.println("more than one node found for name '" + focalgroup +
     * "'not sure how to deal with this. quitting"); } else { for (Node n : foundNodes) focalnode = n; } String ts = ""; try{ BufferedReader br = new
     * BufferedReader(new FileReader(treefilename)); ts = br.readLine(); br.close(); }catch(IOException ioe){ System.out.println("problem reading tree"); }
     * TreeReader tr = new TreeReader(); JadeTree jt = tr.readTree(ts); System.out.println("tree read"); for(int i=0;i<jt.getExternalNodeCount();i++){
     * IndexHits<Node> hits = taxNodeIndex.get("name", jt.getExternalNode(i).getName().replace("_"," ")); int numh = hits.size(); if (numh == 0)
     * System.out.println(jt.getExternalNode(i).getName()+" gets NO hits"); if (numh > 1){
     * System.out.println(jt.getExternalNode(i).getName()+" gets "+numh+" hits"); for(Node tnode : hits){ Path tpath = pf.findSinglePath(tnode, focalnode); } }
     * hits.close(); } }
     */

    /**
     * Right now this walks from the life node through the entire graph. When there are conflicts, this will create a preferred
     * relationship parallel to an existing NCBI relationship if there is one. If there ar no conflicts, it just passes over.
     */
    public void makePreferredOTTOLRelationshipsConflicts() {

//        TraversalDescription TAXCHILDOF_TRAVERSAL = Traversal.description()
//                .relationships(RelType.TAXCHILDOF, Direction.INCOMING);

        // get start node
        Node life = getLifeNode();
        System.out.println(life.getProperty("name"));

        Transaction tx = beginTx();
        int nRelsAdded = 0;
        try {
            
            // walk all nodes looking for conflicts
            for (Node friendnode : TAXCHILDOF_TRAVERSAL.traverse(life).nodes()) {
                boolean foundConflict = false;
                String endNodeName = "";
                //for more generality this should be changed to a hash
                Relationship ncbirel = null;
                Relationship fungirel = null;

                for (Relationship rel : friendnode.getRelationships(Direction.OUTGOING, RelType.TAXCHILDOF)) {
                    if (rel.getEndNode() == rel.getStartNode()) {
                        System.out.println("\n\n\n!!!!!!!!!!!!!!!!!!!CYCLE! Node " + rel.getEndNode() + " points to itself along relationship: " + rel + "\n\n\n");
                        continue;

                    } else {
                        if (endNodeName == "")
                            endNodeName = (String) rel.getEndNode().getProperty("name");
                        
                        if ((String) rel.getEndNode().getProperty("name") != endNodeName)
                            foundConflict = true;

                        if (((String) rel.getProperty("source")).compareTo("ncbi") == 0)
                            ncbirel = rel;
                        
                        if (((String) rel.getProperty("source")).compareTo("paul_kirk_fungi") == 0)
                        	fungirel = rel;
                    }
                }
                
                if (foundConflict && (ncbirel != null || fungirel != null)) {
                    nRelsAdded += 1;
                    // System.out.println("would make one from "+ncbirel.getStartNode().getProperty("name")+" "+ncbirel.getEndNode().getProperty("name"));
                    if (ncbirel != null){
                    	if (ncbirel.getStartNode().getId() != ncbirel.getEndNode().getId()) {
                    		ncbirel.getStartNode().createRelationshipTo(ncbirel.getEndNode(), RelType.PREFTAXCHILDOF);
                    		Relationship newrel2 = ncbirel.getStartNode().createRelationshipTo(ncbirel.getEndNode(), RelType.TAXCHILDOF);
                    		newrel2.setProperty("source", "ottol");
                    	} else {
                    		System.out.println("would make cycle from " + ncbirel.getEndNode().getProperty("name"));
                    		System.exit(0);// NEED TO EXIT BECAUSE THIS IS A PROBLEM
                    	}
                    }else if(fungirel != null){
                    	if (fungirel.getStartNode().getId() != fungirel.getEndNode().getId()) {
                    		fungirel.getStartNode().createRelationshipTo(fungirel.getEndNode(), RelType.PREFTAXCHILDOF);
                            Relationship newrel2 = fungirel.getStartNode().createRelationshipTo(fungirel.getEndNode(), RelType.TAXCHILDOF);
                            newrel2.setProperty("source", "ottol");
                        } else {
                            System.out.println("would make cycle from " + fungirel.getEndNode().getProperty("name"));
                            System.exit(0);// NEED TO EXIT BECAUSE THIS IS A PROBLEM
                        }
                    }
                    if (nRelsAdded % transaction_iter == 0)
                        System.out.println(nRelsAdded);
                }
            }
            
            tx.success();

        } finally {
            tx.finish();
        }
    }

    /**
     * ASSUMES THAT `makePreferredOTTOLRelationshipsConflicts()` HAS ALREADY BEEN RUN. This will walk through the tree, following the
     * preferred rels already identified by `makePreferredOTTOLRelationshipsConflicts()` and will create new preferred relationships
     * where there are no conflicts.
     */
    public void makePreferredOTTOLRelationshipsNOConflicts() {

//        TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
//                .relationships(RelType.TAXCHILDOF, Direction.INCOMING);

        // get the start point
        Node life = getLifeNode();
        System.out.println(life.getProperty("name"));

        Transaction tx = beginTx();
        addToPreferredIndexes(life, ALLTAXA);
        HashSet<Long> traveled = new HashSet<Long>();
        int nNewRels = 0;
        try {
            // walk out to the tips from the base of the tree
            for (Node n : TAXCHILDOF_TRAVERSAL.traverse(life).nodes()) {
                if (n.hasRelationship(Direction.INCOMING, RelType.TAXCHILDOF) == false) {

                    // when we hit a tip, start walking back
                    Node curNode = n;
                    while (curNode.hasRelationship(Direction.OUTGOING, RelType.TAXCHILDOF)) {
                        Node startNode = curNode;
                        if (traveled.contains((Long)startNode.getId())){
                        	break;
                        }else{
                        	traveled.add((Long)startNode.getId());
                        }
                        Node endNode = null;

                        // if the current node already has a preferred relationship, we will just follow it
                        if (startNode.hasRelationship(Direction.OUTGOING, RelType.PREFTAXCHILDOF)) {
                            Relationship prefRel = startNode.getSingleRelationship(RelType.PREFTAXCHILDOF, Direction.OUTGOING);

                            // make sure we don't get stuck in an infinite loop (should not happen, could do weird things to the graph)
                            if (prefRel.getStartNode().getId() == prefRel.getEndNode().getId()) {
                                System.out.println("pointing to itself " + prefRel + " " + prefRel.getStartNode().getId() + " " + prefRel.getEndNode().getId());
                                break;
                            }
                            
                            // prepare to move on
                            endNode = prefRel.getEndNode();

                        } else {

                            // if there is no preferred rel then they all point to the same end node; just follow the first non-looping relationship
                            for (Relationship rel : curNode.getRelationships(RelType.TAXCHILDOF, Direction.OUTGOING)) {
                                if (rel.getStartNode().getId() == rel.getEndNode().getId()) {
                                    System.out.println("pointing to itself " + rel + " " + rel.getStartNode().getId() + " " + rel.getEndNode().getId());
                                    break;
                                } else {
                                    endNode = rel.getEndNode();
                                    break;
                                }
                            }

                            // if we found a dead-end, die
                            if (endNode == null) {
                                System.out.println(curNode.getProperty("name"));
                                System.out.println("Strange, this relationship seems to be pointing at a nonexistent node. Quitting.");
                                System.exit(0);
                            }
                            
                            // create preferred relationships
                            curNode.createRelationshipTo(endNode, RelType.PREFTAXCHILDOF);
                            curNode.createRelationshipTo(endNode, RelType.TAXCHILDOF).setProperty("source", "ottol");
                            nNewRels += 1;
                        }

                        if (startNode == endNode) {
                            System.out.println(startNode);
                            System.out.println("The node seems to be pointing at itself. This is a problem. Quitting.");
                            System.exit(0);

                        // prepare for next iteration
                        } else {
                            curNode = endNode;
                            addToPreferredIndexes(startNode, ALLTAXA);
                        }
                    }
                }

                if (nNewRels % transaction_iter == 0) {
                    System.out.println(nNewRels);
   //                 tx.success();
   //                 tx.finish();
    //                tx = beginTx();
                }
            }
            tx.success();
        } finally {
            tx.finish();
        }
    }
    
    /*
     * New: This is meant to examine whether we can create equivalent statements about named nodes in the full graph
     * The procedure is meant to look at the node and the descentdents of that node and ask whether they are an equivalent
     * name. If so eventually you would want to add the taxa from the one taxonomy that are not in the new one to the new
     * one in the ottol relationships 
     */
    
    public void findEquivalentNamedNodes(String domsource){
		IndexHits<Node> hits = null;
		Node startnode = null;
    	try{
    		hits = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXONOMY_SOURCES).get("source", domsource);
    		startnode = hits.getSingle().getSingleRelationship(RelType.METADATAFOR, Direction.OUTGOING).getEndNode();//there should only be one source with that name
    	}finally{
    		hits.close();
    	}
    	Index<Node> taxNames = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME);
    	
    	for(Node curnode: TAXCHILDOF_TRAVERSAL.traverse(startnode).nodes()){
    		IndexHits<Node> nhits = null;
    		System.out.println((String)curnode.getProperty("name")+":"+curnode);
    		try{
    			nhits = taxNames.get("name", (String)curnode.getProperty("name"));
    			for (Node tnode: nhits){
    				System.out.println(tnode);
    			}
    		}finally{
    			nhits.close();
    		}
    	}
    }
    
    
    /**
     * 
     * @param outfile the outfile that will have the dumped ottol information as id\tparentid\tname
     */
    public void dumpPreferredOTTOLRelationships(String outfile) {
//    	 TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
//                 .relationships(RelType.PREFTAXCHILDOF, Direction.INCOMING);
         // get the start point
         Node life = getLifeNode();
         System.out.println(life.getProperty("name"));
         PrintWriter outFile;
 		 try {
             outFile = new PrintWriter(new FileWriter(outfile));
             outFile.write("uid\t|\tparent_uid\t|\tname\t|\trank\t|\tsource\t|\tsourceid\t|\tsourcepid\t|\t\n");
             for (Node n : PREFTAXCHILDOF_TRAVERSAL.traverse(life).nodes()) {
            	 outFile.write(String.valueOf(n.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()))+"\t|\t");
            	 if(n.hasRelationship(Direction.OUTGOING, RelType.PREFTAXCHILDOF)){
            		 Relationship tr = n.getSingleRelationship(RelType.PREFTAXCHILDOF, Direction.OUTGOING);
            		 Node p = tr.getEndNode();
            		 outFile.write(String.valueOf(p.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()))+"\t|\t");
            	 }else{
            		 outFile.write("\t|\t");
            	 }
            	 outFile.write(String.valueOf(n.getProperty("name"))+"\t|\t");
            	 if (n.hasProperty("rank"))
            		 outFile.write(String.valueOf(n.getProperty("rank"))+"\t|\t");
            	 else
            		 outFile.write("\t|\t");
            	 outFile.write(String.valueOf(n.getProperty("source"))+"\t|\t");
            	 outFile.write(String.valueOf(n.getProperty("sourceid"))+"\t|\t");
            	 if (n.hasProperty("sourcepid"))
            		 outFile.write(String.valueOf(n.getProperty("sourcepid"))+"\t|\t\n");
            	 else
            		 outFile.write("\t|\t\n");
             }
             outFile.close();
         } catch (IOException e) {
             e.printStackTrace();
         }
    }
    
    /**
     * 
     * @param outfile the outfile that will have the dumped ottol information as id\tparentid\tname
     */
    public void dumpPreferredOTTOLSynonymRelationships(String outfile) {
//    	 TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
//                 .relationships(RelType.PREFTAXCHILDOF, Direction.INCOMING);
         // get the start point
         Node life = getLifeNode();
         System.out.println(life.getProperty("name"));
         PrintWriter outFile;
 		 try {
             outFile = new PrintWriter(new FileWriter(outfile));
             outFile.write("uid\t|\tacceptednode_uid\t|\tsynonymname\t|\tnametype\t|\tsource\t|\t\n");
             for (Node n : PREFTAXCHILDOF_TRAVERSAL.traverse(life).nodes()) {
            	 if(n.hasRelationship(Direction.INCOMING, RelType.SYNONYMOF)){
            		 for(Relationship tr: n.getRelationships(Direction.INCOMING, RelType.SYNONYMOF)){
                		 Node p = tr.getStartNode();//synonym node
                		 outFile.write(String.valueOf(p.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()))+"\t|\t");
                		 outFile.write(String.valueOf(n.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()))+"\t|\t");
                		 outFile.write(String.valueOf(p.getProperty("name"))+"\t|\t");
                		 outFile.write(String.valueOf(p.getProperty("nametype"))+"\t|\t");
                		 outFile.write(String.valueOf(p.getProperty("source"))+"\t|\t\n");
            		 }
            	 }
             }
             outFile.close();
         } catch (IOException e) {
             e.printStackTrace();
         }
    }
    
    /**
     * Used to make a hierarchy for the taxonomic contexts (each ContextTreeNode contains links to immediate child ContextTreeNodes),
     * which is in turn used to do a pre-order traversal when building the contexts, which in turn ensures that the last written value
     * of each node's leastContext property contains the least inclusive context.
     * @author cody
     *
     */
    private class ContextTreeNode {
        
        TaxonomyContext context;
        ArrayList<ContextTreeNode> children;

        ContextTreeNode(TaxonomyContext context) {
            this.context = context;
            children = new ArrayList<ContextTreeNode>();
        }
        
        void addChild(ContextTreeNode child) {
            children.add(child);
        }
        
        TaxonomyContext getContext() {
            return context;
        }
        
        List<ContextTreeNode> getChildren() {
            return children;
        }
    }
    
    /**
     * Just prints the context hierarchy below `contextNode`. Used when building indexes to provide the user with confirmation that
     * everything is going according to plan.
     * @param contextNode
     * @param prefix
     */
    private void printContextTree(ContextTreeNode contextNode, String prefix) {

        prefix = prefix + "    ";
        for (ContextTreeNode childNode : contextNode.getChildren()) {
            System.out.println(prefix + childNode.getContext().getDescription().name);
            printContextTree(childNode, prefix);
        }

    }
    
    /**
     * Builds the context-specific indices that are used for more efficient access to sub-regions of the taxonomy, as defined in the
     * ContextDescription enum. Context-specific indices are all just subsets of the preferred taxon indices (by name, synonym, and
     * both) for the entire graph.
     *
     * This method first uses the taxonomy defined by the database to determine the hierarchical nesting structure of the contexts,
     * recording this hierarchy in the form of pointer-links among a group of ContextTreeNode objects. This hierarchy is then passed
     * to a recursive function that builds the indexes, which ensures that they are built pre-order, and thus that each taxon node's
     * leastIndex property (written to each traversed node during the creation of each context's indices) always reflects the last
     * index built (thus, the least inclusive index).
     * 
     * @return root of the ContextTreeNode hierarchy
     */
    public void makeContexts() {
        
        // make map of ContextTreeNode objects for all taxonomic contexts, indexed by root node name
        HashMap<String, ContextTreeNode> contextNodesByRootName = new HashMap<String, ContextTreeNode>();
        for (ContextDescription cd : ContextDescription.values()) {
            contextNodesByRootName.put(cd.licaNodeName, new ContextTreeNode(new TaxonomyContext(cd, this)));
        }

        TraversalDescription prefTaxParentOfTraversal = Traversal.description().depthFirst().
                relationships(RelType.PREFTAXCHILDOF, Direction.OUTGOING);
        
        // for each ContextTreeNode (i.e. each context)
        for (Entry<String, ContextTreeNode> entry: contextNodesByRootName.entrySet()) {
            
            String childName = entry.getKey();
            ContextTreeNode contextNode = entry.getValue();
                        
            // traverse back up the taxonomy tree from the root of this context toward life
            for (Node parentNode : prefTaxParentOfTraversal.traverse(contextNode.context.getRootNode()).nodes()) {

                // if/when we find a more inclusive (i.e. parent) context
                String parentName = String.valueOf(parentNode.getProperty("name"));
                if (contextNodesByRootName.containsKey(parentName) && (parentName.equals(childName) == false)) {

                    System.out.println("Adding " + childName + " as child of " + parentName);
                    
                    // add this link in the contextNode hierarchy and move to the next contextNode
                    ContextTreeNode parentContextNode = contextNodesByRootName.get(parentName);
                    parentContextNode.addChild(contextNode);
                    break;

                }
            }
        }
        
        // get the root of the ContextTreeNode tree (i.e. most inclusive context)
        ContextTreeNode contextHierarchyRoot = contextNodesByRootName.get(LIFE_NODE_NAME);
    
        System.out.println("\nHierarchy for contexts (note: paraphyletic groups do not have their own contexts):");
        printContextTree(contextHierarchyRoot, "");
        System.out.println("");

        // make the contexts!
        makeContextsRecursive(contextHierarchyRoot);
    }
    
    class isSpecificEvaluator implements Evaluator {
		
		@Override
		public Evaluation evaluate(Path inPath) {

//			System.out.println("traversing " + inPath.endNode().getProperty("name"));
			
			String rank = "";
			if (inPath.endNode().hasProperty("rank")) {
				rank = String.valueOf(inPath.endNode().getProperty("rank"));
			}
			
//			System.out.println("rank = " + rank);
			
    		if (isSpecific(rank)) {
    			return Evaluation.INCLUDE_AND_CONTINUE;
    		} else {
    			return Evaluation.EXCLUDE_AND_CONTINUE;
    		}	
		}
    }
    
    /**
     * Make an index recording all the species + infraspecific taxa within each genus.
     */
    public void makeGenericIndexes() {
    	
    	int genCount = 0;
    	int genReportFreq = 100000;

        TraversalDescription prefTaxChildOfTraversal = Traversal.description().
                relationships(RelType.PREFTAXCHILDOF, Direction.INCOMING);

        Transaction tx = beginTx();
    	try {
    		for (Node genus : ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_RANK).get("rank", "genus")) {
//        		System.out.println("starting on genus: " + genus.getProperty("name"));
	        	String gid = String.valueOf(genus.getProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName()));
	        	for (Node sp : prefTaxChildOfTraversal.evaluator(new isSpecificEvaluator()).traverse(genus).nodes()) {
//	        		System.out.println("returned " + sp.getProperty("name"));
	        		prefSpeciesByGenus.add(sp, "genus_uid", gid);
	        	}
	        	
	        	if (genCount % genReportFreq == 0) {
	        		System.out.println(genCount / 1000 + "K");
	        	}
	        	genCount++;
    		}
    		tx.success();
    	} finally {
    		tx.finish();
    	}
    }
    
    /**
     * Uses preorder recursion for building indexes, so that least inclusive indexes are built last.
     * @param contextNode
     */
    private void makeContextsRecursive(ContextTreeNode contextNode) {

        TaxonomyContext context = contextNode.getContext();
        Node contextRootNode = context.getRootNode();
        int i = 0;
        
        Transaction tx = beginTx();
        if (contextRootNode.getProperty("name").equals(LIFE_NODE_NAME) == false) {

            System.out.println("making indices for " + contextRootNode.getProperty("name"));

            for (Node n : PREFTAXCHILDOF_TRAVERSAL.traverse(contextRootNode).nodes()) {
                addToPreferredIndexes(n, context);
                
                i++;
                if (i % 100000 == 0)
                    System.out.println(i / 1000 + "K");
            }
        }
        tx.success();
        tx.finish();
        
        // now move on to all children
        for (ContextTreeNode childNode : contextNode.getChildren())
            makeContextsRecursive(childNode);

    }
    
    /**
     * Just adds `node` under its name and its synonyms to the corresponding node indexes for `context`. Called by the
     * makeContextsRecursive method to build the index for a given context as it traverses the nodes within it.
     * 
     * THIS METHOD ASSUMES (I.E. REQUIRES) THAT IT IS BEING CALLED FROM WITHIN A TRANSACTION.
     * 
     * @param node
     */
    public void addToPreferredIndexes(Node node, TaxonomyContext context) {

        Index<Node> nameIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME);
        Index<Node> synonymIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_SYNONYM);
        Index<Node> nameOrSynonymIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_OR_SYNONYM);

        Index<Node> rankIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_RANK);
        
        // species and subspecific ranks
        Index<Node> speciesNameIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_SPECIES);

        // genera only
        Index<Node> genusNameIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_GENERA);

        // higher taxa
        Index<Node> higherNameIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_HIGHER);
        Index<Node> higherNameOrSynonymIndex = context.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_OR_SYNONYM_HIGHER);
        
        // update the leastcontext property (notion of "least" assumes this is being called by recursive context-building)
        node.setProperty("leastcontext", context.getDescription().toString());

        // add the taxon node under its own name
        nameIndex.add(node, "name", node.getProperty("name"));
        nameOrSynonymIndex.add(node, "name", node.getProperty("name"));

        String rank = "";
        if (node.hasProperty("rank")) {
        	rank = String.valueOf(node.getProperty("rank")).toLowerCase();
        	rankIndex.add(node, "rank", rank);
        }
        
        // add to the rank-specific indexes
        if (isSpecific(rank)) {
        	speciesNameIndex.add(node, "name", node.getProperty("name"));
        } else if (rank.equals("genus")) {
        	genusNameIndex.add(node, "name", node.getProperty("name"));
        	higherNameIndex.add(node, "name", node.getProperty("name"));
        	higherNameOrSynonymIndex.add(node, "name", node.getProperty("name"));
        } else {
        	higherNameIndex.add(node, "name", node.getProperty("name"));
        	higherNameOrSynonymIndex.add(node, "name", node.getProperty("name"));
        }
        
        // add the taxon node under all its synonym names
        for (Node sn : Traversal.description()
                .breadthFirst()
                .relationships(RelType.SYNONYMOF,Direction.INCOMING )
                .traverse(node).nodes()) {
            synonymIndex.add(node, "name", sn.getProperty("name"));
            nameOrSynonymIndex.add(node, "name", sn.getProperty("name"));
            if (!isSpecific(rank)) {
            	higherNameOrSynonymIndex.add(node, "name", sn.getProperty("name"));
            }
        }
    }    

    /**
    -     * Originally a one-time solution to a specific problem that has now been fixed. Leaving it in case it is useful.
    -     * @param node
    -     * @param context
    -     */
    @Deprecated
    public void addToPreferredIndexesAtomicTX(Node node, TaxonomyContext context) {
    	Transaction tx = beginTx();
    	addToPreferredIndexes(node, context);
    	tx.success();
    	tx.finish();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
//        System.out.println("unit testing taxonomy explorer");
//        String DB_PATH ="/home/smitty/Dropbox/projects/AVATOL/graphtests/neo4j-community-1.8.M02/data/graph.db";
//        GraphDatabaseAgent gdb = new GraphDatabaseAgent(DB_PATH);
//        TaxonomyCombiner a = new TaxonomyCombiner(gdb);
        
        System.out.println("No tests implemented...");
    }

}
