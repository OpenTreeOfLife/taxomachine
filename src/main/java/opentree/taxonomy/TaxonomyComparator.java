package opentree.taxonomy;

/**
 * TaxonomyComparator functions
 * ====================
 * compare an added taxonomy to the dominant taxonomy and add nodes where possible
 * compare a taxonomy without adding it so that we can assign ids
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import opentree.taxonomy.contexts.TaxonomyNodeIndex;

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
import org.opentree.properties.OTVocabulary;

public class TaxonomyComparator {
	
	final TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
			.relationships( RelType.TAXCHILDOF,Direction.OUTGOING );
	
	final TraversalDescription CHILDOFIN_TRAVERSAL = Traversal.description()
			.relationships(RelType.TAXCHILDOF, Direction.INCOMING);
	
	public TaxonomyComparator(){}

	/**
	 * This should compare an additional taxonomic source to the one that is 
	 * managed by the main indices. This will typically be the NCBI taxonomy.
	 * 
	 * @param dinga 
	 * @param comparisonsource
	 */
	public void compareGraftTaxonomyToDominant(GraphDatabaseAgent dinga,String comparisonsource){
		Taxonomy domtax = new Taxonomy(dinga);		

		Index<Node> taxSources = domtax.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXONOMY_SOURCES);
		Index<Node> taxaByName = domtax.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME);
		
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
				//turned off synonyms
				/*if(tnode.hasRelationship(Direction.INCOMING, RelType.SYNONYMOF)){
					for (Relationship srel: tnode.getRelationships(Direction.INCOMING, RelType.SYNONYMOF)){
						names.add((String)srel.getStartNode().getProperty("name"));
					}
				}*/
//				System.out.print("Checking:");
//				for(String s: names){
//					System.out.print(" "+s);
//				}
//				System.out.print("\n\n");
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
//							System.out.println("match!: "+ttn.getProperty("name")+ " "+cset.size());
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
					if(nodestoadd.size()> 0)
						System.out.println((String)tnode.getProperty("name")+" nodestoadd: "+nodestoadd.size());
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
		added_source_ids = new HashSet<Long>();
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
		TaxonomyLoaderPreottol tl = new TaxonomyLoaderPreottol(dinga);
		System.out.println("calculating mrcas");
		tl.removeMRCAs(domtax.getLifeNode());
		tl.initMrcaForTipsAndPO(domtax.getLifeNode());
	}
	
	//GLOBAL ids for added source nodes
	//add a check for nodes that were added already because of homonym issues, if you 
	//turn off, for example you will add Drilonematidae and things below twice
	private HashSet<Long> added_source_ids;//will only contain added ids from the source being added 
	
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
//				System.out.println("postorder: "+innode.getProperty("name"));
				//get the nodes to add and add all between those nodes and the matcheddomecompnodes
				HashSet<Long> cset = matchedNodeNodesToAdd.get(innode.getId());
				HashSet<Long> addednodes = new HashSet<Long>();
				HashSet<Long> addednodesch = new HashSet<Long> ();
				HashMap<Long,Node> oldids_newnodes = new HashMap<Long,Node>(); //needed for the relationships
				HashMap<Long,Node> newids_oldnodes = new HashMap<Long,Node>();
				Long finishnodeid = matchedDomCompNodes.get(innode.getId());
				oldids_newnodes.put(finishnodeid, innode);//putting up the final one 
				newids_oldnodes.put(innode.getId(),domgraph.getNodeById(finishnodeid));
				for(Long tl: cset){
					Node tnode = domgraph.getNodeById(tl);
//					System.out.println("tl: "+tl+" "+tnode);
					//need to check if the nested one is in the names to add, if not, then don't add
					while(tnode.getId() != finishnodeid){
						if(addednodesch.contains(tnode.getId()) == false){
							//added test to make sure dups from the source aren't added, occurs with bad homonyms, for example Drilonematidae
							Long testingadded = Long.valueOf((String)tnode.getProperty("sourceid"));
							if(cset.contains(tnode.getId()) == true && added_source_ids.contains(testingadded)==false){
								//adding node
								Node newnode = domgraph.createNode();
//								System.out.println("making node: "+newnode+" "+tnode.getProperty("name"));
								newnode.setProperty("name", (String)tnode.getProperty("name"));
								//TODO: add uid
								newnode.setProperty(OTVocabulary.OT_OTT_ID.propertyName(), newnode.getId());
								newnode.setProperty("source",(String)tnode.getProperty("source"));
								if (tnode.hasProperty("rank"))
									newnode.setProperty("rank",(String)tnode.getProperty("rank"));
								newnode.setProperty("sourceid",(String)tnode.getProperty("sourceid"));
								//added for check on not duplicating source additions
								added_source_ids.add(Long.valueOf((String)tnode.getProperty("sourceid")));
								newnode.setProperty("sourcepid",(String)tnode.getProperty("sourcepid"));
								taxaByName.add(newnode, "name", (String)tnode.getProperty("name"));
								addednodes.add(newnode.getId());
								addednodesch.add(tnode.getId());
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
					Relationship rel = oldids_newnodes.get(newids_oldnodes.get(tl).getId()).
							createRelationshipTo(oldids_newnodes.get(parent.getId()),RelType.TAXCHILDOF);
//					System.out.println("making rel: "+rel+" "+oldids_newnodes.get(newids_oldnodes.get(tl).getId()).getId()+ " -> "+oldids_newnodes.get(parent.getId()));
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
	
	/**
	 * This is intended to be used, for example, to compare the names in a file (preottol) to the ottol
	 * and create a mapping of those. The infile is expected to be in the format of 
	 * id,parentid,name
	 * 
	 * The output will be 
	 * id,ottolid
	 * @param infilename
	 * @param outfilename
	 * @param dinga
	 */
	public void compareDontAddNamesToOTTOL(String infilename, String outfilename,GraphDatabaseAgent dinga){
		Taxonomy domtax = new Taxonomy(dinga);		
		Index<Node> taxSources = domtax.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXONOMY_SOURCES);
		Index<Node> taxaByName = domtax.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME);
		
		String str = "";
		BufferedReader sbr;
		HashMap<String,String> id_name_map = new HashMap<String,String>();
		HashMap<String,String> id_parent_map = new HashMap<String,String>();
		HashMap<String,HashSet<String>> id_nameset_map = new HashMap<String,HashSet<String>>();
		HashMap<String,HashSet<String>> id_nameset_map_name = new HashMap<String,HashSet<String>>();
		//read all the names and ids from the file
		try {
			sbr = new BufferedReader(new FileReader(infilename));
			while ((str = sbr.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(str,"\t|\t");
				String id = st.nextToken();
				String pid = st.nextToken();
				String name = st.nextToken();
				id_name_map.put(id, name);
				id_parent_map.put(id,pid);
				id_nameset_map.put(id,new HashSet<String>());
				id_nameset_map_name.put(id,new HashSet<String>());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

		System.out.println("checking "+id_name_map.size()+" names");
		//get all the names that subtend a particular name and place them in the id_nameset_map
		for(String id: id_name_map.keySet()){
			String curid = id_parent_map.get(id);
			while(id_name_map.containsKey(curid)){
				id_nameset_map.get(curid).add(id);
				id_nameset_map_name.get(curid).add(id_name_map.get(id));
				if(id_parent_map.containsKey(curid)){
					curid = id_parent_map.get(curid);
				}else{
					break;
				}
			}
		}
		HashMap<Long,String> domnames = new HashMap<Long,String>();
		HashSet<String> domtotal = new HashSet<String>();
		long [] tmr = (long []) domtax.getLifeNode().getProperty("mrca");
		for (int i =0;i<tmr.length;i++){
			Node tmrn = domtax.getNodeById(tmr[i]);
			domnames.put(tmrn.getId(), (String) tmrn.getProperty("name"));
			domtotal.add((String) tmrn.getProperty("name"));
		}
		BufferedWriter bwMain = null;
		FileWriter fw = null;
		try{
			bwMain = new BufferedWriter(new FileWriter(outfilename));
			for(String id: id_name_map.keySet()){
				if(id_nameset_map.get(id).size() > 0){
					ArrayList<Node> tn = (ArrayList<Node>) domtax.ALLTAXA.findPrefTaxNodesByNameOrSyn(id_name_map.get(id));
					
					//NO MATCH
					//if(tn.size()<1){
					//	System.out.println(id+" "+id_name_map.get(id)+" "+tn.size());
					//}
					//check each name
					for(Node ttn: tn){
						HashSet<String> cset = new HashSet<String>();
						long [] citmr = (long []) ttn.getProperty("mrca");
						for (int i =0;i<citmr.length;i++){
							Node tmrn = domtax.getNodeById(citmr[i]);
							cset.add((String) tmrn.getProperty("name"));
						}
						citmr = (long []) ttn.getProperty("nested_mrca");
						for (int i =0;i<citmr.length;i++){
							Node tmrn = domtax.getNodeById(citmr[i]);
							if(tmrn.getId() == ttn.getId())
								continue;
							cset.add((String) tmrn.getProperty("name"));
						}
						Set<String> domtotalmoutgroup = new HashSet<String>(domtotal); // use the copy constructor
						domtotalmoutgroup.removeAll(cset);
						boolean test = true;
						Set<String> intersection1 = new HashSet<String>(id_nameset_map_name.get(id)); // use the copy constructor
						intersection1.retainAll(cset);
						if(intersection1.size()==0){
							test = false;
						}else{
							Set<String> intersection2 = new HashSet<String>(domtotalmoutgroup); // use the copy constructor
							intersection2.retainAll(id_nameset_map.get(id));
							if(intersection2.size()>intersection1.size()){
								test = false;
							}
						}
						//best match
						if (test == true){
							//write the result and break
							bwMain.write(id+"\t|\t"+ttn.getProperty(OTVocabulary.OT_OTT_ID.propertyName())+"\t|\t"+ttn.getProperty("name")+"\t|\t\n");
							break;
						}
					}
				}else{
					ArrayList<Node> tn = (ArrayList<Node>) domtax.ALLTAXA.findPrefTaxNodesByNameOrSyn(id_name_map.get(id));
					if(tn.size() == 1){
						//write the result out
						bwMain.write(id+"\t|\t"+tn.get(0).getProperty(OTVocabulary.OT_OTT_ID.propertyName())+"\t|\t"+tn.get(0).getProperty("name")+"\t|\t\n");
					}
					//if(tn.size()<1){
					//	System.out.println(id+" "+id_name_map.get(id)+" "+tn.size());
					//}
				}
			}
			bwMain.close();
		}catch(IOException e1){}
	}
}
