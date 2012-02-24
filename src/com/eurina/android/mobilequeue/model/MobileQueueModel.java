
package com.eurina.android.mobilequeue.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * this class encapsulates the data model from the UI/viewer
 * it is instantiated by UI and it has responsibilities to fetch data concerning service groups and detailed
 * services, the last but least, the real time queue info for the selected service.
 *  
 * @author ET33168
 *
 */

public class MobileQueueModel {

    //inform UI of interesting events
    public interface MMQElmLoadingObserver {
        public void onLoadingStarted();

        public void onProgressUpdated(String progress);

        public void onLoadingFinished(boolean failed);
        
        public void onDataReady(MQ_Elm elm);
    }
    
    public interface MMQMonitorListener {
        abstract void onProcessingNrChanged(int new_nr);

        abstract void onNextNrChanged(int new_nr);
    }
    
    //retrieve info from UI
    public interface MMQPrefsManager {
        public static final int K_NOT_FOUND = -1;
        
        abstract public boolean setIntPrefs(String key, int val);
        abstract public int getIntPrefs(String key);
        abstract public boolean setStrPrefs(String key, String val);
        abstract public String getStrPrefs(String key);
    }
    
    public interface MMQLocaleManager {
        public enum LOC_Key {
            E_UI_VIEW_SERVICE_GROUP_CAPTION,
            E_UI_VIEW_SERVICE_GROUP_DETAILS,
            E_UI_VIEW_FAVORITES_CAPTION,
            E_UI_VIEW_FAVORITES_DETAILS,
            E_UI_TICKET_LABEL_DUE_STR,
            E_UI_TICKET_LABEL_OVERDUE_STR,
            
        }
        abstract public String getLocaleStr(LOC_Key loc_enum);
        abstract public FileOutputStream getFileForWritting(String filename) throws FileNotFoundException;
        abstract public FileInputStream getFileForReading(String filename) throws FileNotFoundException;
    }

    private MobileQueueDoc mDoc;
    private MQServiceUser mUser;
    private MMQPrefsManager mPrefsMan;
    private MMQLocaleManager mLocaleMan;
    
    private static MobileQueueModel sInst = null;
    
    public static MobileQueueModel getInstance() {
        return sInst;        
    }
    
    public static MobileQueueModel createModel() {
        if (sInst == null) {
            sInst = new MobileQueueModel(null, null);
        }
        return sInst;        
    }
    
    public static MobileQueueModel createModel(MMQPrefsManager prefsMan, MMQLocaleManager localeMan) {
        if (sInst == null) {
            sInst = new MobileQueueModel(prefsMan, localeMan);
        }
        return sInst;        
    }
    
    private MobileQueueModel(MMQPrefsManager prefsMan, MMQLocaleManager localeMan) {
        mPrefsMan = prefsMan;
        mLocaleMan = localeMan;
        mDoc = new MobileQueueDoc();
        /**
         * 1. retrieve default url from prefMan
         * 2. if null, use the default hostname to check if a new one is available
         * 2.1 if found, set the new one in prefMan and set the static var sHostname in MobileQueueNetter
         * 3. it's time to auth itself
         */
        String hostname = mPrefsMan.getStrPrefs("PREFERRED_HOSTNAME");
        MobileQueueNetter netter = new MobileQueueNetter(null);
        if (hostname == null) {
            hostname = MobileQueueNetter.MAIN_ENTRY_PAGE;
            String query = ".op=HLO_CLT";
            hostname = netter.getPreferedHostname(query);
            mPrefsMan.setStrPrefs("HOSTNAME", hostname);
            MobileQueueNetter.setHostName(hostname);
        }
        authenClient(netter);

    }
    
    public void setPrefsManager(final MMQPrefsManager prefsMan) {
        mPrefsMan = prefsMan;
    }
    
    public void setLocaleManager(final MMQLocaleManager localeMan) {
        mLocaleMan = localeMan;
    }
    
    MobileQueueDoc getInternalDoc() {
        return mDoc;
    }
    
    private boolean authenClient(MobileQueueNetter netter) {
        boolean ret = false;
        if (netter != null) {
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put(".op", "0");
            pairs.put("fmt", new Integer(MobileQueueNetter.RES_FMT_JSON).toString());
            //TODO, how to authenticate itself
            //pairs.put("qid", new Integer(getParent().getId()).toString());
            String response = netter.authClient(pairs);
            if (response != null) {
                try {
                    JSONObject json = new JSONObject(response);
                    String sid = json.getString("MQTEI");
                    MobileQueueNetter.setMQTEI(sid);
                    int uid = json.getInt("UID");
                    mUser = new MQServiceUser(uid, null, null);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            ret = true;
        }
        return ret;
    }
    
    public boolean checkUpdate(String current) {
        boolean ret = false;
        
        return ret;
        
    }


    /**
     * this method is called by UI when user clicks on an item in the treeview/listview
     * the model should decide what action should be taken by basing on the concrete subclass
     * i.e, MQFavoriteItem should open the view for queue status
     *      MQServiceGroup should list the possible service providers
     * @param itm
     * @param obs
     * @return MQ_Elm
     */

    public MQ_Elm loadItem(MQ_Elm itm, MMQElmLoadingObserver obs) {
        if (itm == null)
            return null;
        MQ_Elm elm = itm;
        if (!itm.isDataReady()) {
            itm.addLoadingObserver(obs);
        }
        itm.loadData();   
        return elm;
    }

    public MQ_Elm loadRootItem(MMQElmLoadingObserver obs) {
        MQ_Elm elm = null;
        if (mDoc != null) {
            elm = mDoc.loadDocRoot(obs);
        }
        return elm;
    }

    public boolean updateTicket(MQ_Elm ticket, MMQMonitorListener listener, MMQElmLoadingObserver obs) {
        boolean ret = false;
        if (ticket != null && ticket instanceof MQServiceTicket) {
            MQServiceTicket mqst = (MQServiceTicket) ticket;
            ret = mqst.updateInfo(listener, obs);
        }
        return ret;
    }
    
    public boolean updateQueue(MQ_Elm queue, MMQMonitorListener listener, MMQElmLoadingObserver obs) {
        boolean ret = false;
        if (queue != null && queue instanceof MQServiceQueue) {
            MQServiceQueue mqq = (MQServiceQueue) queue;
            ret = mqq.updateQueueData(listener, obs);
        }
        return ret;
    }
    
    public final List<MQ_Elm> getTickets() {
        return mUser != null? mUser.getTickets() : null;
    }
    
    public void saveMQData() {
        if (mDoc != null) {
            mDoc.onAppExit();
        }
    }

    static String getCurrentUserId() {
        return Integer.toString(getInstance().mUser.getId());
    }

    static MQServiceUser getCurrentUser() {
        return getInstance().mUser;
    }
    
    static int getIntPrefs(String key) {
        return MobileQueueModel.getInstance().mPrefsMan != null ? MobileQueueModel.getInstance().mPrefsMan.getIntPrefs(key) : MMQPrefsManager.K_NOT_FOUND;
    }
    
    static String getLocaleStr(MMQLocaleManager.LOC_Key loc_enum) {
        return MobileQueueModel.getInstance().mLocaleMan != null ? MobileQueueModel.getInstance().mLocaleMan.getLocaleStr(loc_enum) : null;
    }
    
    static public FileOutputStream getFileForWritting(String filename) throws FileNotFoundException {
        return MobileQueueModel.getInstance().mLocaleMan != null ? MobileQueueModel.getInstance().mLocaleMan.getFileForWritting(filename) : null;
    }

    static public FileInputStream getFileForReading(String filename) throws FileNotFoundException {
        return MobileQueueModel.getInstance().mLocaleMan != null ? MobileQueueModel.getInstance().mLocaleMan.getFileForReading(filename) : null;
    }

    static String getMD5(byte[] input) throws NoSuchAlgorithmException {
        String md5 = "";
        if (input.length > 0) {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] mdbytes = md.digest(input);

            //convert the byte to hex format method 1
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            md5 = sb.toString();
        }
        return md5;
    }
}
