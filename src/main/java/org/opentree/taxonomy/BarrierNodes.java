package org.opentree.taxonomy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.Traversal;
import org.opentree.taxonomy.contexts.Nomenclature;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;

/**
 * Defines the barrier nodes for the various nomenclatural codes and provides methods for accessing them and their metadata.
 * Would it be useful to have this class implemented as an enum?
 * @author cody
 *
 */
public final class BarrierNodes {

    private static final int LARGE = 100000000;
    private Taxonomy taxonomy;
    private HashMap<Node, Nomenclature> barrierNodeToNomenclatureMap = new HashMap<Node, Nomenclature>();
    private HashMap<Node, String> barrierNodeToBarrierCladeNameMap = new HashMap<Node, String>();
    
    /**
     * Key is node name and value is governing nomenclature.
     */
    private static final HashMap<String, Nomenclature> barrierCladeNameToNomenclatureMap = new HashMap<String, Nomenclature>() {
        private static final long serialVersionUID = 1L;
        {
            put("Fungi",Nomenclature.ICBN);
            put("Viridiplantae",Nomenclature.ICBN);
            put("Bacteria",Nomenclature.ICNB);
            put("Metazoa",Nomenclature.ICZN);
            put("Alveolata",Nomenclature.ICBN);
            put("Rhodophyta",Nomenclature.ICBN);
            put("Glaucocystophyceae",Nomenclature.ICBN);
            put("Haptophyceae",Nomenclature.ICBN);
            put("Choanoflagellida",Nomenclature.ICZN);
            //maybe add Protostomia, see Heterochaeta in ncbi
        }
    };
    
    /**
     * This is in order to match some of the barrier nodes that are in the other taxonomies    
     */
    private static final HashMap<String,HashSet<String>> barrierNamesSearch = new HashMap<String,HashSet<String>>() {
    	private static final long serialVersionUID = 1L;
    	{
    	 put("Metazoa",new HashSet<String>(){{add("Metazoa");add("Animalia");}});
    	 put("Viridiplantae",new HashSet<String>(){{add("Viridiplantae");add("Plantae");}});
    	}
    };

    public BarrierNodes(Taxonomy t) {
        taxonomy = t;
    }
    
    /**
     * Finds the set of nodes matching the barrier names. Because names may be homonyms, we have to use the ones that are closest to the root.
     * 
     * @return barrierNodes
     */
    public void initializeBarrierNodes() {

        Node lifen = taxonomy.getLifeNode();

        // traverse from each barrier node to life and pick the closest one
        PathFinder<Path> tfinder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelType.TAXCHILDOF, Direction.OUTGOING), 10000);
//        ArrayList<Node> barnodes = new ArrayList<Node>();

        for (String itns : barrierCladeNameToNomenclatureMap.keySet()) {
            int bestcount = LARGE;
            Node bestitem = null;
            IndexHits<Node> hits = taxonomy.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME).get("name", itns);
            try {
                for (Node node : hits) {
                	System.out.println(node);
                    Path tpath = tfinder.findSinglePath(node, lifen);
                    if (tpath == null)
                    	continue;
                    int pl = tpath.length();
                    if (pl < bestcount) {
                        bestcount = pl;
                        bestitem = node;
                    }
                }
            } finally {
                hits.close();
            }
            //if there is no match, try to match synonyms if they exist
            if(bestitem == null){
            	if(barrierNamesSearch.containsKey(itns)==true){
            		System.out.println("checking some of the other names for: "+itns);
            		for(String tname: barrierNamesSearch.get(itns)){
            			System.out.println("checking: "+tname);
            			hits = taxonomy.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME).get("name", tname);
                        try {
                            for (Node node : hits) {
                            	System.out.println(node);
                                Path tpath = tfinder.findSinglePath(node, lifen);
                                if (tpath == null)
                                	continue;
                                int pl = tpath.length();
                                if (pl < bestcount) {
                                    bestcount = pl;
                                    bestitem = node;
                                }
                            }
                        } finally {
                            hits.close();
                        }
            		}
            	}
            	
            	if (bestitem == null){
            		System.out.println("trying to match barriers with the synonyms");
            		hits = taxonomy.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_SYNONYM).get("name", itns);
            		bestcount = LARGE;
            		try {
            			for (Node node : hits) {
            				System.out.println(node);
            				Path tpath = tfinder.findSinglePath(node, lifen);
            				if (tpath == null)
            					continue;
            				int pl = tpath.length();
            				if (pl < bestcount) {
            					bestcount = pl;
            					bestitem = node;
            				}
            			}
            		} finally {
            			hits.close();
            		}
            	}
            }

            if (bestitem == null) {
            	System.out.println("Could not find barrier: " + itns);
            	continue;
            }
            
        	System.out.println("Found barrier: " + itns + " " + bestitem.getId());
        	if (barrierNodeToNomenclatureMap.containsKey(bestitem)) {
        		throw new UnsupportedOperationException("The barrier clade \"" + itns + "\" is mapped to the same node as the barrier clade \"" + 
        				barrierNodeToBarrierCladeNameMap.get(bestitem) + ". This is disallowed.");
        	} else {
            	barrierNodeToNomenclatureMap.put(bestitem, barrierCladeNameToNomenclatureMap.get(itns));
            	barrierNodeToBarrierCladeNameMap.put(bestitem, itns);
        	}
        }
    }

    public Map<Node, Nomenclature> getBarrierNodeToNomenclatureMap() {
    	return barrierNodeToNomenclatureMap;
    }
    
    /**
     * Just returns the set of barrier node names.
     * @return barrierNames
     */
    public Set<String> getBarrierCladeNames() {
        Set<String> bn = barrierCladeNameToNomenclatureMap.keySet();
        HashSet<String> barrierNames = new HashSet<String>();
        for (String name : bn) {
            barrierNames.add(name);
        }
        return barrierNames;
    }
    
    /**
     * Returns the set of names that might stand for the barrier node (synonyms)
     */
    public HashMap<String,HashSet<String>> getBarrierCladeNamesSearchMap(){
    	return barrierNamesSearch;
    }

    /*
     * Returns the mapping of barrier node names (keys) to their governing nomenclature (values).
     * @return barrierNodeMap
     */
//    public Map<String,String> getBarrierNodeMap() {
//        return barrierNamesMap;
//    }
    
    /**
     * Checks whether the passed argument `name` is a perfect match to a barrier node name.
     * @return isMatch
     */
    public boolean containsName(String name) {
        return barrierNodeToNomenclatureMap.keySet().contains(name);
    }
}
