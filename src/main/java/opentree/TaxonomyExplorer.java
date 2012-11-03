package opentree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jade.tree.JadeNode;
import jade.tree.JadeTree;
import jade.tree.TreeReader;
import opentree.TaxonomyBase.RelTypes;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;

@SuppressWarnings("deprecation")
public class TaxonomyExplorer extends TaxonomyBase{
	private SpeciesEvaluator se;
	private ChildNumberEvaluator cne;
	int transaction_iter = 10000;
	

	public TaxonomyExplorer(){
		cne = new ChildNumberEvaluator();
		cne.setChildThreshold(100);
		se = new SpeciesEvaluator();
	}
	
	public TaxonomyExplorer(String graphname){
		graphDb = new EmbeddedGraphDatabase( graphname );
		taxNodeIndex = graphDb.index().forNodes("taxNodes");
		synNodeIndex = graphDb.index().forNodes("synNodes");
	}
  
    public void setEmbeddedDB(String graphname){
        graphDb = new EmbeddedGraphDatabase( graphname ) ;
        taxNodeIndex = graphDb.index().forNodes( "taxNodes" );
		synNodeIndex = graphDb.index().forNodes("synNodes");
    }

    public void setDbService(GraphDatabaseService graphDb) {
        taxNodeIndex = graphDb.index().forNodes("taxNodes");
		synNodeIndex = graphDb.index().forNodes("synNodes");
    }
	
    public Node getLifeNode() {
        return taxNodeIndex.get("name", LIFE_NODE_NAME).getSingle();
    }
    
    /**
     * Checks taxNodeIndex for `name` and returns null (if the name is not found) or an IndexHits<Node> object containing the found nodes. Helper function
     * primarily written to avoid forgetting to call hits.close(). Somewhat disconcerting that we return `hits` after it has been closed, but it works. Would be
     * helpful to validate that this is expected behavior.
     * 
     * @return
     */
    public IndexHits<Node> findTaxNodesByName(final String name) {
        IndexHits<Node> hits = taxNodeIndex.get("name", name); // TODO: change to preferred nodes when this is functional
        hits.close();
        return hits;
    }

    /**
     * Checks synNodeIndex for `name` and returns null (if the name is not found) or an IndexHits<Node> object containing the found nodes. Helper function
     * primarily written to avoid forgetting to call hits.close(). Somewhat disconcerting that we return `hits` after it has been closed, but it works. Would be
     * helpful to validate that this is expected behavior.
     * 
     * @return
     */
    public IndexHits<Node> findSynNodesByName(final String name) {
        IndexHits<Node> hits = synNodeIndex.get("name", name); // TODO: change to preferred nodes when this is functional
        hits.close();
        return hits;
    }

    /**
     * Checks taxNodeIndex for `name` and returns all hits. Uses fuzzy searching. Returns null (if the name is not found) or an IndexHits<Node> object
     * containing the found nodes. Helper function primarily written to avoid forgetting to call hits.close(). Somewhat disconcerting that we return `hits`
     * after it has been closed, but it works. Would be helpful to validate that this is expected behavior.
     * 
     * @return
     */
    public IndexHits<Node> findTaxNodesByNameFuzzy(final String name, float minIdentity) {
        IndexHits<Node> hits = taxNodeIndex.query(new FuzzyQuery(new Term("name", name), minIdentity)); // TODO: change to preferred nodes when this is functional
        hits.close();
        return hits;
    }

    /**
     * A wrapper that uses the default minimum identity score for fuzzy matches
     * @param name
     * @return
     */
    public IndexHits<Node> findTaxNodesByNameFuzzy(final String name) {
        return findTaxNodesByNameFuzzy(name, DEFAULT_FUZZYMATCH_IDENTITY);
    }
    
    /**
     * Checks synNodeIndex for `name` and returns all hits. Uses fuzzy searching. Returns null (if the name is not found) or an IndexHits<Node> object
     * containing the found nodes. Helper function primarily written to avoid forgetting to call hits.close(). Somewhat disconcerting that we return `hits`
     * after it has been closed, but it works. Would be helpful to validate that this is expected behavior.
     * 
     * @return
     */
    public IndexHits<Node> findSynNodesByNameFuzzy(final String name, float minIdentity) {
     // TODO: change to use preferred nodes only when this becomes functional
        IndexHits<Node> hits = synNodeIndex.query(new FuzzyQuery(new Term("name", name), minIdentity));
        hits.close();
        return hits;
    }
    
    /**
     * A wrapper that uses the default minimum identity score for fuzzy matches
     * @param name
     * @return
     */
    public IndexHits<Node> findSynNodesByNameFuzzy(final String name) {
        return findSynNodesByNameFuzzy(name, DEFAULT_FUZZYMATCH_IDENTITY);
    }
    
    public Node getTaxNodeForSynNode(Node synonymNode) {
    	
        TraversalDescription synonymTraversal = Traversal.description()
		        .relationships(RelTypes.SYNONYMOF, Direction.OUTGOING);

        Node taxNode = null;
        for(Node tn : synonymTraversal.traverse(synonymNode).nodes())
    	 	taxNode = tn;

        return taxNode;
    }
	
    public int getInternodalDistance(Node n1, Node n2, RelationshipType relType) {
        
        TraversalDescription synonymTraversal = Traversal.description()
                .relationships(RelTypes.SYNONYMOF, Direction.OUTGOING);
        
        return 0;
    }
    
	/*
	 * Given a Taxonomic name (as name), this will attempt to find cycles which should be conflicting taxonomies
	 */
	public void findTaxonomyCycles(String name) {
	    IndexHits<Node> foundNodes = findTaxNodesByName(name);
        Node firstNode = null;
        if (foundNodes.size() < 1){
            System.out.println("name '" + name + "' not found. quitting.");
            return;
        } else if (foundNodes.size() > 1) {
            System.out.println("more than one node found for name '" + name + "'not sure how to deal with this. quitting");
        } else {
            for (Node n : foundNodes)
                firstNode = n;
        }
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
		        .relationships( RelTypes.TAXCHILDOF,Direction.INCOMING );
		System.out.println(firstNode.getProperty("name"));
		ArrayList<Node> conflictingnodes = new ArrayList<Node>();
		for(Node friendnode : CHILDOF_TRAVERSAL.traverse(firstNode).nodes()){
			int count = 0;
			boolean conflict = false;
			String endNode = "";
			for(Relationship rel : friendnode.getRelationships(Direction.OUTGOING)){
				if (endNode == "")
					endNode = (String) rel.getEndNode().getProperty("name");
				if ((String)rel.getEndNode().getProperty("name") != endNode){
					conflict = true;
				}
				count += 1;
			}
			if (count > 1 && conflict){
				conflictingnodes.add(friendnode);
			}
		}
		System.out.println(conflictingnodes.size());
		for(int i=0;i<conflictingnodes.size();i++){
			System.out.println(conflictingnodes.get(i).getProperty("name"));
		}
	}	
	
	/* TODO: THIS SHOULD BE REPLACED BY TNRS CHECK TREE FUNCTION
    public void checkNamesInTree(String treefilename,String focalgroup){
        PathFinder <Path> pf = GraphAlgoFactory.shortestPath(Traversal.pathExpanderForTypes(RelTypes.TAXCHILDOF, Direction.OUTGOING), 100);
        IndexHits<Node> foundNodes = findTaxNodeByName(focalgroup);
        Node focalnode = null;
        if (foundNodes.size() < 1){
            System.out.println("name '" + focalgroup + "' not found. quitting.");
            return;
        } else if (foundNodes.size() > 1) {
            System.out.println("more than one node found for name '" + focalgroup + "'not sure how to deal with this. quitting");
        } else {
            for (Node n : foundNodes)
                focalnode = n;
        } 
        String ts = "";
        try{
            BufferedReader br = new BufferedReader(new FileReader(treefilename));
            ts = br.readLine();
            br.close();
        }catch(IOException ioe){
            System.out.println("problem reading tree");
        }
        TreeReader tr = new TreeReader();
        JadeTree jt = tr.readTree(ts);
        System.out.println("tree read");
        for(int i=0;i<jt.getExternalNodeCount();i++){
            IndexHits<Node> hits = taxNodeIndex.get("name", jt.getExternalNode(i).getName().replace("_"," "));
            int numh = hits.size();
            if (numh == 0)
                System.out.println(jt.getExternalNode(i).getName()+" gets NO hits");
            if (numh > 1){
                System.out.println(jt.getExternalNode(i).getName()+" gets "+numh+" hits");
                for(Node tnode : hits){
                    Path tpath = pf.findSinglePath(tnode, focalnode);
                }
            }
            hits.close();
        }
    } */
	
	/**
	 * Right now this walks from the life node through the entire graph.
	 * When there are conflicts, this prefers the NCBI branches.
	 */
	public void makePreferredOTTOLRelationshipsConflicts(){
		Transaction tx;
		String name = "life";
		IndexHits<Node> foundNodes = findTaxNodesByName(name);
        Node firstNode = null;
        if (foundNodes.size() < 1){
            System.out.println("name '" + name + "' not found. quitting.");
            return;
        } else if (foundNodes.size() > 1) {
            System.out.println("more than one node found for name '" + name + "'not sure how to deal with this. quitting");
        } else {
            for (Node n : foundNodes)
                firstNode = n;
        }
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
				.relationships( RelTypes.TAXCHILDOF,Direction.INCOMING );
		System.out.println(firstNode.getProperty("name"));
		int count = 0;
		tx = graphDb.beginTx();
		try{
			for(Node friendnode : CHILDOF_TRAVERSAL.traverse(firstNode).nodes()){
				boolean conflict = false;
				String endNode = "";
				Relationship ncbirel = null;
				Relationship ottolrel = null;
				for(Relationship rel : friendnode.getRelationships(Direction.OUTGOING)){
					if (rel.getEndNode() == rel.getStartNode()){
						continue;
					}else{
						if (endNode == "")
							endNode = (String) rel.getEndNode().getProperty("name");
						if ((String)rel.getEndNode().getProperty("name") != endNode){
							conflict = true;
						}
						if(((String)rel.getProperty("source")).compareTo("ncbi")==0)
							ncbirel = rel;
					}
				}
				if (conflict && ncbirel != null){
					count += 1;
//					System.out.println("would make one from "+ncbirel.getStartNode().getProperty("name")+" "+ncbirel.getEndNode().getProperty("name"));
					if(ncbirel.getStartNode()!=ncbirel.getEndNode()){
						ncbirel.getStartNode().createRelationshipTo(ncbirel.getEndNode(), RelTypes.PREFTAXCHILDOF);
						Relationship newrel2 = ncbirel.getStartNode().createRelationshipTo(ncbirel.getEndNode(), RelTypes.TAXCHILDOF);
						newrel2.setProperty("source", "ottol");
					}else{
						System.out.println("would make cycle from "+ncbirel.getEndNode().getProperty("name"));
					}
					
				}
				if(count % transaction_iter == 0)
					System.out.println(count);
			}
			tx.success();
		}finally{
			tx.finish();
		}
	}
	
	public void makePreferredOTTOLRelationshipsNOConflicts(){
		Transaction tx;
		String name = "life";
		IndexHits<Node> foundNodes = findTaxNodesByName(name);
        Node firstNode = null;
        if (foundNodes.size() < 1){
            System.out.println("name '" + name + "' not found. quitting.");
            return;
        } else if (foundNodes.size() > 1) {
            System.out.println("more than one node found for name '" + name + "'not sure how to deal with this. quitting");
        } else {
            for (Node n : foundNodes)
                firstNode = n;
        }
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
				.relationships( RelTypes.TAXCHILDOF,Direction.INCOMING );
		System.out.println(firstNode.getProperty("name"));
		int count = 0;
		tx = graphDb.beginTx();
		try{
			for(Node friendnode : CHILDOF_TRAVERSAL.traverse(firstNode).nodes()){
				if(friendnode.hasRelationship(Direction.INCOMING) == false){//is tip
					Node curnode = friendnode;
					while(curnode.hasRelationship(Direction.OUTGOING)){
//						System.out.println(curnode.getProperty("name"));
						if(curnode.hasRelationship(Direction.OUTGOING,RelTypes.PREFTAXCHILDOF)){
							Relationship trel = curnode.getSingleRelationship(RelTypes.PREFTAXCHILDOF, Direction.OUTGOING);
//							System.out.println(trel.getStartNode().getId()+" "+trel.getEndNode().getId());
							if(trel.getStartNode().getId() == trel.getEndNode().getId()){
								break;
							}
							curnode = trel.getEndNode();
						}else{
							Node endnode = null;
							for(Relationship trel: curnode.getRelationships(RelTypes.TAXCHILDOF,Direction.OUTGOING)){
//								System.out.println(trel.getProperty("source")+" "+trel.getStartNode().getProperty("name")+" "+trel.getEndNode().getProperty("name"));
								if (trel.getEndNode() == curnode){
									continue;
								}else{
									endnode = trel.getEndNode();
									break;
								}
							}
							if(endnode == null){
								System.out.println(curnode.getProperty("name"));
								System.exit(0);
							}
							Relationship newrel = curnode.createRelationshipTo(endnode, RelTypes.PREFTAXCHILDOF);
							Relationship newrel2 = curnode.createRelationshipTo(endnode, RelTypes.TAXCHILDOF);
							newrel2.setProperty("source", "ottol");
							curnode = endnode;
							count += 1;
						}
					}
				}
				if(count % transaction_iter == 0){
					System.out.println(count);
					tx.success();
					tx.finish();
					tx = graphDb.beginTx();
				}
			}
			tx.success();
		}finally{
			tx.finish();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		System.out.println("unit testing taxonomy explorer");
//	    String DB_PATH ="/home/smitty/Dropbox/projects/AVATOL/graphtests/neo4j-community-1.8.M02/data/graph.db";
//	    TaxonomyExplorer a = new TaxonomyExplorer(DB_PATH);
//	    a.runittest();
	}

}
