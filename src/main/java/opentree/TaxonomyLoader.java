package opentree;


import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.*;
import org.apache.log4j.Logger;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * TaxonomyLoader is intended to control the initial creation 
 * and addition of taxonomies to the taxonomy graph.
 *
 */
public class TaxonomyLoader extends TaxonomyBase{
	static Logger _LOG = Logger.getLogger(TaxonomyLoader.class);
	int transaction_iter = 100000;
	int LARGE = 100000000;
	int globaltransactionnum = 0;
	Transaction gtx = null;
		
	//basic traversal method
	final TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
			.relationships( RelTypes.TAXCHILDOF,Direction.OUTGOING );
	
	/**
	 * Initializer assume that the graph is being used as embedded
	 * @param graphname directory path to embedded graph
	 */
	public TaxonomyLoader(String graphname){
		graphDb = new EmbeddedGraphDatabase( graphname );
		taxNodeIndex = graphDb.index().forNodes( "taxNodes" );
		prefTaxNodeIndex = graphDb.index().forNodes("prefTaxNodes");
		prefSynNodeIndex = graphDb.index().forNodes("prefSynNodes");
		synNodeIndex = graphDb.index().forNodes("synNodes");
		taxSourceIndex = graphDb.index().forNodes("taxSources");
	}
	
	/**
	 * Reads a taxonomy file with rows formatted as:
	 *	taxon_id\t|\tparent_id\t|\tName with spaces allowed\n
	 *
	 * Creates nodes and TAXCHILDOF relationships for each line.
	 * Nodes get a "name" property. Relationships get "source", "childid", "parentid" properties.
	 * 
	 * Nodes are indexed in taxNames "name" key and id value.
	 * 
	 * A metadata node is created to point to the root
	 * 
	 * The line that has no parent will be the root of this tree
	 * 
	 * @param sourcename this becomes the value of a "source" property in every relationship between the taxonomy nodes
	 * @param filename file path to the taxonomy file
	 * @param synonymfile file that holds the synonym
	 */
	public void initializeTaxonomyIntoGraph(String sourcename, String filename, String synonymfile){
		String str = "";
		int count = 0;
		Transaction tx;
		ArrayList<String> templines = new ArrayList<String>();
		HashMap<String,ArrayList<ArrayList<String>>> synonymhash = null;
		boolean synFileExists = false;
		if(synonymfile.length()>0)
			synFileExists = true;
		//preprocess the synonym file
		//key is the id from the taxonomy, the array has the synonym and the type of synonym
		if(synFileExists){
			synonymhash = new HashMap<String,ArrayList<ArrayList<String>>>();
			try {
				BufferedReader sbr = new BufferedReader(new FileReader(synonymfile));
				while((str = sbr.readLine())!=null){
					StringTokenizer st = new StringTokenizer(str,"\t|\t");
					String id = st.nextToken();
					String name = st.nextToken();
					String type = st.nextToken();
					ArrayList<String> tar = new ArrayList<String>();
					tar.add(name);tar.add(type);
					if (synonymhash.get(id) == null){
						ArrayList<ArrayList<String> > ttar = new ArrayList<ArrayList<String> >();
						synonymhash.put(id, ttar);
					}
					synonymhash.get(id).add(tar);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			System.out.println("synonyms: "+synonymhash.size());
		}
		//finished processing synonym file
		HashMap<String, Node> dbnodes = new HashMap<String, Node>();
		HashMap<String, String> parents = new HashMap<String, String>();
		try{
			tx = graphDb.beginTx();
			//create the metadata node
			Node metadatanode = null;
			try{
				metadatanode = graphDb.createNode();
				metadatanode.setProperty("source", sourcename);
				metadatanode.setProperty("author", "no one");
				taxSourceIndex.add(metadatanode, "source", sourcename);
				tx.success();
			}finally{
				tx.finish();
			}
			BufferedReader br = new BufferedReader(new FileReader(filename));
			while((str = br.readLine())!=null){
				count += 1;
				templines.add(str);
				if (count % transaction_iter == 0){
					System.out.print(count);
					System.out.print("\n");
					tx = graphDb.beginTx();
					try{
						for(int i=0;i<templines.size();i++){
							StringTokenizer st = new StringTokenizer(templines.get(i),"\t|\t");
							int numtok = st.countTokens();
							String inputId = st.nextToken();
							String InputParentId = "";
							if(numtok == 3)
								InputParentId = st.nextToken();
							String inputName = st.nextToken();
							Node tnode = graphDb.createNode();
							tnode.setProperty("name", inputName);
							taxNodeIndex.add( tnode, "name", inputName);
							dbnodes.put(inputId, tnode);
							if (numtok == 3){
								parents.put(inputId, InputParentId);
							}else{ // this is the root node
								System.out.println("created root node and metadata link");
								metadatanode.createRelationshipTo(tnode, RelTypes.METADATAFOR);
							}
							// synonym processing
							if(synFileExists){
								if(synonymhash.get(inputId)!=null){
									ArrayList<ArrayList<String>> syns = synonymhash.get(inputId);
									for(int j=0;j<syns.size();j++){
										
										String synName = syns.get(j).get(0);
										String synNameType = syns.get(j).get(1);
										
										Node synode = graphDb.createNode();
										synode.setProperty("name",synName);
										synode.setProperty("nametype",synNameType);
										synode.setProperty("source",sourcename);
										synode.createRelationshipTo(tnode, RelTypes.SYNONYMOF);
										synNodeIndex.add(synode, "name", synName);
									}
								}
							}
						}
						tx.success();
					}finally{
						tx.finish();
					}
					templines.clear();
				}
			}
			br.close();
			tx = graphDb.beginTx();
			try{
				for(int i=0;i<templines.size();i++){
					StringTokenizer st = new StringTokenizer(templines.get(i),"\t|\t");
					int numtok = st.countTokens();
					String first = st.nextToken();
					String second = "";
					if(numtok == 3)
						second = st.nextToken();
					String third = st.nextToken();
					Node tnode = graphDb.createNode();
					tnode.setProperty("name", third);
					taxNodeIndex.add( tnode, "name", third);
					dbnodes.put(first, tnode);
					if (numtok == 3){
						parents.put(first, second);
					}else{//this is the root node
						System.out.println("created root node and metadata link");
						metadatanode.createRelationshipTo(tnode, RelTypes.METADATAFOR);
					}
					//synonym processing
					if(synFileExists){
						if(synonymhash.get(first)!=null){
							ArrayList<ArrayList<String>> syns = synonymhash.get(first);
							for(int j=0;j<syns.size();j++){
								
								String synName = syns.get(j).get(0);
								String synNameType = syns.get(j).get(1);
								
								Node synode = graphDb.createNode();
								synode.setProperty("name",synName);
								synode.setProperty("nametype",synNameType);
								synode.setProperty("source",sourcename);
								synode.createRelationshipTo(tnode, RelTypes.SYNONYMOF);
								synNodeIndex.add(synode, "name", synName);
							}
						}
					}
				}
				tx.success();
			}finally{
				tx.finish();
			}
			templines.clear();
			//add the relationships
			ArrayList<String> temppar = new ArrayList<String>();
			count = 0;
			for(String key: dbnodes.keySet()){
				count += 1;
				temppar.add(key);
				if (count % transaction_iter == 0){
					System.out.println(count);
					tx = graphDb.beginTx();
					try{
						for (int i=0;i<temppar.size();i++){
							try {
								Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelTypes.TAXCHILDOF);
								rel.setProperty("source", sourcename);
								rel.setProperty("childid",temppar.get(i));
								rel.setProperty("parentid",parents.get(temppar.get(i)));
							}catch(java.lang.IllegalArgumentException io){
//								System.out.println(temppar.get(i));
								continue;
							}
						}
						tx.success();
					}finally{
						tx.finish();
					}
					temppar.clear();
				}
			}
			tx = graphDb.beginTx();
			try{
				for (int i=0;i<temppar.size();i++){
					try {
						Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelTypes.TAXCHILDOF);
						rel.setProperty("source", sourcename);
						rel.setProperty("childid",temppar.get(i));
						rel.setProperty("parentid",parents.get(temppar.get(i)));
					}catch(java.lang.IllegalArgumentException io){
//						System.out.println(temppar.get(i));
						continue;
					}
				}
				tx.success();
			}finally{
				tx.finish();
			}
		}catch(IOException ioe){}
		//mark all of the barrier nodes with additional metadata
		System.out.println("setting barrier nodes");
		BarrierNodes bn = new BarrierNodes(graphDb);
		ArrayList<Node> barrierNodes = bn.getBarrierNodes();
		HashMap<String,String> barrierNodesMap = bn.getBarrierNodeMap();
		TraversalDescription CHILDREN_TRAVERSAL = Traversal.description()
				.relationships( RelTypes.TAXCHILDOF,Direction.INCOMING );
		tx = graphDb.beginTx();
		try{
			for (int i=0;i<barrierNodes.size();i++){
				for(Node currentNode : CHILDREN_TRAVERSAL.traverse(barrierNodes.get(i)).nodes()){
					currentNode.setProperty("taxcode", barrierNodesMap.get(barrierNodes.get(i).getProperty("name")));
				}
			}
			tx.success();
		}finally{
			tx.finish();
		}
	}
	
	HashMap<String, ArrayList<String>> globalchildren = null;
	HashMap<String,String> globalidnamemap = null;
	PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.TAXCHILDOF, Direction.OUTGOING ),10000);
	HashMap<Node,Node> lastexistingmatchparents = new HashMap<Node,Node>();
	
	/**
	 * See addInitialTaxonomyTableIntoGraph 
	 * This function acts like addInitialTaxonomyTableIntoGraph but it 
	 *	can be called for a taxonomy that is not the first taxonomy in the graph
	 * 
	 * Rather than each line resulting in a new node, only names that have not
	 *		 been encountered before will result in new node objects.
	 *
	 * To connect a subtree from the new taxonomy to the taxonomy tree the 
	 *	taxNodeIndex of the existing graph is checked the new name. If multiple
	 *	nodes have been assigned the name, then the one with the lowest score
	 *	is assumed to be the closest match (the score is calculated by counting
	 *	the number of nodes traversed in the path new->anc* + the number of 
	 *	nodes in old->anc* where "anc*" denotes the lowest ancestor in
	 *	the new taxon's ancestor path that has a match in the old graph (and
	 *	the TAXCHILDOF is the relationship on the path).
	 *	
	 * @param filename file path to the taxonomy file
	 * @param sourcename this becomes the value of a "source" property in every relationship between the taxonomy nodes
	 */
	public void addAdditionalTaxonomyToGraph(String sourcename, String rootid, String filename, String synonymfile){
		Node rootnode = null;
		String str = "";
		String roottaxid = "";
		if (rootid.length() > 0){
			rootnode = graphDb.getNodeById(Long.valueOf(rootid));
			System.out.println(rootnode);
		}
		PathFinder<Path> tfinder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelTypes.TAXCHILDOF, Direction.OUTGOING ),10000);

		HashMap<String,ArrayList<ArrayList<String>>> synonymhash = null;
		boolean synFileExists = false;
		if(synonymfile.length()>0)
			synFileExists = true;
		//preprocess the synonym file
		//key is the id from the taxonomy, the array has the synonym and the type of synonym
		if(synFileExists){
			synonymhash = new HashMap<String,ArrayList<ArrayList<String>>>();
			try {
				BufferedReader sbr = new BufferedReader(new FileReader(synonymfile));
				while((str = sbr.readLine())!=null){
					StringTokenizer st = new StringTokenizer(str,"\t|\t");
					String id = st.nextToken();
					String name = st.nextToken();
					String type = st.nextToken();
					ArrayList<String> tar = new ArrayList<String>();
					tar.add(name);tar.add(type);
					if (synonymhash.get(id) == null){
						ArrayList<ArrayList<String> > ttar = new ArrayList<ArrayList<String> >();
						synonymhash.put(id, ttar);
					}
					synonymhash.get(id).add(tar);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			System.out.println("synonyms: "+synonymhash.size());
		}
		
		//get what barriers in taxonomy are parent to the input root (so is this
		//higher than plants, fungi, or animals (helps clarify homonyms
		BarrierNodes bn = new BarrierNodes(graphDb);
		ArrayList<Node> barrierNodes = bn.getBarrierNodes();
		HashMap<String,String> barrierNodesMap = bn.getBarrierNodeMap();
		Node rootbarrier = null;
		for (int i =0;i<barrierNodes.size();i++){
			Path tpath = tfinder.findSinglePath(rootnode, barrierNodes.get(i));
			if (tpath != null)
				rootbarrier = barrierNodes.get(i);
		}
		HashMap<String,Node> taxcontainedbarriersmap = new HashMap<String,Node>();
		
		HashMap<String, String> idparentmap = new HashMap<String, String>(); // node number -> parent's number
		HashMap<String, String> idnamemap = new HashMap<String, String>();
		ArrayList<String> idlist = new ArrayList<String>();
		Transaction tx;
		//GET NODE
		tx = graphDb.beginTx();
		try{
			Node metadatanode = null;
			metadatanode = graphDb.createNode();
			metadatanode.setProperty("source", sourcename);
			metadatanode.setProperty("author", "no one");
			taxSourceIndex.add(metadatanode, "source", sourcename);
			try{
				BufferedReader br = new BufferedReader(new FileReader(filename));
				while((str = br.readLine())!=null){
					StringTokenizer st = new StringTokenizer(str,"\t|\t");
					int numtok = st.countTokens();
					String id = st.nextToken();
					idlist.add(id);
					String parid = "";
					if(numtok == 3)
						parid = st.nextToken();
					String name = st.nextToken();
					idnamemap.put(id, name);
					if (numtok == 3){
						idparentmap.put(id, parid);
					}else{//this is the root node
						if(rootnode == null){
							System.out.println("the root should never be null");
							System.exit(0);
							//if the root node is null then you need to make a new one
							//rootnode = graphDb.createNode();
							//rootnode.setProperty("name", third);
						}
						roottaxid = id;
						System.out.println("matched root node and metadata link");
						metadatanode.createRelationshipTo(rootnode, RelTypes.METADATAFOR);
					}
					//check to see if the name is a barrier
					HashSet<String> barrierNames = bn.getBarrierNodeNames();
					if (barrierNames.contains(name)){
						for(int j=0;j<barrierNodes.size();j++){
							if (((String)barrierNodes.get(j).getProperty("name")).equals(name)){	
								taxcontainedbarriersmap.put(id,barrierNodes.get(j));
								System.out.println("added barrier node "+name);
							}
						}
					}
				}
				br.close();
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("problem with infile");
				System.exit(0);
			}
			tx.success();
		}finally{
			tx.finish();
		}
	
		
		System.out.println("node run through");
		HashMap<String,Node> idnodemap = new HashMap<String,Node>();
		int count = 0;
		int acount = 0;
		tx=graphDb.beginTx();
		try{
			for(int i=0;i<idlist.size();i++){
				String curid = idlist.get(i);
				boolean badpath = false;
				Node curidbarrier = null;
				ArrayList<String> path1 = new ArrayList<String>();
				while(curid.equals(roottaxid)==false){
					if(idparentmap.containsKey(curid)==false){
						badpath = true;
						break;
					}else{
						if(taxcontainedbarriersmap.containsKey(curid))
							curidbarrier = taxcontainedbarriersmap.get(curid);
						curid = idparentmap.get(curid);
						path1.add(curid);
					}
				}
				curid = idlist.get(i);
				if(curidbarrier == null){
					curidbarrier = rootbarrier;
				}
				//			System.out.print(curidbarrier);
				if(badpath){
					System.out.println("bad path: "+idlist.get(i)+" "+idnamemap.get(curid));
					continue;
				}
				//get any hits
				IndexHits<Node> hits = taxNodeIndex.get("name", idnamemap.get(curid));
				try{
					if(hits.size()==0){//no hit
						Node newnode = graphDb.createNode();
						newnode.setProperty("name", idnamemap.get(curid));
						if(curidbarrier != null){
							if(curidbarrier.hasProperty("taxcode"))
								newnode.setProperty("taxcode", (String)curidbarrier.getProperty("taxcode"));
						}
						taxNodeIndex.add( newnode, "name", idnamemap.get(curid));
						idnodemap.put(curid,newnode);
						acount += 1;
//						System.out.println("new name: "+idnamemap.get(curid));
					}else{
						Node bestnode = null;
						for(Node node:hits){
							if (node.hasProperty("taxcode")){
								if(curidbarrier != null){
									if(curidbarrier.hasProperty("taxcode"))
										if (node.getProperty("taxcode").equals(curidbarrier.getProperty("taxcode")))
											bestnode = node;
								}else{
									//System.out.println("name: "+idnamemap.get(curid)+" "+((String)node.getProperty("barrier")));
									//these are often problems
								}
							}else{
								if(node.hasProperty("taxcode") == false)
									if(curidbarrier != null){
										if(curidbarrier.hasProperty("taxcode") == false)
											bestnode = node;
									}else{
										bestnode = node;
									}
							}
						}
						if(bestnode == null){
							Node newnode = graphDb.createNode();
							newnode.setProperty("name", idnamemap.get(curid));
							if(curidbarrier != null){
								if(curidbarrier.hasProperty("taxcode"))
									newnode.setProperty("taxcode", (String)curidbarrier.getProperty("taxcode"));
							}
							taxNodeIndex.add( newnode, "name", idnamemap.get(curid));
							idnodemap.put(curid,newnode);
							System.out.println("node to make " +idnamemap.get(curid) + " "+curidbarrier+ " "+curid);
							acount += 1;
						}else{
							idnodemap.put(curid,bestnode);
						}
					}
				}finally{
					hits.close();
				}
				count += 1;
				if (count % 10000 == 0)
					System.out.println(count+" "+acount);
			}
			tx.success();
		}finally{
			tx.finish();
		}

		System.out.println("relationship run through");
		tx=graphDb.beginTx();
		try{
			count = 0;
			for(int i=0;i<idlist.size();i++){
				String curid = idlist.get(i);
				if(idparentmap.containsKey(curid)){//not the root
					//get the node
					//make the relationship
					Node tnode = idnodemap.get(curid);
					Node tnodep = idnodemap.get(idparentmap.get(curid));
					Relationship rel = tnode.createRelationshipTo(tnodep, RelTypes.TAXCHILDOF);
					rel.setProperty("source", sourcename);
					rel.setProperty("childid", curid);
					rel.setProperty("parentid", idparentmap.get(curid));
					count += 1;
					if (count % 100000 == 0)
						System.out.println(count);
				}
			}
			tx.success();
		}finally{
			tx.finish();
		}
		//synonym processing
		tx=graphDb.beginTx();
		try{
			if(synFileExists){
				System.out.println("synonyms processing");
				for(int i=0;i<idlist.size();i++){
					String first = idlist.get(i);
					if(synonymhash.get(first)!=null){
						Node tnode = idnodemap.get(first);
						ArrayList<ArrayList<String>> syns = synonymhash.get(first);
						for(int j=0;j<syns.size();j++){
							String synName = syns.get(j).get(0);
							String synNameType = syns.get(j).get(1);
							Node synode = graphDb.createNode();
							synode.setProperty("name",synName);
							synode.setProperty("nametype",synNameType);
							synode.setProperty("source",sourcename);
							synode.createRelationshipTo(tnode, RelTypes.SYNONYMOF);
							synNodeIndex.add(synode, "name", synName);
						}
					}
				}
			}
			tx.success();
		}finally{
			tx.finish();
		}
	}
	
	public void runittest(String filename,String filename2){

	}
	
	public static void main( String[] args ){
		System.out.println( "unit testing taxonomy loader" );
		String DB_PATH ="/home/smitty/Dropbox/projects/AVATOL/graphtests/neo4j-community-1.8.M02/data/graph.db";
		TaxonomyLoader a = new TaxonomyLoader(DB_PATH);
		//String filename = "/media/data/Dropbox/projects/AVATOL/graphtests/taxonomies/union4.txt";
		String filename =  "/home/smitty/Dropbox/projects/AVATOL/graphtests/taxonomies/col_acc.txt";
		String filename2 = "/home/smitty/Dropbox/projects/AVATOL/graphtests/taxonomies/ncbi_no_env_samples.txt";
		a.runittest(filename,filename2);
		System.exit(0);
	}
}
