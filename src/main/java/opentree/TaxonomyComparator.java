package opentree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.index.impl.lucene.Hits;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;

public class TaxonomyComparator {
	
	final TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
			.relationships( RelType.TAXCHILDOF,Direction.OUTGOING );
	
	final TraversalDescription CHILDOFIN_TRAVERSAL = Traversal.description()
			.relationships(RelType.TAXCHILDOF, Direction.INCOMING);
	
	public TaxonomyComparator(){}

	public void compareTaxonomyToDominant(GraphDatabaseAgent dinga,String comparisonsource){
		Taxonomy domtax = new Taxonomy(dinga);		

		Index<Node> taxSources = domtax.ALLTAXA.getNodeIndex(NodeIndexDescription.TAX_SOURCES);
		Index<Node> taxaByName = domtax.ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME);
		
		//get the root of the comparison taxonomy
		IndexHits<Node> hits = taxSources.get("source",comparisonsource);
		Node comproot = hits.getSingle().getSingleRelationship(RelType.METADATAFOR, Direction.OUTGOING).getEndNode();
		hits.close();

		HashMap<Long,String> domnames = new HashMap<Long,String>();
		HashMap<Long,Long> matchedDomCompNodes = new HashMap<Long,Long>();
		HashMap<Long,HashSet<Long>> matchedNodeNodesToAdd = new HashMap<Long,HashSet<Long>>();
		HashSet<String> domtotal = new HashSet<String>();
		long [] tmr = (long []) domtax.getLifeNode().getProperty("mrca");
		for (int i =0;i<tmr.length;i++){
			Node tmrn = domtax.getNodeById(tmr[i]);
			domnames.put(tmrn.getId(), (String) tmrn.getProperty("name"));
			domtotal.add((String) tmrn.getProperty("name"));
		}
		HashMap<Long,String> compnames = new HashMap<Long,String>();
		HashSet<String> comptotal = new HashSet<String>();
		long [] tmr2 = (long []) comproot.getProperty("mrca");
		for (int i =0;i<tmr2.length;i++){
			Node tmrn = domtax.getNodeById(tmr2[i]);
			compnames.put(tmrn.getId(), (String) tmrn.getProperty("name"));
			comptotal.add((String) tmrn.getProperty("name"));
		}
		int totalcount = 0;
		//traverse the dominant taxonomy looking for names that would match
		for (Node tnode: CHILDOFIN_TRAVERSAL.depthFirst().traverse(domtax.getLifeNode()).nodes()){
			if (tnode.hasRelationship(Direction.INCOMING,RelType.TAXCHILDOF) == false){
				continue;
			}else{
				totalcount+= 1;
				//get the names for this node
				HashSet<String> tset = new HashSet<String>();
				HashSet<Long> tsetn = new HashSet<Long>();
				long [] itmr = (long []) tnode.getProperty("mrca");
				for (int i =0;i<itmr.length;i++){
					Node tmrn = domtax.getNodeById(itmr[i]);
					tset.add((String) tmrn.getProperty("name"));
					tsetn.add(tmrn.getId());
				}
				//adding nested tsets
				itmr = (long []) tnode.getProperty("nested_mrca");
				for (int i =0;i<itmr.length;i++){
					Node tmrn = domtax.getNodeById(itmr[i]);
					if(tmrn.getId() == tnode.getId())
						continue;
					tset.add((String) tmrn.getProperty("name"));
					tsetn.add(tmrn.getId());
				}
				Set<String> domtotalmoutgroup = new HashSet<String>(domtotal); // use the copy constructor
				domtotalmoutgroup.removeAll(tset);
				//could be singular but for the synonyms
				ArrayList<String> names = new ArrayList<String>();
				names.add((String)tnode.getProperty("name"));
				if(tnode.hasRelationship(Direction.INCOMING, RelType.SYNONYMOF)){
					for (Relationship srel: tnode.getRelationships(Direction.INCOMING, RelType.SYNONYMOF)){
						names.add((String)srel.getStartNode().getProperty("name"));
					}
				}
				System.out.print("Checking:");
				for(String s: names){
					System.out.print(" "+s);
				}
				System.out.print("\n\n");
				//get any matches in the other taxonomy
				int bestsize = 0; //size of the best match
				Node bestnode = null;
				HashSet<Long> nodestoadd = new HashSet<Long>();
				HashSet<String> stringstoadd = new HashSet<String>();
				for(String ts: names){
					ArrayList<Node> tn = (ArrayList<Node>) domtax.ALLTAXA.findNodes(taxaByName, comparisonsource, ts);
//					System.out.println(tn.size());
					for(Node ttn : tn){
						if(ttn.hasRelationship(RelType.TAXCHILDOF, Direction.INCOMING)==false)
							continue;
						HashSet<String> cset = new HashSet<String>();
						HashSet<Long> csetn = new HashSet<Long>();
						long [] citmr = (long []) ttn.getProperty("mrca");
						for (int i =0;i<citmr.length;i++){
							Node tmrn = domtax.getNodeById(citmr[i]);
							cset.add((String) tmrn.getProperty("name"));
							csetn.add(tmrn.getId());
						}
						citmr = (long []) ttn.getProperty("nested_mrca");
						for (int i =0;i<citmr.length;i++){
							Node tmrn = domtax.getNodeById(citmr[i]);
							if(tmrn.getId() == ttn.getId())
								continue;
							cset.add((String) tmrn.getProperty("name"));
							csetn.add(tmrn.getId());
						}
						boolean test = true;
						Set<String> intersection1 = new HashSet<String>(tset); // use the copy constructor
						intersection1.retainAll(cset);
						if(intersection1.size()==0){
							test = false;
						}else{
							Set<String> intersection2 = new HashSet<String>(domtotalmoutgroup); // use the copy constructor
							intersection2.retainAll(cset);
							if(intersection2.size()>0){
								test = false;
							}
						}
						//best match
						if (test == true){
							System.out.println("match!: "+ttn.getProperty("name")+ " "+cset.size());
							if (cset.size() > bestsize){
								bestsize = cset.size();
								bestnode = ttn;
								nodestoadd = csetn;
								stringstoadd = cset;
							}
						}
					}
				}
				if (bestnode != null){
					stringstoadd.removeAll(tset);
					HashSet<Long> toremove = new HashSet<Long>();
					for(Long nd: nodestoadd){
						if(stringstoadd.contains(domtax.getNodeById(nd).getProperty("name")) == false)
							toremove.add(nd);
					}
					nodestoadd.removeAll(toremove);
					System.out.println("nodestoadd: "+nodestoadd.size());
					if (nodestoadd.size() > 0){
						matchedNodeNodesToAdd.put(tnode.getId(),nodestoadd);
						matchedDomCompNodes.put(tnode.getId(), bestnode.getId());
					}
				}else{
					continue;
				}
			}
		}
		System.out.println("matched: "+matchedDomCompNodes.size()+" out of "+totalcount);
		System.out.println("postorder add taxa");
		Transaction tx = null;
		try{
			tx = dinga.beginTx();
			postorderAddTaxa(domtax.getLifeNode(),domtax.getLifeNode(),matchedDomCompNodes
				,matchedNodeNodesToAdd,comparisonsource,dinga,taxaByName);
		tx.success();
		}finally{
			tx.finish();
		}
		//remake the mrca
		TaxonomyLoader tl = new TaxonomyLoader(dinga);
		System.out.println("calculating mrcas");
		tl.removeMRCAs(domtax.getLifeNode());
		tl.initMrcaForTipsAndPO(domtax.getLifeNode());
	}
	
	private void postorderAddTaxa(Node innode, Node lifenode,HashMap<Long,Long> matchedDomCompNodes
			,HashMap<Long,HashSet<Long>> matchedNodeNodesToAdd,String compsource,GraphDatabaseAgent domgraph
			,Index<Node> taxaByName){
		for(Relationship rel: innode.getRelationships(Direction.INCOMING, RelType.TAXCHILDOF)){
			postorderAddTaxa(rel.getStartNode(),lifenode,matchedDomCompNodes
					,matchedNodeNodesToAdd,compsource,domgraph,taxaByName);
		}
		if(innode.hasRelationship(Direction.INCOMING, RelType.TAXCHILDOF)){
			//skipping lifenode because it just accumulates junk
			if(matchedNodeNodesToAdd.containsKey(innode.getId()) && innode.getId() != lifenode.getId()){
//				System.out.println(innode.getProperty("name"));
				//get the nodes to add and add all between those nodes and the matcheddomecompnodes
				HashSet<Long> cset = matchedNodeNodesToAdd.get(innode.getId());
				HashSet<Long> addednodes = new HashSet<Long>();
				HashMap<Long,Node> oldids_newnodes = new HashMap<Long,Node>(); //needed for the relationships
				HashMap<Long,Node> newids_oldnodes = new HashMap<Long,Node>();
				Long finishnodeid = matchedDomCompNodes.get(innode.getId());
				oldids_newnodes.put(finishnodeid, innode);//putting up the final one 
				newids_oldnodes.put(innode.getId(),domgraph.getNodeById(finishnodeid));
				for(Long tl: cset){
					Node tnode = domgraph.getNodeById(tl);
					//need to check if the nested one is in the names to add, if not, then don't add
					while(tnode.getId() != finishnodeid){
						if(addednodes.contains(tnode.getId()) == false){
							if(cset.contains(tnode.getId()) == true){
								//adding node
//								System.out.println("making node");
								Node newnode = domgraph.createNode();
								newnode.setProperty("name", (String)tnode.getProperty("name"));
								//TODO: add uid
								taxaByName.add(newnode, "name", (String)tnode.getProperty("name"));
								addednodes.add(newnode.getId());
								oldids_newnodes.put(tnode.getId(), newnode);
								newids_oldnodes.put(newnode.getId(),tnode);
							}
							tnode = tnode.getSingleRelationship(RelType.TAXCHILDOF, Direction.OUTGOING).getEndNode();
						}else{
							break;
						}
					}
				}
				for(Long tl:addednodes){
					Node parent = newids_oldnodes.get(tl).getSingleRelationship(RelType.TAXCHILDOF, Direction.OUTGOING).getEndNode();
					while(oldids_newnodes.containsKey(parent.getId())==false && (parent.getId() != finishnodeid) ){
						parent = parent.getSingleRelationship(RelType.TAXCHILDOF, Direction.OUTGOING).getEndNode();
					}
					while(addednodes.contains(oldids_newnodes.get(parent.getId()).getId())==false &&
							(parent.getId() != finishnodeid)){
						parent = parent.getSingleRelationship(RelType.TAXCHILDOF, Direction.OUTGOING).getEndNode();
						while(oldids_newnodes.containsKey(parent.getId())==false && (parent.getId() != finishnodeid) ){
							parent = parent.getSingleRelationship(RelType.TAXCHILDOF, Direction.OUTGOING).getEndNode();
						}
					}
					//create relationship
//					System.out.println("creating from (old)new: ("+newids_oldnodes.get(tl)+")" +
//							oldids_newnodes.get(newids_oldnodes.get(tl).getId())+" ("+parent+")"+oldids_newnodes.get(parent.getId()));
					Relationship rel = oldids_newnodes.get(newids_oldnodes.get(tl).getId()).
							createRelationshipTo(oldids_newnodes.get(parent.getId()),RelType.TAXCHILDOF);
					//TODO: add information on rel
					Relationship oldrel = newids_oldnodes.get(tl).getSingleRelationship(RelType.TAXCHILDOF, Direction.OUTGOING);
					String childid = (String)oldrel.getProperty("childid");
					String parentid = (String)oldrel.getProperty("parentid");
					rel.setProperty("source", compsource);
					rel.setProperty("childid",childid);
					rel.setProperty("parentid",parentid);
				}
				
				//now remove them from all the parents
				Node curnode = innode;
				HashSet<Long> csetcopy = new HashSet<Long>(cset);
				while(curnode.getId() != lifenode.getId()){
					if(matchedNodeNodesToAdd.containsKey(curnode.getId())){
						matchedNodeNodesToAdd.get(curnode.getId()).removeAll(csetcopy);
					}
					curnode = curnode.getSingleRelationship(RelType.TAXCHILDOF, Direction.OUTGOING).getEndNode();
				}
				matchedNodeNodesToAdd.get(lifenode.getId()).removeAll(csetcopy);
			}
			innode.removeProperty("mrca");
			innode.removeProperty("nested_mrca");
		}
	}

}

//DELETE IN A BIT, THIS IS FOR TAXONOMIES IN SEPARATE GRAPHS
/*
 * This assumes that the best matching taxonomy is the name that 
 * includes at least one matching name subtending the node and 
 * no overlapping names in the outgroup. So if you are comparing 
 * dominant n1 and comparison n2
 * intersect(n1,n2) >= 1
 * intersect(ogroup1,n2) == 0
 */
/*
public void compareTaxonomyToDominant(GraphDatabaseAgent dinga,GraphDatabaseAgent inga){
	Taxonomy domtax = new Taxonomy(dinga);		
	Taxonomy comptax = new Taxonomy(inga);
	//just to make it cleaner, can optimize later
	System.out.println("getting dominant taxa names");
	HashMap<Long,String> domnames = new HashMap<Long,String>();
	HashMap<Long,Long> matchedDomCompNodes = new HashMap<Long,Long>();
	HashMap<Long,HashSet<Long>> matchedNodeNodesToAdd = new HashMap<Long,HashSet<Long>>();
	HashSet<String> domtotal = new HashSet<String>();
	long [] tmr = (long []) domtax.getLifeNode().getProperty("mrca");
	System.out.println("1: "+domtax.getLifeNode()+" "+tmr.length);
	for (int i =0;i<tmr.length;i++){
		Node tmrn = domtax.getNodeById(tmr[i]);
		domnames.put(tmrn.getId(), (String) tmrn.getProperty("name"));
		domtotal.add((String) tmrn.getProperty("name"));
	}
	System.out.println(domtax == comptax);
	System.out.println("total domnames: "+domtotal.size());
	System.out.println("getting comparison taxa names");
	HashMap<Long,String> compnames = new HashMap<Long,String>();
	HashSet<String> comptotal = new HashSet<String>();
	long [] tmr2 = (long []) comptax.getLifeNode().getProperty("mrca");
	System.out.println("2: "+comptax.getLifeNode()+" "+tmr2.length);
	for (int i =0;i<tmr2.length;i++){
		Node tmrn = comptax.getNodeById(tmr2[i]);
		compnames.put(tmrn.getId(), (String) tmrn.getProperty("name"));
		comptotal.add((String) tmrn.getProperty("name"));
	}
	System.out.println("total compnames: "+comptotal.size());
	int totalcount = 0;
	//traverse the dominant taxonomy looking for names that would match
	for (Node tnode: CHILDOFIN_TRAVERSAL.depthFirst().traverse(domtax.getLifeNode()).nodes()){
		if (tnode.hasRelationship(Direction.INCOMING,RelType.TAXCHILDOF) == false){
			continue;
		}else{
			//get the names for this node
			HashSet<String> tset = new HashSet<String>();
			HashSet<Long> tsetn = new HashSet<Long>();
			long [] itmr = (long []) tnode.getProperty("mrca");
			for (int i =0;i<itmr.length;i++){
				Node tmrn = domtax.getNodeById(itmr[i]);
				tset.add((String) tmrn.getProperty("name"));
				tsetn.add(tmrn.getId());
			}
			System.out.println("original tset: "+tset);
			Set<String> domtotalmoutgroup = new HashSet<String>(domtotal); // use the copy constructor
			domtotalmoutgroup.removeAll(tset);
			//could be singular but for the synonyms
			ArrayList<String> names = new ArrayList<String>();
			names.add((String)tnode.getProperty("name"));
			if(tnode.hasRelationship(Direction.INCOMING, RelType.SYNONYMOF)){
				for (Relationship srel: tnode.getRelationships(Direction.INCOMING, RelType.SYNONYMOF)){
					names.add((String)srel.getStartNode().getProperty("name"));
				}
			}
			System.out.print("Checking:");
			for(String s: names){
				System.out.print(" "+s);
			}
			System.out.print("\n\n");
			//get any matches in the other taxonomy
			int bestsize = 0; //size of the best match
			Node bestnode = null;
			HashSet<Long> nodestoadd = new HashSet<Long>();
			HashSet<String> stringstoadd = new HashSet<String>();
			for(String ts: names){
				ArrayList<Node> tn = (ArrayList<Node>) comptax.ALLTAXA.findTaxNodesByName(ts);
//				System.out.println(tn.size());
				for(Node ttn : tn){
					if(ttn.hasRelationship(RelType.TAXCHILDOF, Direction.INCOMING)==false)
						continue;
					HashSet<String> cset = new HashSet<String>();
					HashSet<Long> csetn = new HashSet<Long>();
					long [] citmr = (long []) ttn.getProperty("mrca");
					for (int i =0;i<citmr.length;i++){
						Node tmrn = comptax.getNodeById(citmr[i]);
						cset.add((String) tmrn.getProperty("name"));
						csetn.add(tmrn.getId());
					}
					boolean test = true;
					System.out.print("t: ");
					System.out.print(tset);
					System.out.print("\nc: ");
					System.out.print(cset);
					System.out.print("\n");
					Set<String> intersection1 = new HashSet<String>(tset); // use the copy constructor
					intersection1.retainAll(cset);
					System.out.println(intersection1);
					System.out.println("intersection1: "+intersection1.size());
					if(intersection1.size()==0){
						test = false;
					}else{
						Set<String> intersection2 = new HashSet<String>(domtotalmoutgroup); // use the copy constructor
						intersection2.retainAll(cset);
						System.out.println("intersection2: "+intersection2.size());
						System.out.println(intersection2);
						if(intersection2.size()>0){
							test = false;
						}
					}
					//best match
					if (test == true){
						System.out.println("match!: "+ttn.getProperty("name")+ " "+cset.size());
						if (cset.size() > bestsize){
							bestsize = cset.size();
							bestnode = ttn;
							nodestoadd = csetn;
							stringstoadd = cset;
						}
					}
				}
			}
			if (bestnode != null){
				matchedDomCompNodes.put(tnode.getId(), bestnode.getId());
				System.out.println("tset: "+tset);
				System.out.println("stringstoadd: "+stringstoadd);
				stringstoadd.removeAll(tset);
				System.out.println("stringstoadd: "+stringstoadd.size());
				HashSet<Long> toremove = new HashSet<Long>();
				for(Long nd: nodestoadd){
					if(stringstoadd.contains(inga.getNodeById(nd).getProperty("name")) == false)
						toremove.add(nd);
				}
				nodestoadd.removeAll(toremove);
				System.out.println("nodestoadd: "+nodestoadd.size());
				matchedNodeNodesToAdd.put(tnode.getId(),nodestoadd);
			}else{
				continue;
			}
			totalcount+= 1;
		}
	}
	System.out.println("matched: "+matchedDomCompNodes.size()+" out of "+totalcount);
	System.out.println("postorder add taxa");
	Transaction tx = null;
	try{
		tx = dinga.beginTx();
		postorderAddTaxa(domtax.getLifeNode(),domtax.getLifeNode(),matchedDomCompNodes
			,matchedNodeNodesToAdd,inga);
	tx.success();
	}finally{
		tx.finish();
	}
	//remake the mrca
	TaxonomyLoader tl = new TaxonomyLoader(dinga);
	System.out.println("calculating mrcas");
	try{
		tx = dinga.beginTx();
		tl.postorderAddMRCAsTax(domtax.getLifeNode());
		tx.success();
	}finally{
		tx.finish();
	}
	
}

private void postorderAddTaxa(Node innode, Node lifenode,HashMap<Long,Long> matchedDomCompNodes
		,HashMap<Long,HashSet<Long>> matchedNodeNodesToAdd,GraphDatabaseAgent compgraph){
	for(Relationship rel: innode.getRelationships(Direction.INCOMING, RelType.TAXCHILDOF)){
		postorderAddTaxa(rel.getStartNode(),lifenode,matchedDomCompNodes
				,matchedNodeNodesToAdd,compgraph);
	}
	if(innode.hasRelationship(Direction.INCOMING, RelType.TAXCHILDOF)){
		if(matchedNodeNodesToAdd.containsKey(innode.getId())){
//			System.out.println(innode.getProperty("name"));
			//get the nodes to add and add all between those nodes and the matcheddomecompnodes
			HashSet<Long> cset = matchedNodeNodesToAdd.get(innode.getId());
			HashSet<Long> addednodes = new HashSet<Long>();
			HashMap<Long,Node> oldids_newnodes = new HashMap<Long,Node>(); //needed for the relationships
			Long finishnodeid = matchedDomCompNodes.get(innode.getId());
			oldids_newnodes.put(finishnodeid, innode);//putting up the final one 
			for(Long tl: cset){
				Node tnode = compgraph.getNodeById(tl);
				//need to check if the nested one is in the names to add, if not, then don't add
				while(tnode.getId() != finishnodeid){
					if(addednodes.contains(tnode.getId()) == false){
						//adding node
						System.out.println("making node");
						Node newnode = compgraph.createNode();
						newnode.setProperty("name", (String)tnode.getProperty("name"));
						addednodes.add(newnode.getId());
						oldids_newnodes.put(tnode.getId(), newnode);
					}else{
						break;
					}
				}
			}
			for(Long tl:addednodes){
				//create relationship
				//start node ;
				//;
				//create to 
				Relationship rel = oldids_newnodes.get(tl).createRelationshipTo(oldids_newnodes.get(compgraph.getNodeById(tl)
						.getSingleRelationship(RelType.TAXCHILDOF,Direction.OUTGOING)
						.getEndNode().getId())
						,RelType.TAXCHILDOF);
				rel.setProperty("source", "NEWONE");
			}
			
			//now remove them from all the parents
			Node curnode = innode;
			while(curnode.getId() != lifenode.getId()){
				if(matchedNodeNodesToAdd.containsKey(curnode.getId())){
					matchedNodeNodesToAdd.get(curnode.getId()).removeAll(cset);
				}
				curnode = curnode.getSingleRelationship(RelType.TAXCHILDOF, Direction.OUTGOING).getEndNode();
			}
		}
		innode.removeProperty("mrca");
		innode.removeProperty("nested_mrca");
	}
}
*/