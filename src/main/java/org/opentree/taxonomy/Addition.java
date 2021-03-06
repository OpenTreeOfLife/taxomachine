// Compare with smasher

package org.opentree.taxonomy;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.simple.parser.JSONParser; 
import org.json.simple.parser.ParseException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.rest.repr.BadInputException;

public class Addition {

    public static void processAdditionDirectory(String dir, GraphDatabaseService gdb)
        throws IOException, ParseException, BadInputException
    {
        List<File> files = listAdditionDocuments(dir);
        for (File file : files)
            processAdditionDocument(file, gdb);
    }


    public static List<File> listAdditionDocuments(String dir) {
        return listAdditionDocuments(new File(dir));
    }

    public static List<File> listAdditionDocuments(File dir) {
        FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith("additions-") && name.endsWith(".json");
                }
            };
        File[] files = dir.listFiles(filter);
        if (files == null)      // directory doesn't exist
            return new ArrayList<File>();
        List<File> listOfFiles = Arrays.asList(files);
        listOfFiles.sort(compareFiles);
        return listOfFiles;
    }

	static Comparator<File> compareFiles = new Comparator<File>() {
		public int compare(File x, File y) {
            String a = x.getName();
            String b = y.getName();
            int compareLengths = a.length() - b.length();
            if (compareLengths != 0) return compareLengths;
            return a.compareTo(b);
		}
	};

    public static Map<String, Long> processAdditionDocument(File file, GraphDatabaseService gdb)
        throws IOException, ParseException, BadInputException
    {
        BufferedReader fr = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                                                                     "UTF-8"));
		JSONParser parser = new JSONParser();
        Object obj = parser.parse(fr);
        return processAdditionDocument(obj, gdb);
    }

    public static Map<String, Long> processAdditionDocument(String doc, GraphDatabaseService gdb)
        throws ParseException, BadInputException
    {
		JSONParser parser = new JSONParser();
        Object obj = parser.parse(doc);
        return processAdditionDocument(obj, gdb);
    }


    public static Map<String, Long> processAdditionDocument(Object json, GraphDatabaseService gdb)
        throws BadInputException
    {
        Map<String, Long> tagToId = new HashMap<String, Long>();
        List<Long> ids = new ArrayList<Long>();
        TaxonomyLoaderOTT tlo = new TaxonomyLoaderOTT(gdb);

        Transaction tx = gdb.beginTx();
		try {
            tlo.setupIndexes();
            Taxonomy taxonomy = new Taxonomy(gdb);
            if (json instanceof Map) {
                Map top = (Map)json;
                Object taxaObj = top.get("taxa");
                String docId = (String)(top.get("id")); // addition document id
                List taxa = (List)taxaObj;
                for (Object descriptionObj : taxa) {
                    Map description = (Map)descriptionObj;
                    Long ott_id = toId(description.get("ott_id"));
                    String tag = (String)(description.get("tag"));
                    String name = (String)(description.get("name"));
                    Long parentId = toId(description.get("parent"));
                    String parentTag = (String)(description.get("parent_tag"));
                    String rank = (String)(description.get("rank"));
                    List sourceList = (List)(description.get("sources"));
                    List flags = (List)(description.get("flags"));

                    // Don't ever define an id twice!
                    if (taxonomy.getTaxonForOTTId(ott_id) != null)
                        continue;

                    if (ott_id == null)
                        throw new BadInputException(String.format("missing ott_id for %s", tag));
                    ids.add(ott_id);

                    if (name == null)
                        throw new BadInputException(String.format("missing name for %s", tag));

                    // Get parent taxon id
                    if (parentId == null)
                        parentId = tagToId.get(parentTag);
                    if (parentId == null)
                        throw new BadInputException(String.format("missing parent id %s for %s", tag));

                    if (rank == null) rank = "";

                    String sources;
                    if (docId != null)
                        sources = String.format("%s:%s", docId, ott_id);
                    else {
                        StringBuilder sb = new StringBuilder();
                        if (sourceList != null)
                            for (Object sourceObj : sourceList) {
                                Object s = ((Map)sourceObj).get("source");
                                if (s != null) {
                                    if (sb.length() > 0)
                                        sb.append(",");
                                    sb.append((String)s);
                                }
                            }
                        sources = sb.toString();
                    }

                    String[] flagsArray;
                    if (flags == null)
                        flagsArray = new String[] {};
                    else {
                        int size = flags.size();
                        flagsArray = new String[size];
                        int i = 0;
                        for (Object flagObj : flags)
                            flagsArray[i++] = (String)flagObj;
                    }

                    String uniqname = "";

                    tlo.processOTTData(ott_id, parentId, name, rank, sources, uniqname, flagsArray);

                    // For backward references
                    if (tag != null)
                        tagToId.put(tag, ott_id);

                    // Prepare for the relationship-creation pass
                    if (tlo.dbNodeForOTTIdMap.get(parentId) == null) {
                        Taxon match = tlo.getTaxonForOTTId(parentId); // neo4j query
                        if (match != null) {
                            // shows up in data/log/console.log
                            //System.out.format("Parent id %s -> node %s (for %s)\n", parentId, match.getName(), name);
                            tlo.dbNodeForOTTIdMap.put(parentId, match.getNode());
                        } else
                            throw new BadInputException(String.format("no taxon with OTT id %s (parent of %s)",
                                                                      parentId, name));
                    }
                }
            } else
                throw new BadInputException("bad json for addition");
            tx.success();
        } finally {
            tx.finish();
        }

		// process taxon ids for relationships (mainly parent node)
		tx = gdb.beginTx();
		try {
			for (Long id : ids)
				tlo.processOTTRels(id);
			tx.success();
		} finally {
			tx.finish();
		}
		
        return tagToId;
    }

    static Long toId(Object ottIdObj) {
        if (ottIdObj == null)
            return null;
        else if (ottIdObj instanceof Long)
            return ((Long)ottIdObj).longValue();
        else if (ottIdObj instanceof Integer)
            return (long)((Integer)ottIdObj).intValue();
        else
            return Long.parseLong((String)ottIdObj);
    }

}
