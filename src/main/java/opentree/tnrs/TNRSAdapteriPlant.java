package opentree.tnrs;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLException;

import opentree.NodeIndexDescription;
import opentree.Taxon;
import opentree.Taxonomy;
import opentree.tnrs.TNRSAdapter;
import opentree.tnrs.TNRSHit;
import opentree.tnrs.TNRSMatchSet;
import opentree.tnrs.TNRSNameResult;
import opentree.tnrs.TNRSResults;
import opentree.tnrs.adaptersupport.iplant.StatusMessage;
import opentree.tnrs.adaptersupport.iplant.iPlantHit;
import opentree.tnrs.adaptersupport.iplant.iPlantNameResult;
import opentree.tnrs.adaptersupport.iplant.iPlantResponse;

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
import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

/**
 * Used by TNRSQuery to perform searches against the iPlant TNRS system. This query has a 30-second timeout;
 * it will continue to try and download results every second for 30 seconds after the query is sent, but if
 * no response is received in this time, it will quietly exit.
 * 
 * @author Cody Hinchliff
 *
 */
public class TNRSAdapteriPlant extends TNRSAdapter {

    private static final long RETRY_INTERVAL = 1000L;
    private static final long MAX_TIME = 10000L;
    private static final double TEMP_SCORE = 0.5;

    private static final String submiturl = "http://api.phylotastic.org/tnrs/submit";
    private static final String retrieveurl = "http://api.phylotastic.org/tnrs/retrieve/";
    
    public TNRSAdapteriPlant() {}

    public void doQuery(Set<String> searchStrings, Taxonomy taxonomy, TNRSResults results) {

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
                httpGet = new HttpGet(retrieveurl + tag);
                HttpResponse response2;
                HttpEntity entity2 = null;
                String resultJSON;

                for (int i = 0; i < MAX_TIME / RETRY_INTERVAL; i++) {
                    try {

                        // get the response entity
                        response2 = httpclient.execute(httpGet);
                        entity2 = response2.getEntity();

                        // load the response string
                        resultJSON = EntityUtils.toString(entity2);
                        iPlantResponse response = mapper.readValue(resultJSON, iPlantResponse.class);

                        for (iPlantNameResult thisNameResult : response) {
                            String thisSearchString = thisNameResult.submittedName;
                            boolean thisNameMatched = false;
                            TNRSMatchSet matches = new TNRSMatchSet();
                            
                            Index<Node> prefTaxNodesByName = taxonomy.ALLTAXA.getNodeIndex(NodeIndexDescription.PREFERRED_TAXON_BY_NAME);
                            for (iPlantHit thisHit : thisNameResult) {
                                IndexHits<Node> matchedNodes = prefTaxNodesByName.get("name", thisHit.acceptedName);
                                if (matchedNodes.size() > 0) {
                                    for (Node n : matchedNodes) {
                                        
                                        Taxon matchedTaxon = taxonomy.getTaxon(n);
                                        
                                        // check to see if the fuzzily matched node is a homonym
                                        IndexHits<Node> directMatches = prefTaxNodesByName.get("name", matchedTaxon.getName());
                                        boolean isHomonym = false;
                                        if (directMatches.size() > 1)
                                            isHomonym = true;

                                        // add match
                                        matches.addMatch(new TNRSHit().
                                                setIsApprox(true).
                                                setIsHomonym(isHomonym).
                                                setMatchedTaxon(matchedTaxon).
                                                setOtherData(thisHit.getData()).
                                                setScore(TEMP_SCORE).
                                                setSearchString(thisSearchString).
                                                setSourceName("iplant"));
                                    }
                                    thisNameMatched = true;
                                }
                            }
                            results.addNameResult(new TNRSNameResult(thisSearchString, matches));
                            HashSet<String> toRemove = new HashSet<String>();
                            if (thisNameMatched) {
                                for (String s : searchStrings) {
                                    if (s.compareTo(thisSearchString) == 0) {
                                        toRemove.add(s);
                                        break;
                                    }
                                }
                                for (String s : toRemove) {
                                    searchStrings.remove(s);
                                }
                            }
                        }
                        
                        break;

                    } catch (JsonParseException e) {
                        e.printStackTrace();
                    } catch (JsonMappingException e) {
                        // don't print the stack trace if we got a status message telling us to wait
                        if (e instanceof UnrecognizedPropertyException) {
                            String[] toks = e.getMessage().split("\\s");
                            if (toks.length < 2) {
                                e.printStackTrace();
                            } else if (toks[2].compareTo("\"message\"") != 0) {
                                e.printStackTrace();
                            } else {
                                System.out.println("waiting for response");
                            }
                        }
                        else {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Thread.sleep((long) RETRY_INTERVAL);
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

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}