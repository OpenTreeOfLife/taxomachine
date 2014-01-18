package org.opentree.taxonomy;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

/**
 * General functionality for taxonomy loading. Abstract class inherited by actual taxonomy loading classes.
 * @author sas, ceh
 *
 */
public abstract class TaxonomyLoaderBase extends Taxonomy {

	static Logger _LOG = Logger.getLogger(TaxonomyLoaderPreottol.class);
	public static int transaction_iter = 100000;
	int LARGE = 100000000;

	public TaxonomyLoaderBase(GraphDatabaseAgent gdb) {
		super(gdb);
	}
	
	public TaxonomyLoaderBase(GraphDatabaseService gds) {
		super(gds);
	}
	
	/**
	 * delete all the mrcas and nested mrcas
	 * @param dbnode
	 */
	public void removeMRCAs(Node dbnode) {
		TraversalDescription TAXCHILDOF_TRAVERSAL = Traversal.description()
		        .relationships( RelType.TAXCHILDOF,Direction.INCOMING );
		Transaction tx = graphDb.beginTx();
		int count = 0;
		try {
			for (Node frnode: TAXCHILDOF_TRAVERSAL.traverse(dbnode).nodes()) {
				frnode.removeProperty("mrca");
				frnode.removeProperty("nested_mrca");
				if (count % transaction_iter == 0) {
					System.out.println(count);
					tx.success();
					tx.finish();
					tx = graphDb.beginTx();
				}
				count += 1;
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}
	
	private Transaction	general_tx;
	private int cur_tran_iter = 0;
	public void initMrcaForTipsAndPO(Node startnode) {
		general_tx = graphDb.beginTx();
		//start from the node called root
//		Node startnode = getLifeNode();
		try {
			//root should be the taxonomy startnode
			TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
			        .relationships( RelType.TAXCHILDOF,Direction.INCOMING );
			for (Node friendnode : CHILDOF_TRAVERSAL.traverse(startnode).nodes()) {
				Node taxparent = getAdjNodeFromFirstRelationship(friendnode, RelType.TAXCHILDOF, Direction.OUTGOING);
				if (taxparent != null) {
					Node firstchild = getAdjNodeFromFirstRelationship(friendnode, RelType.TAXCHILDOF, Direction.INCOMING);
					if (firstchild == null) {//leaf
						long [] tmrcas = {friendnode.getId()};
						friendnode.setProperty("mrca", tmrcas);
						long [] ntmrcas = {};
						friendnode.setProperty("nested_mrca", ntmrcas);
					}
					cur_tran_iter += 1;
					if (cur_tran_iter % transaction_iter == 0) {
						general_tx.success();
						general_tx.finish();
						general_tx = graphDb.beginTx();
						System.out.println("cur transaction: " + cur_tran_iter);
					}
				} else {
					System.out.println(friendnode+"\t" + friendnode.getProperty("name"));
				}
			}
			general_tx.success();
		} finally {
			general_tx.finish();
		}
		//start the mrcas
		System.out.println("calculating mrcas");
		try {
			general_tx = graphDb.beginTx();
			postorderAddMRCAsTax(startnode);
			general_tx.success();
		} finally {
			general_tx.finish();
		}
	}
	
	static public Node getAdjNodeFromFirstRelationship(Node nd, RelationshipType relType, Direction dir) {
		for (Relationship rel: nd.getRelationships(relType, dir)) {
			if (dir == Direction.OUTGOING) {
				return rel.getEndNode();
			} else {
				return rel.getStartNode();
			}
		}
		return null;
	}
	
	static public Node getAdjNodeFromFirstRelationshipBySource(Node nd, RelationshipType relType, Direction dir,  String src) {
		for (Relationship rel : nd.getRelationships(relType, dir)) {
			if (((String)rel.getProperty("source")).equals(src)) {
				if (dir == Direction.OUTGOING) {
					return rel.getEndNode();
				} else {
					return rel.getStartNode();
				}
			}
		}
		return null;
	}
	
	/**
	 * Just a wrapper for the verifyLoadedTaxonomy that makes it agnostic to source
	 */
	public void verifyMainTaxonomy() {
		verifyLoadedTaxonomy(null); // passing null as sourcename makes it agnostic to source
	}
	
	/**
	 * We are checking the validity of the taxonomy that is loaded.
	 * Validity is based on where there are nodes that have multiple parents
	 * within a source. Will ignore the source property if sourcename == null
	 * 
	 * @param sourcename this is the source that we are checking the validity of
	 */
	public void verifyLoadedTaxonomy(String sourcename) {
		//get life node
		//traverse starting at life, checking to see if any of the nodes have multiple parents if looking at the source from sourcename
		Node startnode = getLifeNode();
		TraversalDescription MRCACHILDOF_TRAVERSAL = Traversal.description()
		        .relationships( RelType.TAXCHILDOF,Direction.INCOMING );
		for (Node friendnode : MRCACHILDOF_TRAVERSAL.traverse(startnode).nodes()) {
			int count = 0;
			for (Relationship rel: friendnode.getRelationships(Direction.OUTGOING, RelType.TAXCHILDOF)) {
				if (sourcename == null) {
					count += 1;
				} else if (((String)rel.getProperty("source")).compareTo(sourcename) == 0) {
					count += 1;
				}
				if (rel.getEndNode().getId() == rel.getStartNode().getId()) {
					System.out.println("invalid (self cycle): " + friendnode);
				}
			}
			if (count > 1) {
				System.out.println("invalid (multiple parents): " + friendnode);
			}
		}
	}
	
	/**
	 * for initial taxonomy to tree processing.  adds a mrca->long[]  property
	 *	to the node and its children (where the elements of the array are the ids
	 *	of graph of life nodes). The property is is the union of the mrca properties
	 *	for the subtree. So the leaves of the tree must already have their mrca property
	 *	filled in!
	 *
	 * @param dbnode should be a node in the graph-of-life (has incoming MRCACHILDOF relationship)
	 */
	private void postorderAddMRCAsTax(Node dbnode) {
		//traversal incoming and record all the names
		for (Relationship rel : dbnode.getRelationships(Direction.INCOMING,RelType.TAXCHILDOF)) {
			Node tnode = rel.getStartNode();
			postorderAddMRCAsTax(tnode);
		}
		//could make this a hashset if dups become a problem
		ArrayList<Long> mrcas = new ArrayList<Long> ();
		ArrayList<Long> nested_mrcas = new ArrayList<Long>();
		if (dbnode.hasProperty("mrca") == false) {
			for (Relationship rel: dbnode.getRelationships(Direction.INCOMING,RelType.TAXCHILDOF)) {
				Node tnode = rel.getStartNode();
				long[] tmrcas = (long[])tnode.getProperty("mrca");
				for (int j = 0; j < tmrcas.length; j++) {
					mrcas.add(tmrcas[j]);
				}
				long[] nestedtmrcas = (long[])tnode.getProperty("nested_mrca");
				for (int j = 0; j < nestedtmrcas.length; j++) {
					nested_mrcas.add(nestedtmrcas[j]);
				}
			}
			long[] ret = new long[mrcas.size()];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = mrcas.get(i).longValue();
			}
			try {
				dbnode.setProperty("mrca", ret);
			} catch(Exception e) {
				System.out.println(dbnode);
				System.out.println(ret.length);
				System.out.println(mrcas.size());
				for (Relationship rel: dbnode.getRelationships(Direction.INCOMING,RelType.TAXCHILDOF)) {
					Node tnode = rel.getStartNode();
					long[] tmrcas = (long[])tnode.getProperty("mrca");
					System.out.println("tmrcas: " + tmrcas.length + " " + rel);
					long[] nestedtmrcas = (long[])tnode.getProperty("nested_mrca");
					System.out.println("tmrcas: " + nestedtmrcas.length + " " + rel);
				}
				System.exit(0);
			}
			
			nested_mrcas.add(dbnode.getId());
			long[] ret2 = new long[nested_mrcas.size()];
			for (int i = 0; i < ret2.length; i++) {
				ret2[i] = nested_mrcas.get(i).longValue();
			}
			dbnode.setProperty("nested_mrca", ret2);
		}
	}	
}
