
package com.eurina.android.mobilequeue.model;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQElmLoadingObserver;
import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQLocaleManager;
import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQMonitorListener;



class MQRootElm extends MQ_Elm {
    //these two are the parent items in the UI treeview
    private MQDisplayGroup                  mSrvGrp;
    private MQDisplayGroup                  mFavGrp;

    private static final int K_DOC_ROOT_ID = 0;

    private static final String K_ROOT_ITM_NAME = "ROOT_ITEM";

    private static final String K_ROOT_ITM_DESC = "ROOT ITEM FOR MOBILE QUEUE";

    public MQRootElm() {
        super(MQRootElm.K_DOC_ROOT_ID, MQRootElm.K_ROOT_ITM_NAME, MQRootElm.K_ROOT_ITM_DESC);
        mSrvGrp = null;
        mFavGrp = null;
    }

    public boolean loadDisplayGroups() {
        // first load favorites by reading local file
        boolean ret = readFavorites();

        /*if (ret)*/ {
            try {
                ret = loadServiceGroups();
                m_loaded = true;
            } catch (HttpException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private boolean loadServiceGroups() throws HttpException {
        boolean ret = false;
        if (mSrvGrp == null || !mSrvGrp.isDataReady()) {
            if (mSrvGrp == null) {
                //"Service Groups";
                String name = MobileQueueModel.getLocaleStr(MMQLocaleManager.LOC_Key.E_UI_VIEW_SERVICE_GROUP_CAPTION);
                //"List of groups that provide Mobile Queue service.";
                String desc = MobileQueueModel.getLocaleStr(MMQLocaleManager.LOC_Key.E_UI_VIEW_SERVICE_GROUP_DETAILS);
                mSrvGrp = new MQDisplayGroup(MQDisplayGroup.K_VIEW_DISP_SRV_GRP_ID, name, desc);
                addChild(mSrvGrp);
            }

            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put(".op", "10");
            doPostData(pairs);
        }
        return ret;
    }

    protected boolean handleJSONData(JSONObject root) {
        if (root == null)
            return false;
        try {
            JSONArray grpAry = root.getJSONArray("SERVICEGROUPS");
            if (grpAry != null) {
                for (int i = 0; i < grpAry.length(); i++) {
                    JSONObject grp = grpAry.getJSONObject(i);
                    mSrvGrp.addServiceGroupItem(
                            grp.getInt("ID"), grp.getString("NAME"), grp.getString("ADR"), 
                            grp.getString("TLF"), grp.getString("URL"), grp.getString("DESC"));
                }
                mSrvGrp.m_loaded = true;
            }
        } catch (JSONException e) {
            // 
            e.printStackTrace();
        }
        return true;
    }

    private boolean readFavorites() {
        boolean ret = false;
        if (mFavGrp == null || !mFavGrp.isDataReady()) {
            if (mFavGrp == null) {
                //"Favorites";
                String name = MobileQueueModel.getLocaleStr(MMQLocaleManager.LOC_Key.E_UI_VIEW_FAVORITES_CAPTION);
                //"List of historical Mobile Queue services you used.";
                String desc = MobileQueueModel.getLocaleStr(MMQLocaleManager.LOC_Key.E_UI_VIEW_FAVORITES_DETAILS);

                mFavGrp = new MQDisplayGroup(MQDisplayGroup.K_VIEW_DISP_FAV_GRP_ID, name, desc);
                addChild(mFavGrp);
            }
            //TODO
            //read favorite items from local file
            //PrefMan should be used to read how many items should be kept
            ret = mFavGrp.readFavorites();
        }
        return ret;
    }

    /**
     * this method should be called by UI before the application is about to exit
     * here we only need to write all favorite items held in mFavGrp, MQDisplayGroup, to local file
     * those historical favorite items will be read back next time app starts up
     */
    public void saveData() {
        if (mFavGrp != null) {
            mFavGrp.writeFavorites();
        }

    }

    public MQDisplayGroup getFavsGroup() {
        return mFavGrp;
    }
}

/**
 * class representing the two top-level elements in treeview
 * for mSrvGrp, it holds MQServiceGroup and for mFavGrp, it holds MQFavoriteItem
 * Favorites & Service Groups
 * @author et33168
 *
 */
class MQDisplayGroup extends MQ_Elm {

    static final int K_VIEW_DISP_FAV_GRP_ID = 1;

    static final int K_VIEW_DISP_SRV_GRP_ID = 2;

    static final String K_FAVS_FILE_NAME = "hdata.fav";

    public MQDisplayGroup(int id, String name, String desc) {
        super(id, name, desc);
    }

    public boolean addServiceGroupItem(int id, String name, String adress, String tlf, String url, String desc) {
        if (id != 0 && name != null) {
            MQServiceGroup grp = new MQServiceGroup(id, name, adress, tlf, url, desc);
            return addChild(grp);
        }
        return false;
    }

    public boolean addFavoriteItem(MQFavoriteItem itm) {
        if (itm != null) {
            int s = childrenSize();
            if (s == 0)
                return addChild(itm);
            else if (s > 0) {
                for (int i = 0; i < s; i++) {
                    MQFavoriteItem child = (MQFavoriteItem) getChild(i);
                    if (itm.getAccessedDate().after(child.getAccessedDate())) {
                        return insertChild(i, itm);
                    }
                }
            }
        }
        return false;
    }

    public boolean readFavorites() {
        boolean ret = false;
        String favData = readFavsFile(null);
        if (favData != null && favData.length() > 0) {
            JSONObject json;
            try {
                json = new JSONObject(favData);
                JSONArray itms = json.getJSONArray("ITMS");
                if (itms != null && itms.length() > 0) {
                    for (int i = 0; i < itms.length(); i++) {
                        JSONObject itm = itms.getJSONObject(i);
                        MQFavoriteItem fitm = new MQFavoriteItem(this, itm.getInt("ID"), itm.getString("NM"), itm.getString("DS"), itm.getString("AD"),
                                itm.getInt("PI"), itm.getInt("GI"));
                        ret = addFavoriteItem(fitm);
                    }
                }
            } catch (JSONException e) {
                // 
                e.printStackTrace();
            }
        }

        return ret;
    }

    public boolean writeFavorites() {
        boolean ret = false;
        if (m_id == K_VIEW_DISP_FAV_GRP_ID) {
            int size = MobileQueueModel.getIntPrefs("MAX_FAVS_SIZE");
            size = size == 0? 3 : size;
            size = Math.min(size, childrenSize());
            String json = "";
            for (int i = 0; i < size; i++) {
                MQFavoriteItem itm = (MQFavoriteItem) getChild(i);
                if (i > 0)
                    json += ",\n";
                json += itm.getDetailedInfo();                
            }
            if (!json.equals("")) {
                json = "ITMS:\n[\n" + json + "\n]";
                byte[] inputs;
                try {
                    inputs = json.getBytes("UTF-8");
                    String thedigest = MobileQueueModel.getMD5(inputs);
                    json = "{\nSID:\""+thedigest+"\",\n" + json + "\n}";
                    //write json to local file
                    ret = writeToFavsFile(json, null);                    
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }



    private boolean writeToFavsFile(String data, String file) {
        boolean ret = false;
        if (data != null && data.length() > 0) {
            try {
                String fn = file != null ? file : MQDisplayGroup.K_FAVS_FILE_NAME;
                FileOutputStream outFile = MobileQueueModel.getFileForWritting(fn);
                DataOutputStream dos = new DataOutputStream(outFile);
                dos.writeBytes(data);
                dos.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            ret = true;
        }
        return ret;
    }

    private String readFavsFile(String file) {
        StringBuilder contents = new StringBuilder();
        try {
            String fn = file != null ? file : MQDisplayGroup.K_FAVS_FILE_NAME;
            FileInputStream inFile = MobileQueueModel.getFileForReading(fn);
            if (inFile != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(inFile));
                String line = null;
                while ((line = br.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
                br.close();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return contents.toString();
    }
}

class MQFavoriteItem extends MQ_Elm {
    private int m_grpid;
    private int m_pvdid;
    private Date m_accessed;
    private MQServiceQueue m_queue;

    public MQFavoriteItem(MQDisplayGroup parent, int id, String name, String desc, String accessed, int pvdid, int grpid) {
        super(id, name, desc);
        if (accessed != null) {
            m_accessed = new Date(Date.parse(accessed));
        }
        m_grpid = grpid;
        m_pvdid = pvdid;
        m_queue = new MQServiceQueue(parent.getId() - m_id, name, desc);
        m_queue.setGroupId(grpid);
    }

    public MQFavoriteItem(MQDisplayGroup favs, MQ_Elm queue) {
        super(favs.m_id - queue.m_id, queue.getName(), queue.getDesc());
        setQueue(queue);
        m_accessed = new Date();
        //addFavoriteItem should be called after m_accessed is initialized.
        favs.addFavoriteItem(this);
    }

    public String getDetailedInfo() {
        //get JSON presentation of this class
        String jsonStr = "{\n" +
        "ID:"+m_id+
        ", NM:\""+m_name+"\""+
        ", DS:\""+m_desc+"\""+
        ", AD:\""+m_accessed+"\""+
        ", GI:"+m_grpid+""+
        ", PI:"+m_pvdid+""+
        "\n}";
        return jsonStr;
    }

    public MQ_Elm getChild(int index) {
        if (index >= 0 && index < 1) {
            return m_queue;
        }
        return null;
    }

    public Date getAccessedDate() {
        return m_accessed;
    }

    private int getQueueId() {
        int id = getParent().getId() - m_id;
        return id;
    }

    boolean loadData() {
        if (!m_loaded) {
            /**
             * 1. check if the queue to be loaded is already loaded by its provider
             * 1.1 if so, just associate m_queue with it
             * 1.2 else, load it here
             * 2. how to inform its provider of its availability?
             * 2.1 for its provider might not be available yet.
             */
            MQServiceQueue queue = (MQServiceQueue) MobileQueueModel.getInstance().getInternalDoc().getElmById(getQueueId(), MQServiceQueue.class);

            if (queue != null) {
                m_queue = queue;
            } else {
                //                Map<String, String> pairs = new HashMap<String, String>();
                //                //get the concrete queue data, DON'T SEND q=1 PARAM here
                //                pairs.put(".op", "40");
                //                pairs.put("qid", new Integer(getQueueId()).toString());
                //                doPostData(pairs);
                m_queue.addLoadingObserver(m_loading_observers.get(0));
                m_loading_observers.clear();
                MQServiceProvider provider = (MQServiceProvider) MobileQueueModel.getInstance().getInternalDoc().getElmById(m_pvdid, MQServiceProvider.class);
                if (provider != null) {
                    provider.addChild(m_queue);
                }
                m_queue.loadData();
            }
            m_loaded = true;
            setAccessed(Calendar.getInstance().getTime());
            m_next_step = KNextStep.ENEXT_STEP_QUEUE;
            m_loaded = true;
        } else {
            setAccessed(Calendar.getInstance().getTime());
        }

        return true;
    }   


    protected boolean handleJSONData(JSONObject root) {
        if (root == null)
            return false;
        try {
            JSONArray qds = root.getJSONArray("QDS");
            if (qds != null) {
                JSONObject qd = qds.getJSONObject(0);
                m_queue = new MQServiceQueue(getQueueId(), m_name, m_desc);
                m_queue.updateQueueInfo(qd);
                JSONArray qdBtns = qd.getJSONArray("BTNS");
                for (int i = 0; i < qdBtns.length(); i++) {
                    JSONObject jbtn = qdBtns.getJSONObject(i);
                    MQServiceButton mbtn = new MQServiceButton(
                            jbtn.getInt("ID"), jbtn.getString("BTNTXT"), jbtn.getString("BTNTITLE"), jbtn.getString("BTNEVT"));
                    m_queue.addChild(mbtn);
                }
                //here we have to add this newly loaded queue to our internal queue group->provider->queue structure
                //but lack of provider info, this queue is becoming orphan
                m_next_step = KNextStep.ENEXT_STEP_QUEUE;
                m_loaded = true;
            }
        } catch (JSONException e) {
            // 
            e.printStackTrace();
        }
        return true;
    }

    void setAccessed(Date time) {
        m_accessed = time;
        //since its accessed time is changed, 
        //it has to be reinserted into its parent
        MQDisplayGroup p = (MQDisplayGroup) getParent();
        p.removeChild(this);
        p.addFavoriteItem(this);        
    }

    public void setQueue(MQ_Elm queue) {
        if (queue instanceof MQServiceQueue) {
            m_queue = (MQServiceQueue) queue;
            MQ_Elm parent = queue.getParent();
            m_pvdid = parent.getId();
            m_grpid = parent.getParent() != null ? parent.getParent().getId() : 0;
        }

    }


}
/**
 * this class represents the service group, like, Post office, Apotek1
 * it holds list of the concrete service providers, posten has "Posten Manglerud", "Posten sentrum", etc, etc
 * @author ET33168
 *
 */
class MQServiceGroup extends MQ_Elm {
    private String m_tlf;

    private String m_adress;

    private String m_url;

    public MQServiceGroup(int id, String name, String adress, String tlf, String url, String desc) {
        super(id, name, desc);
        m_tlf = tlf;
        m_adress = adress;
        m_url = url;
    }

    boolean loadData() {
        if (!m_loaded) {
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put(".op", "20");
            pairs.put("grpid", new Integer(m_id).toString());
            doPostData(pairs);
        }
        return true;
    }

    protected boolean handleJSONData(JSONObject root) {
        if (root == null)
            return false;
        try {
            JSONArray prvdAry = root.getJSONArray("PROVIDERS");
            if (prvdAry != null) {
                for (int i = 0; i < prvdAry.length(); i++) {
                    JSONObject prvd = prvdAry.getJSONObject(i);
                    MQServiceProvider provider = new MQServiceProvider(
                            prvd.getInt("ID"), prvd.getString("NAME"), prvd.getString("ADR"), 
                            prvd.getString("TLF"), prvd.getString("EMAIL"), prvd.getString("URL"), null);
                    addChild(provider);
                }
                m_loaded = true;
            }
        } catch (JSONException e) {
            // 
            e.printStackTrace();
        }
        return true;
    }

    public String getTlf() {
        return m_tlf;
    }

    public String getAdress() {
        return m_adress;
    }

    public String getUrl() {
        return m_url;
    }

}

/**
 * this class represents a concrete service provider, like Posten Manglerud
 * it holds one or multiple MQServiceQueue
 * @author ET33168
 *
 */
class MQServiceProvider extends MQ_Elm {

    private String m_adress;

    private String m_tlf;

    private String m_email;

    private String m_url;

    public MQServiceProvider(int id, String name, String adress, String tlf, String email, String url, String desc) {
        super(id, name, desc);
        m_adress = adress;
        m_tlf = tlf;
        m_email = email;
        m_url = url;
    }

    boolean loadData() {
        if (!m_loaded) {
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put(".op", "30");
            pairs.put("pvdid", new Integer(m_id).toString());
            doPostData(pairs);
        }
        return true;
    }

    protected boolean handleJSONData(JSONObject root) {
        if (root == null)
            return false;
        try {
            JSONArray qiAry = root.getJSONArray("QIS");
            if (qiAry != null) {
                int ql = qiAry.length();
                m_next_step = KNextStep.ENEXT_STEP_UNKNOWN;
                for (int i = 0; i < ql; i++) {
                    JSONObject jqi = qiAry.getJSONObject(i);
                    //here we just fetched id, name, desc associated with this provider for the time time.
                    //the rest important info will be retrieved after the user chooses a concrete queue
                    MQServiceQueue qi = new MQServiceQueue(
                            jqi.getInt("ID"), jqi.getString("NAME"), jqi.getString("DESC"));
                    addChild(qi);
                    /**
                     * don't fetch buttons info now. 
                     * this method should just fetch an overview info for each queue, in a general level
                     * the detailed button info should be fetched when this queue is selected
                     */
                    /*JSONArray qdBtns = jqi.getJSONArray("BTNS");
                    for (int b = 0; b < qdBtns.length(); b++) {
                        JSONObject jbtn = qdBtns.getJSONObject(b);
                        MQServiceButton mbtn = new MQServiceButton(
                                jbtn.getInt("ID"), jbtn.getString("BTNTXT"), jbtn.getString("BTNTITLE"), jbtn.getString("BTNEVT"));
                        qi.addChild(mbtn);
                    }*/
                }
                if (ql > 1) {
                    m_next_step = KNextStep.ENEXT_STEP_PROVIDER;
                } else if (ql == 1) {
                    m_next_step = KNextStep.ENEXT_STEP_QUEUE;
                }
                m_loaded = true;
            }
        } catch (JSONException e) {
            // 
            e.printStackTrace();
        }
        return true;
    }


    public String getTlf() {
        return m_tlf;
    }

    public String getAdress() {
        return m_adress;
    }

    public String getEmail() {
        return m_email;
    }

    public String getUrl() {
        return m_url;
    }
}

/**
 * MQServiceQueue represents a concrete queue. The reason why we need name and desc for it here is that
 * one service provider probably has more than 1 queue. In this case, the UI will have to list up all those
 * queues by listing its name & desc. With this way, it facilitates UI and model because we don't need to
 * have an extra class MQServiceQueueItem that could simply represent the real queue, on the other hand, the 
 * shortcoming is that queue data for all possible queues are fetched and after the eventual queue is chosen, 
 * the queue info is not fresh, therefore it needs an update immediately.
 * @author ET33168
 *
 */

class MQServiceQueue extends MQ_Elm {
    private int m_processing_nr;

    private int m_next_nr;

    private int m_avg_wtime;
    /**
     * this var is used to hold GROUP_ID where this queue belongs to
     * that is, the MQ_ServiceGroup. The reason to keep it is that
     * buttons to a queue can be shared at level of entire group,
     * e.g, for all post sub offices, the queue has the same button caption,
     * therefore we don't need to store a separate row for each sub post office
     * in DB ADM_SERVICE_BUTTON
     */
    private int m_grp_id;

    private MMQMonitorListener m_listener;

    public MQServiceQueue(int id, String name, String desc, int processing_nr, int next_nr, int wtime) {
        super(id, name, desc);
        m_processing_nr = processing_nr;
        m_next_nr = next_nr;
        m_avg_wtime = wtime;
        m_listener = null;
    }    

    public MQServiceQueue(int id, String name, String desc) {
        super(id, name, desc);
        m_processing_nr = 0;
        m_next_nr = 0;
        m_avg_wtime = 0;
        m_grp_id = 0;
        m_listener = null;
    }

    public String getDetailedInfo() {
        String jsonStr = "{\n" +
        "PNR:"+m_processing_nr+
        ", NNR:"+m_next_nr+
        ", AWT:"+(m_next_nr-m_processing_nr)*m_avg_wtime+
        "\n}";
        return jsonStr;
    }

    boolean loadData() {
        if (!m_loaded) {
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put(".op", "40");
            pairs.put("qid", new Integer(m_id).toString());
            if (m_grp_id == 0) {
                MQ_Elm tmp = m_parent;
                do {                    
                    if (tmp != null && tmp instanceof MQServiceGroup) {
                        m_grp_id = tmp.getId();
                        break;
                    }
                    tmp = m_parent.getParent();
                } while (tmp != null);
            }
            pairs.put("grpid", new Integer(m_grp_id).toString());
            doPostData(pairs);
        }
        return true;
    }

    protected boolean handleJSONData(JSONObject root) {
        if (root == null)
            return false;
        try {
            JSONArray qds = root.getJSONArray("QDS");
            if (qds != null) {
                JSONObject qd = qds.getJSONObject(0);
                updateQueueInfo(qd);
                //TODO, what if we don't first call getJSONObject("BTNS")
                JSONArray qdBtns = qd.getJSONArray("BTNS");
                for (int i = 0; i < qdBtns.length(); i++) {
                    JSONObject jbtn = qdBtns.getJSONObject(i);
                    MQServiceButton mbtn = new MQServiceButton(
                            jbtn.getInt("ID"), jbtn.getString("BTNTXT"), jbtn.getString("BTNTITLE"), jbtn.getString("BTNEVT"));
                    addChild(mbtn);
                }
                m_loaded = true;
            }
        } catch (JSONException e) {
            // 
            e.printStackTrace();
        }
        return true;
    }

    void updateQueueInfo(JSONObject jobj) throws JSONException {
        if (jobj != null) {
            updateProcessingNr(jobj.getInt("PNR"));
            updateNextNr(jobj.getInt("NNR"));
            m_avg_wtime = jobj.getInt("AVGWTM");                  
        }
    }

    private void updateProcessingNr(int nr) {
        if (nr > 0 && nr >= m_processing_nr) {
            m_processing_nr = nr;
            if (m_listener != null) {
                m_listener.onProcessingNrChanged(nr);
            }
        } else {
            Log.d("IPARAM", "invalid incoming param:"+nr+" , m_processing_nr = "+m_processing_nr);
            throw new IllegalArgumentException("Invalid processing number");
        }
    }

    private void updateNextNr(int nr) {
        if (nr > 0 && nr >= m_next_nr) {
            m_next_nr = nr;
            if (m_listener != null) {
                m_listener.onNextNrChanged(nr);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    boolean updateQueueData(MMQMonitorListener listener, MMQElmLoadingObserver obs) {
        m_listener = listener;
        addLoadingObserver(obs);

        if (!hasChild()) {
            return loadData();
        }

        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put(".op", "40");
        pairs.put("qid", new Integer(m_id).toString());
        //only need the queue data,
        pairs.put("q", "1");
        pairs.put("fmt", new Integer(MobileQueueNetter.RES_FMT_JSON).toString());
        MobileQueueNetter netter = new MobileQueueNetter(this);
        int sc = netter.postData(pairs);
        if (sc == HttpStatus.SC_OK) {
            String data = netter.readData();
            JSONObject json;
            try {
                json = new JSONObject(data);
                JSONArray qdAry = json.getJSONArray("QDS");
                if (qdAry != null && qdAry.length() > 0) {
                    updateQueueInfo(qdAry.getJSONObject(0));
                }
                //reset listener after updating
                m_listener = null;
            } catch (JSONException e) {
                // 
                e.printStackTrace();
            }
        }
        return false;
    }

    int getPocessingNr() {
        return m_processing_nr;
    }

    int getNextNr() {
        return m_next_nr;
    }

    int getWaitingTime() {
        return m_avg_wtime;
    }

    void setGroupId(int grpid) {
        m_grp_id = grpid;        
    }

}

/**
 * a bit ugly thing with this class is that this class will have the concrete MQServiceTicket as child
 * this weird relationship doesn't make so much sense in the real world, but we needs this kind of relationship with respect to UML
 * because we need the connection between MQServiceQueue and MQServiceTicket with MQServiceButton bridging them
 * in this way, we can simplify the UI, when the user draws a queue ticket by clicking button
 * UI just calls loadItem(button), and don't bother to consider more logics
 * 
 * @author ET33168
 *
 */
class MQServiceButton extends MQ_Elm {
    private String m_post_str;
    //here button holds a ref to MQServiceTicket
    private MQServiceTicket mTicket;

    public MQServiceButton(int id, String caption, String title, String postClickedMsg) {
        super(id, caption, title);
        m_post_str = postClickedMsg;
    }

    public String getDetailedInfo() {
        String jsonStr = "{\n" +
        "POSTEVTSTR:"+m_post_str+
        "\n}";
        return jsonStr;
    }

    boolean loadData() {
        if (!m_loaded) {
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put(".op", "50");
            pairs.put("qid", new Integer(getParent().getId()).toString());
            pairs.put("uid", MobileQueueModel.getCurrentUserId());
            doPostData(pairs);
        }
        return true;
    }

    protected boolean handleJSONData(JSONObject root) {
        if (root == null)
            return false;
        try {
            JSONObject ret = root.getJSONObject("RET");
            if (ret != null) {
                int myNr = ret.getInt("MNR");
                int pnr = ret.getInt("PNR");
                int id = ret.getInt("ID");
                int wtime = ret.getInt("AWT");
                String name = getParent().getName();
                mTicket = new MQServiceTicket(id, name, pnr, myNr, wtime);
                mTicket.setQueue(getParent());
                mTicket.setNotes(m_post_str);
                addChild(mTicket);
                //we can't call addChild() here for it changes its parent to the MQServiceTicket
                //because UI still needs to retrieve MQ_Elm for ticket later via MQ_Elm to MQServiceButton
                //therefore we introduced a new method addTicket to MQServiceUser
                MobileQueueModel.getCurrentUser().addTicket(mTicket);
                //it's time to set its parent, the queue to Favorites
                MobileQueueModel.getInstance().getInternalDoc().updateFavoriteItem(getParent());
                m_loaded = true;
                m_next_step = KNextStep.ENEXT_STEP_TICKET;
            }
        } catch (JSONException e) {
            // 
            e.printStackTrace();
        }
        return true;
    }

}

class MQServiceUser extends MQ_Elm {

    private List<MQ_Elm> m_tickets;

    public MQServiceUser(int id, String name, String desc) {
        super(id, name, desc);
        m_tickets = new ArrayList<MQ_Elm>();
    }

    boolean addTicket(final MQServiceTicket ticket) {
        boolean ret = false;

        if (ticket != null) {
            if (!m_tickets.contains(ticket)) {
                ret = m_tickets.add(ticket);
            }
        }
        return ret;
    }

    public int getTicketSize() {
        return m_children.size();
    }

    public List<MQ_Elm> getTickets() {
        return m_tickets;
    }
}

class MQServiceTicket extends MQ_Elm {
    private static final int                K_TICKET_STATUS_DUE = 1;
    private static final int                K_TICKET_STATUS_OVERDUE = 2;


    private int m_processing_nr;

    private int m_my_nr;

    private int m_avg_wtime;

    private long m_drawn_time;

    private int m_status;
    //hold the reminder info for a ticket, i.e, which documents a applicant should take with him
    private String m_notes;

    //we assume the MQServiceUser is its parent
    //but it's also associated with a MQServiceQueue
    private MQServiceQueue m_queue;

    public MQServiceTicket(int id, String name, int processing_nr, int ticket_nr, int wtime) {
        //here name, "Queue" + id, should be the same as the queue name
        //for it'll be displayed on appWidget, therefore meaningful
        super(id, name, "class MQServiceQueue for " + id);
        m_processing_nr = processing_nr;
        m_my_nr = ticket_nr;
        m_avg_wtime = wtime;
        m_drawn_time = System.currentTimeMillis();
        m_status = ticket_nr >= processing_nr ? K_TICKET_STATUS_DUE : K_TICKET_STATUS_DUE;
    }

    public void setNotes(String notes) {
        m_notes = notes;        
    }

    public void setQueue(MQ_Elm queue) {

        if (queue != null && queue instanceof MQServiceQueue)
            m_queue = (MQServiceQueue) queue;        
    }

    public String getDetailedInfo() {
        String status = "";

        if (m_status == MQServiceTicket.K_TICKET_STATUS_DUE) {
            //"Due in %s minutes";
            status = String.format(MobileQueueModel.getLocaleStr(MMQLocaleManager.LOC_Key.E_UI_TICKET_LABEL_DUE_STR), 
                    new Integer((m_my_nr-m_processing_nr)*m_avg_wtime));
        }
        else {
            //"Overdue in %s minutes";
            status = String.format(MobileQueueModel.getLocaleStr(MMQLocaleManager.LOC_Key.E_UI_TICKET_LABEL_OVERDUE_STR), 
                    new Integer((m_processing_nr-m_my_nr)*m_avg_wtime));

        }

        String jsonStr = "{\n" +
        "STS:\""+status+"\""+
        ", PNR:"+m_processing_nr+
        ", MNR:"+m_my_nr+
        ", AWT:"+(m_my_nr-m_processing_nr)*m_avg_wtime+
        ", NTS:\""+m_notes+"\""+
        "\n}";
        return jsonStr;
    }

    public boolean updateInfo(MMQMonitorListener listener, MMQElmLoadingObserver obs) {
        if (m_queue != null && m_status == MQServiceTicket.K_TICKET_STATUS_DUE) {
            m_queue.updateQueueData(null, obs);
            m_processing_nr = m_queue.getPocessingNr();
            m_avg_wtime = m_queue.getWaitingTime();
            m_status = m_my_nr >= m_processing_nr ? K_TICKET_STATUS_DUE : K_TICKET_STATUS_DUE;
            if (listener != null)
                listener.onProcessingNrChanged(m_processing_nr);
        }
        return true;
    }

    private void updateProcessingNr(int pnr) {
        m_processing_nr = pnr;
        if (m_my_nr < m_processing_nr) {
            m_status = MQServiceTicket.K_TICKET_STATUS_DUE;
        } else {
            m_status = MQServiceTicket.K_TICKET_STATUS_OVERDUE;
        }
    }
}
