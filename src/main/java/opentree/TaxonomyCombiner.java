package opentree;

import java.util.ArrayList;

import opentree.tnrs.MultipleHitsException;
import opentree.tnrs.TNRSQuery;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class TaxonomyCombiner extends Taxonomy {

    int transaction_iter = 10000;

    public TaxonomyCombiner(GraphDatabaseAgent t) {
        super(t);
    }

    /**
     * Attempt to find cycles (should be conflicting taxonomies) below the node matched by `name`.
     */
    public void findTaxonomyCycles(String name) {

        Node firstNode = null;
        try {
            firstNode = (new TNRSQuery(this)).getExactMatches(name).getSingleMatch().getMatchedNode();
        } catch (MultipleHitsException e) {
            e.printStackTrace();
        }

        TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
                .relationships(RelTypes.TAXCHILDOF, Direction.INCOMING);
        System.out.println(firstNode.getProperty("name"));
        ArrayList<Node> conflictingnodes = new ArrayList<Node>();
        for (Node friendnode : CHILDOF_TRAVERSAL.traverse(firstNode).nodes()) {
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

        TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
                .relationships(RelTypes.TAXCHILDOF, Direction.INCOMING);

        // get start node
        Node life = getLifeNode();
        System.out.println(life.getProperty("name"));

        Transaction tx = beginTx();
        int nRelsAdded = 0;
        try {
            
            // walk all nodes looking for conflicts
            for (Node friendnode : CHILDOF_TRAVERSAL.traverse(life).nodes()) {
                boolean foundConflict = false;
                String endNodeName = "";
                Relationship ncbirel = null;

                for (Relationship rel : friendnode.getRelationships(Direction.OUTGOING)) {
                    if (rel.getEndNode() == rel.getStartNode()) {
                        System.out.println("CYCLE!" + rel.getEndNode() + " " + rel.getStartNode() + " " + rel);
                        continue;

                    } else {
                        if (endNodeName == "")
                            endNodeName = (String) rel.getEndNode().getProperty("name");
                        
                        if ((String) rel.getEndNode().getProperty("name") != endNodeName)
                            foundConflict = true;

                        if (((String) rel.getProperty("source")).compareTo("ncbi") == 0)
                            ncbirel = rel;
                    }
                }
                if (foundConflict && ncbirel != null) {
                    nRelsAdded += 1;
                    // System.out.println("would make one from "+ncbirel.getStartNode().getProperty("name")+" "+ncbirel.getEndNode().getProperty("name"));
                    if (ncbirel.getStartNode().getId() != ncbirel.getEndNode().getId()) {
                        ncbirel.getStartNode().createRelationshipTo(ncbirel.getEndNode(), RelTypes.PREFTAXCHILDOF);
                        Relationship newrel2 = ncbirel.getStartNode().createRelationshipTo(ncbirel.getEndNode(), RelTypes.TAXCHILDOF);
                        newrel2.setProperty("source", "ottol");
                    } else {
                        System.out.println("would make cycle from " + ncbirel.getEndNode().getProperty("name"));
                        System.exit(0);// NEED TO EXIT BECAUSE THIS IS A PROBLEM
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
     * ASSUMES THAT `makePreferredOTTOLRelationshipsConflicts()` HAS ALREADY BEEN RUN. This will walks through the tree, following the
     * preferred rels already identified by `makePreferredOTTOLRelationshipsConflicts()` and will create new preferred relationships
     * where there are no conflicts.
     */
    public void makePreferredOTTOLRelationshipsNOConflicts() {

        TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
                .relationships(RelTypes.TAXCHILDOF, Direction.INCOMING);

        // get the start point
        Node life = getLifeNode();
        System.out.println(life.getProperty("name"));

        int nNewRels = 0;
        Transaction tx = beginTx();
        try {
            // walk out to the tips from the base of the tree
            for (Node n : CHILDOF_TRAVERSAL.traverse(life).nodes()) {
                if (n.hasRelationship(Direction.INCOMING) == false) {

                    // when we hit a tip, start walking back
                    Node curNode = n;
                    while (curNode.hasRelationship(Direction.OUTGOING)) {
                        Node startNode = curNode;
                        Node endNode = null;

                        // if the current node already has a preferred relationship, we will just follow it
                        if (startNode.hasRelationship(Direction.OUTGOING, RelTypes.PREFTAXCHILDOF)) {
                            Relationship prefRel = startNode.getSingleRelationship(RelTypes.PREFTAXCHILDOF, Direction.OUTGOING);

                            // make sure we don't get stuck in an infinite loop (should not happen, could do weird things to the graph)
                            if (prefRel.getStartNode().getId() == prefRel.getEndNode().getId()) {
                                System.out.println("pointing to itself " + prefRel + " " + prefRel.getStartNode().getId() + " " + prefRel.getEndNode().getId());
                                break;
                            }
                            
                            // prepare to move on
                            endNode = prefRel.getEndNode();

                        // if there is no preferred rel then they are all the same, we will just pick one
                        } else {

                            // follow the first non-looping relationship
                            for (Relationship rel : curNode.getRelationships(RelTypes.TAXCHILDOF, Direction.OUTGOING)) {
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
                                System.exit(0);
                            }
                            
                            // create preferred relationships
                            curNode.createRelationshipTo(endNode, RelTypes.PREFTAXCHILDOF);
                            curNode.createRelationshipTo(endNode, RelTypes.TAXCHILDOF).setProperty("source", "ottol");
                            nNewRels += 1;
                        }

                        if (startNode == curNode) {
                            System.out.println(startNode);
                            System.out.println("THERE IS A PROBLEM");
                            System.exit(0);

                        // prepare for next iteration
                        } else {
                            curNode = endNode;
                            addToPreferredIndexes(startNode);
                        }
                    }
                }

                if (nNewRels % transaction_iter == 0) {
                    System.out.println(nNewRels);
                    tx.success();
                    tx.finish();
                    tx = beginTx();
                }
            }
            tx.success();
        } finally {
            tx.finish();
        }
    }

    /**
     * Just add the node `n` to the prefTaxNodes index and its synonyms to prefSynNodes.
     * @param node
     */
    private void addToPreferredIndexes(Node node) {

        // add the node itself
        NodeIndex.PREFERRED_TAXON_BY_NAME.add(node, "name", node.getProperty("name"));

        // add synonyms
        for (Node sn : Traversal.description().breadthFirst()
                .relationships(RelTypes.SYNONYMOF,Direction.INCOMING ).traverse(node).nodes())
            NodeIndex.PREFERRED_SYNONYM_BY_NAME.add(node, "name", sn.getProperty("name"));
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
