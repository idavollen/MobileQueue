
package com.eurina.android.mobilequeue.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 * this interface is used to inform the http data loading status.
 * 
 * @author et33168
 *
 */
abstract class MQHTTPDataLoadObserver {
    abstract void onLoadingStarted();
    abstract void onLoadingProgressed(String progress);
    abstract void onLoadingFinished();
    abstract void onLoadingFailed();
    
}

/**
 * this class has its main duty to communicate with the CGI on server to fetch various types of data
 * should it be scheduled by another thread or run as service provider
 * @author ET33168
 *
 */

class MobileQueueNetter {

    /**
     * this is the entry URL for the first time. In the future, it might have to redirect client to different hosts
     * based on the load balance or location awareness.
     * for the first time, clients will always send request to this entry. based on the source IP or loading balance, 
     * a new hostname will be returned in AUTH step. and the client SHOULD store it for future use
     */
    public static final String MAIN_ENTRY_PAGE = "http://qte.yourtaste.no/mqws.cgi?";
    
    private static final String MQ_USER_AGENT = "MQ-netter(Android, %s)";

    /**
     * Shared buffer used by {@link #getUrlContent(String)} when reading results
     * from an API request.
     */
    private static byte[]           sRecvBuffer = new byte[512];
    
    private static String           sHostName = null;
    
    private static final String     sUserAgent = String.format(MQ_USER_AGENT, android.os.Build.VERSION.RELEASE);
    
    private static String           sSid = null;
    
    private static final int        sDefaultResFormat = MobileQueueNetter.RES_FMT_JSON;
    
    //loading states
    private static final int        STATE_STARTING = 1;
    private static final int        STATE_LOADING = 2;
    private static final int        STATE_RECEIVING = 3;
    private static final int        STATE_DONE = 4;
    
    //response format
    public static final int         RES_FMT_JSON = 1;
    public static final int         RES_FMT_XML = 2;
    
    private HttpClient              mClient = new DefaultHttpClient();
    
    private HttpPost                mHttpPost;
    private HttpGet                 mHttpGet;
    private HttpResponse            mResponse;
    private int                     mState;
    private int                     mResFormat;
    private MQHTTPDataLoadObserver  mObserver;
    
    public MobileQueueNetter(MQHTTPDataLoadObserver obs) {
        //TODO, for the time being, I don't think there is need to have multiple simultaneous connections
        // therefore, I use only static mClient, which is shared by all instances of this class
        // if it turns out to be wrong, we have to instantiate HttpClient for each instance
        
        //sharing one singleton instance of httpstack has caused some mysterious NPE
        mClient = new DefaultHttpClient();
        mState = MobileQueueNetter.STATE_DONE;
        mObserver = obs;
        mResFormat = MobileQueueNetter.RES_FMT_JSON;
    }
    
    public String authClient(Map<String, String> credentials) {
        if (credentials.get("fmt") == null) {
            credentials.put("fmt", new Integer(sDefaultResFormat).toString());
            mResFormat = sDefaultResFormat;
        } else {
            mResFormat = Integer.valueOf(credentials.get("fmt"));
        }
        int sc = postData(credentials);
        
        if (sc == HttpStatus.SC_OK) {
            return readData();
        }
        
        return null;
    }
    
    public String getPreferedHostname(String oldHost) {
        String newHost = MobileQueueNetter.MAIN_ENTRY_PAGE;
        if (oldHost != null) {
            String url = oldHost;
            if (!oldHost.startsWith("http://")) {
                url = MobileQueueNetter.sHostName != null? MobileQueueNetter.sHostName : MobileQueueNetter.MAIN_ENTRY_PAGE;
                url = url+oldHost;
            }
            int sc = 0;
            try {
                sc = loadData(url);
            } catch (HttpException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //TODO, don't use http redirection
            if (sc == HttpStatus.SC_MOVED_PERMANENTLY || sc == HttpStatus.SC_MOVED_TEMPORARILY) {
                //get the new location for 301 or 302
                newHost = mResponse.getLastHeader("Location").getValue();
                //we're not interested in the message, force it to be done
                mState = MobileQueueNetter.STATE_DONE;
            }            
        }
        return newHost;
    }
    
    private int loadData(String url) throws HttpException {  
        int sc = 0;
        if (mState != MobileQueueNetter.STATE_DONE) {
            throw new RuntimeException("found unfinished loading! loading state = " + mState+ ", request is:"+mHttpGet);
        }
        int tmp = url.indexOf("fmt=");
        if (tmp != -1) {
            int end = url.indexOf("&", tmp+4); //4 is for "fmt="
            mResFormat = Integer.valueOf(url.substring(tmp, end));
        }
        mState = MobileQueueNetter.STATE_STARTING; 
        mHttpGet = new HttpGet(url);
        mHttpGet.setHeader("User-Agent", sUserAgent);
        if (sSid != null) {
            mHttpGet.setHeader("Cookie", "MQTEI="+sSid);
        }
        if (mObserver != null) {
            mObserver.onLoadingStarted();
        }
        
        try {
            mState = MobileQueueNetter.STATE_LOADING;
            mResponse = mClient.execute(mHttpGet);
            if (mObserver != null) {
                mObserver.onLoadingProgressed("sending request...");
            }
    
            // Check if server response is valid
            StatusLine status = mResponse.getStatusLine();
            sc = status.getStatusCode();
            if (mObserver != null) {
                mObserver.onLoadingProgressed("has got response");
            }
        } catch (ClientProtocolException e) {
            throw new HttpException("Problem with sending request to server", e);
        } catch (IOException e) {
            throw new HttpException("Problem with sending request to server", e);
        }
        return sc;
    }
    
    public int postData(Map<String, String> pairs) {
        mState = MobileQueueNetter.STATE_STARTING; 
        if (mObserver != null) {
            mObserver.onLoadingStarted();
        }
        String url = MobileQueueNetter.sHostName != null? MobileQueueNetter.sHostName : MobileQueueNetter.MAIN_ENTRY_PAGE;
        mHttpPost = new HttpPost(url);  
        mHttpPost.setHeader("User-Agent", sUserAgent);
        if (sSid != null) {
            mHttpPost.setHeader("Cookie", "MQTEI="+sSid);
        }
        
        try {   
            // Add your data   
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(pairs.size());
            for (String key : pairs.keySet()) {
                if (key.equals("fmt")) {
                    mResFormat = Integer.valueOf(pairs.get(key));
                }
                nameValuePairs.add(new BasicNameValuePair(key, pairs.get(key))); 
            }
            mHttpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));   
      
            // Execute HTTP Post Request 
            mResponse = mClient.execute(mHttpPost);   
            return mResponse.getStatusLine().getStatusCode();
               
        } catch (ClientProtocolException e) {   
            e.printStackTrace(); 
        } catch (IOException e) {   
            e.printStackTrace();   
        }
        return -1;
    }
    
    public String readData() {
        String data = null;
            
        try {
            // Pull content stream from response
            HttpEntity entity = mResponse.getEntity();
            InputStream inputStream = entity.getContent();
            long length = entity.getContentLength();
    
            ByteArrayOutputStream content = new ByteArrayOutputStream();
    
            // Read response into a buffered stream
            long readData = 0;
            int readBytes = 0;
            
            mState = MobileQueueNetter.STATE_RECEIVING;
            while ((readBytes = inputStream.read(sRecvBuffer)) != -1) {
                content.write(sRecvBuffer, 0, readBytes);
                readData += readBytes;
                if (mObserver != null) {
                    String msg = "read "+Long.toString(readData);
                    if (length != -1) {
                        msg += " of "+ length;
                    }
                    msg += " bytes.";
                    mObserver.onLoadingProgressed(msg);
                }
            }
            // Return result from buffered stream
            data = new String(content.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mState = MobileQueueNetter.STATE_DONE;
            if (mObserver != null) {
                mObserver.onLoadingFinished();
            }
            //do we need to reset them
            //mHttpGet = null;
            //mResponse = null;
            
        }
        return data;
        
    }
    
    
    public static void setHostName(String hostname) {
        sHostName = hostname;        
    }
    
    public static void setMQTEI(String sid) {
        sSid = sid;        
    }

    public boolean isByJSON() {
        return mResFormat == MobileQueueNetter.RES_FMT_JSON;
    }

    public boolean isByXML() {
        return mResFormat == MobileQueueNetter.RES_FMT_XML;
    }
    
    



    
        
    
}
