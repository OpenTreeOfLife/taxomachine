package opentree.tnrs;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.HashSet;
import javax.net.ssl.SSLException;
import javax.ws.rs.core.MediaType;
import opentree.taxonomy.TaxonomyExplorer;
import opentree.tnrs.adaptersupport.gnr.*;
import opentree.tnrs.adaptersupport.iplant.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.Node;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * @author Cody Hinchliff
 * 
 *         Provides methods for performing TNRS queries given a search string according to various options.
 */

public class TNRSQuery {

    public final TNRSAdapterGNR GNR = new TNRSAdapterGNR();
    public final TNRSAdapteriPlant IPLANT = new TNRSAdapteriPlant();

    private static final long RETRY_INTERVAL = 1000L;
    private static final long MAX_TIME = 30000L;
    
    private String _graphName;
    private TaxonomyExplorer _taxonomy;
    private TNRSMatchSet _results;
    private HashSet<String> _unmatchedNames;

    public TNRSQuery(String graphName) {
        _graphName = graphName;
        _taxonomy = new TaxonomyExplorer(_graphName);
    }

    /**
     * @param searchString
     * @param adapters
     * @return results
     * 
     *         Performs a TNRS query on 'searchString', and loads the results of the query into a TNRSMatchSet object called 'results', which is returned.
     *         'adapters' is an array of TNRSAdapter objects designating the sources which should be queried. If no adapters are passed, then the query is just
     *         performed against the local taxonomy graph.
     */
    public TNRSMatchSet getMatches(String[] searchStrings, TNRSAdapter... adapters) {
        _results = new TNRSMatchSet(_graphName);
        _unmatchedNames = new HashSet<String>();

        for (int i = 0; i < searchStrings.length; i++) {
            String thisName = searchStrings[i];
            Node matchedNode = _taxonomy.findTaxNodeByName(thisName);

            // first: check the graph index (simple)
            if (matchedNode != null) {
                _results.addMatch(new TNRSDirectMatch(thisName, matchedNode));
                continue;
            }

            // second: direct matches to synonyms within the graph (need to interface with stephen)
            // third: fuzzy matching within the graph (could be fuzzy matches to junior synonyms or recognized taxa)

            // remember names we can't match
            if (_unmatchedNames.contains(thisName) == false)
                _unmatchedNames.add(thisName);
        }

        System.out.println("checkpoint 1");

        // fourth: call passed adapters for help with names we couldn't match
        if (_unmatchedNames.size() > 0) {
            for (int i = 0; i < adapters.length; i++) {
                adapters[i].doQuery(_unmatchedNames);
            }
        }
        return _results;
    }

    public HashSet<String> getUnmatchedNames() {
        return _unmatchedNames;
    }

    private class TNRSAdapterGNR extends TNRSAdapter {

        String url = "http://resolver.globalnames.org/name_resolvers.json";

        public void doQuery(HashSet<String> searchStrings) {
            int id = 0; // just a placeholder

            System.out.println("gnr checkpoint 1");

            // build the query
            String queryString = "{\"data\":\"";
            boolean isFirst = true;
            for (String s : searchStrings) {
                if (isFirst)
                    isFirst = false;
                else
                    queryString += "\n";

                queryString += id + "|" + s;
            }
            queryString += "\"}";

            System.out.println(queryString);

            // set up the connection to GNR
            ClientConfig cc = new DefaultClientConfig();
            Client c = Client.create(cc);
            WebResource gnr = c.resource(url);

            // send the query (get the response)
            String respJSON = gnr.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).post(String.class, queryString);

            // System.out.println(respJSON);

            // parse the JSON response
            GNRResponse response = null;
            ObjectMapper mapper = new ObjectMapper();
            try {
                response = mapper.readValue(respJSON, GNRResponse.class);
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("gnr checkpoint 2");

            for (GNRNameResult thisNameResult : response) {
                System.out.println(thisNameResult.supplied_name_string);
                for (GNRMatch thisMatch : thisNameResult) {
                    String matchedName = thisMatch.canonical_form;
                    boolean thisNameMatched = false;
                    if (matchedName != null) {
                        Node matchedNode = _taxonomy.findTaxNodeByName(thisMatch.canonical_form);
                        if (matchedNode != null) {
                            thisNameMatched = true;
                            thisMatch.matchedNode = matchedNode;
                            thisMatch.searchString = thisNameResult.supplied_name_string;
                            _results.addMatch(thisMatch);
                        } else {
                            // check if it matches a synonym
                        }
                    }
                    if (thisNameMatched) {
                        for (String s : _unmatchedNames) {
                            if (s.compareTo(matchedName) == 0) {
                                _unmatchedNames.remove(s);
                            }
                        }
                    }
                }
            }
        }
    }

    private class TNRSAdapteriPlant extends TNRSAdapter {
        String submiturl = "http://api.phylotastic.org/tnrs/submit";
        String retrieveurl = "http://api.phylotastic.org/tnrs/retrieve/";

        public void doQuery(HashSet<String> searchStrings) {

            System.out.println("iplant checkpoint 1");

            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {

                public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                    if (executionCount >= 5) {
                        // Do not retry if over max retry count
                        return false;
                    }
                    if (exception instanceof InterruptedIOException) {
                        // Timeout
                        return false;
                    }
                    if (exception instanceof UnknownHostException) {
                        // Unknown host
                        return false;
                    }
                    if (exception instanceof ConnectException) {
                        // Connection refused
                        return false;
                    }
                    if (exception instanceof SSLException) {
                        // SSL handshake exception
                        return false;
                    }
                    HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
                    boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                    if (idempotent) {
                        // Retry if the request is considered idempotent
                        return true;
                    }
                    return false;
                }

            };
            httpclient.setHttpRequestRetryHandler(myRetryHandler);

            // build name string for request
            String names = "";
            boolean first = true;
            for (String n : searchStrings) {
                if (first)
                    first = false;
                else
                    names += "\n";
                names += n;
            }
            names = java.net.URLEncoder.encode(names);
            System.out.println(submiturl + "?query=" + names);
            // submit request, retrieve response
            HttpGet httpGet = new HttpGet(submiturl + "?query=" + names);
            try {
                HttpResponse response1 = httpclient.execute(httpGet);

                // The underlying HTTP connection is still held by the response object
                // to allow the response content to be streamed directly from the network socket.
                // In order to ensure correct deallocation of system resources
                // the user MUST either fully consume the response content or abort request
                // execution by calling HttpGet#releaseConnection().

                try {
                    // get the response entity
                    HttpEntity entity1 = response1.getEntity();
                    // load the response string
                    String statusJSON = EntityUtils.toString(entity1);
                    // ensure the response is fully consumed
                    EntityUtils.consume(entity1);

                    // attempt to map the JSON into an status message object
                    ObjectMapper mapper = new ObjectMapper();
                    String tag = null;
                    try {
                        StatusMessage status = mapper.readValue(statusJSON, StatusMessage.class);
                        tag = status.message.split("\\s")[1];
                    } catch (JsonParseException e) {
                        System.out.println(statusJSON);
                        // if it fails, we might need to try a different object...
                        e.printStackTrace();
                    } catch (JsonMappingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    System.out.println(retrieveurl + tag);
                    HttpGet httpGet2 = new HttpGet(retrieveurl + tag);
                    HttpResponse response2;
                    HttpEntity entity2 = null;
                    String resultJSON;

                    for (int i = 0; i < MAX_TIME / RETRY_INTERVAL; i++) {
                        try {

                            // get the response entity
                            response2 = httpclient.execute(httpGet2);
                            entity2 = response2.getEntity();

                            // load the response string
                            resultJSON = EntityUtils.toString(entity2);
//                            System.out.println(resultJSON);
                            iPlantResponse response = mapper.readValue(resultJSON, iPlantResponse.class);

                            for (iPlantNameResult thisNameResult : response) {
                                String thisSearchString = thisNameResult.submittedName;
                                boolean thisNameMatched = false;
                                for (iPlantMatch thisMatch : thisNameResult) {
                                    Node matchedNode = _taxonomy.findTaxNodeByName(thisMatch.acceptedName);
                                    if (matchedNode != null) {
                                        thisMatch.matchedNode = matchedNode;
                                        thisMatch.searchString = thisSearchString;
                                        thisNameMatched = true;
                                        _results.addMatch(thisMatch);
                                    }
//                                    System.out.println(thisMatch.acceptedName);
                                }
                                if (thisNameMatched) {
                                    for (String s : _unmatchedNames) {
                                        if (s.compareTo(thisSearchString) == 0) {
                                            _unmatchedNames.remove(s);
                                        }
                                    }
                                }
                            }
                            
                            break;

                        } catch (JsonParseException e) {
                            e.printStackTrace();
                        } catch (JsonMappingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Thread.sleep(RETRY_INTERVAL);
                        if (i == (MAX_TIME / RETRY_INTERVAL) - 1) {
                            System.out.println("The request timed out or the query was malformed.");
                        }
                    }

                    // ensure the response is fully consumed
                    EntityUtils.consume(entity2);

                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    httpGet.releaseConnection();
                }

            } catch (ClientProtocolException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
