package opentree;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import opentree.tnrs.MultipleHitsException;
import opentree.tnrs.TNRSQuery;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class TaxonomySynthesizer extends Taxonomy {

    int transaction_iter = 10000;

    private static final TraversalDescription PREFTAXCHILDOF_TRAVERSAL = Traversal.description().breadthFirst().
            relationships(RelType.PREFTAXCHILDOF, Direction.INCOMING);

    private static final TraversalDescription TAXCHILDOF_TRAVERSAL = Traversal.description().breadthFirst().
            relationships(RelType.TAXCHILDOF, Direction.INCOMING);
    
    private static final TraversalDescription TAXCHILDOF_OUTGOING_TRAVERSAL = Traversal.description().breadthFirst().
            relationships(RelType.TAXCHILDOF, Direction.OUTGOING);
    
    public TaxonomySynthesizer(GraphDatabaseAgent t) {
        super(t);
    }

    /**
     * Dump all the name data in a format consistent with Phylotastic treestore requirements
     */
    public void makeOTTOLNameDump(Node rootNode, String outFileName) {
        
        System.out.println("Test: writing names from " + rootNode.getProperty("name") + " to " + outFileName);
        
        for (Node n : PREFTAXCHILDOF_TRAVERSAL.traverse(rootNode).nodes()) {
            System.out.println("name: " + n.getProperty("name"));
            
            // source name : source UID
            HashMap<String, String> sourceIdMap = new HashMap<String, String>();
            
/*            boolean hasChildren = false;
            for (Relationship l : TAXCHILDOF_TRAVERSAL.evaluator(Evaluators.toDepth(1)).traverse(n).relationships()) {
                hasChildren = true;
                // this taxon has children, so we can get info from of its incoming relationships

                String sourceName = "";
                if (l.hasProperty("source"))
                    System.out.println(sourceName);
                    sourceName = String.valueOf(l.getProperty("source"));

                String taxUId = "";
                if (l.hasProperty("parentid"))
                    taxUId = String.valueOf(l.getProperty("parentid")); */
            
            for (Relationship l : TAXCHILDOF_OUTGOING_TRAVERSAL.evaluator(Evaluators.toDepth(1)).traverse(n).relationships()) {
                String sourceName = "";
                if (l.hasProperty("source")) {
                    sourceName = String.valueOf(l.getProperty("source"));
                    System.out.println(sourceName);
                }

                String taxUId = "";
                if (l.hasProperty("childid")) {
                    taxUId = String.valueOf(l.getProperty("childid"));
                }
                
                if (sourceName != "") {
                    sourceIdMap.put(sourceName, taxUId);
                }
            }
            
/*            if (!hasChildren) {
                // this taxon has no children, so we need to check its outgoing relationships
                
                for (Relationship l : TAXCHILDOF_OUTGOING_TRAVERSAL.evaluator(Evaluators.toDepth(1)).traverse(n).relationships()) {
                    String sourceName = "";
                    if (l.hasProperty("source"))
                        sourceName = String.valueOf(l.getProperty("source"));

                    String taxUId = "";
                    if (l.hasProperty("childid"))
                        taxUId = String.valueOf(l.getProperty("childid"));
                    
                    if (sourceName != "")
                        sourceIdMap.put(sourceName, taxUId);
                }
            } */
            
            for (Entry<String, String> info : sourceIdMap.entrySet()) {
                String sourceName = info.getKey();
                String taxUId = info.getValue();
                System.out.println("\tsource: " + sourceName + ", uid: " + taxUId);
            }
        }
    }
    
    /**
     * Attempt to find cycles (should be conflicting taxonomies) below the node matched by `name`.
     */
    public void findTaxonomyCycles(String name) {

        Node firstNode = null;
        try {
            firstNode = (new TNRSQuery(this)).matchExact(name).getSingleMatch().getMatchedNode();
        } catch (MultipleHitsException e) {
            e.printStackTrace();
        }

//        TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
//                .relationships(RelType.TAXCHILDOF, Direction.INCOMING);
        System.out.println(firstNode.getProperty("name"));
        ArrayList<Node> conflictingnodes = new ArrayList<Node>();
        for (Node friendnode : TAXCHILDOF_TRAVERSAL.traverse(firstNode).nodes()) {
            int count = 0;
            boolean conflict = false;
            String endNode = "";
            for (Relationship rel : friendnode.getRelationships(Direction.OUTGOING)) {
                if (endNode == "")
                    endNode = (String) rel.getEndNode().getProperty("name");
                if ((String) rel.getEndNode().getProperty("name") != endNode) {
                    conflict = true;
                }
                count += 1;
            }
            if (count > 1 && conflict) {
                conflictingnodes.add(friendnode);
            }
        }
        System.out.println(conflictingnodes.size());
        for (int i = 0; i < conflictingnodes.size(); i++) {
            System.out.println(conflictingnodes.get(i).getProperty("name"));
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
    		hits = ALLTAXA.getNodeIndex(NodeIndexDescription.TAX_SOURCES).get("source", domsource);
    		startnode = hits.getSingle().getSingleRelationship(RelType.METADATAFOR, Direction.OUTGOING).getEndNode();//there should only be one source with that name
    	}finally{
    		hits.close();
    	}
    	Index<Node> taxNames = ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME);
    	
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
     * Builds the context-specific indices that are used for more efficient access to sub-regions of the taxonomy, as defined in the
     * ContextDescription enum. Context-specific indices are all just subsets of the preferred taxon indices (by name, synonym, and
     * both) for the entire graph.
     *
     * This method first uses the taxonomy to determine the hierarchical nesting structure of the contexts, recording this hierarchy
     * in the form of pointer-links among a group of ContextTreeNode objects. This hierarchy is then passed to a recursive function
     * that builds the indexes, which ensures that they are built outside-in, and thus that each taxon node's leastIndex property
     * (written to each traversed node during the creation of each context's indices) always reflects the least inclusive index name.
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
    
    /**
     * Just prints the hierarchy below `contextNode`
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
     * Uses recursive method for building indexes, starting with most inclusive and working in, so that least inclusive indexes are built last.
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
                    System.out.println(i);
            }
        }
        tx.success();
        tx.finish();
        
        // now move on to all children
        for (ContextTreeNode childNode : contextNode.getChildren())
            makeContextsRecursive(childNode);

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
             for (Node n : PREFTAXCHILDOF_TRAVERSAL.traverse(life).nodes()) {
            	 if(n.hasRelationship(Direction.OUTGOING, RelType.PREFTAXCHILDOF)){
            		 Relationship tr = n.getSingleRelationship(RelType.PREFTAXCHILDOF, Direction.OUTGOING);
            		 Node p = tr.getEndNode();
            		 String source = "";
            		 for (Relationship r: n.getRelationships(RelType.TAXCHILDOF, Direction.OUTGOING)){
            			 source = (String)r.getProperty("source");
            		 }
            		 outFile.write(n.getId()+"\t"+p.getId()+"\t"+n.getProperty("name")+"\t"+source+"\n");
            	 }else{
            		 outFile.write(n.getId()+"\t"+"\t"+n.getProperty("name")+"\tncbi\n");
            	 }
             }
             outFile.close();
         } catch (IOException e) {
             e.printStackTrace();
         }
    }
    
    /**
     * Just adds `node` under its name and its synonyms. THIS METHOD ASSUMES (I.E. REQUIRES) THAT IT IS BEING CALLED FROM WITHIN A TRANSACTION.
     * to the corresponding indices for `context`.
     * @param node
     */
    public void addToPreferredIndexes(Node node, TaxonomyContext context) {

        Index<Node> prefTaxNodesByName = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME);
        Index<Node> prefTaxNodesBySynonym = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_SYNONYM);
        Index<Node> prefTaxNodesByNameOrSynonym = context.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME_OR_SYNONYM);
        
        // update the leastcontext property (notion of "least" assumes this is being called by recursive context-building)
        node.setProperty("leastcontext", context.getDescription().toString());

        // add the taxon node under its own name
        prefTaxNodesByName.add(node, "name", node.getProperty("name"));
        prefTaxNodesByNameOrSynonym.add(node, "name", node.getProperty("name"));

        // add the taxon node under all its synonym names
        for (Node sn : Traversal.description()
                .breadthFirst()
                .relationships(RelType.SYNONYMOF,Direction.INCOMING )
                .traverse(node).nodes()) {
            prefTaxNodesBySynonym.add(node, "name", sn.getProperty("name"));
            prefTaxNodesByNameOrSynonym.add(node, "name", sn.getProperty("name"));
        }
    }    

    /**
     * Originally a one-time solution to a specific problem that has now been fixed. Leaving it in case it is useful.
     * @param node
     * @param context
     */
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
