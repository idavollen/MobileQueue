
package com.eurina.android.mobilequeue.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQElmLoadingObserver;


public class MQ_Elm extends MQHTTPDataLoadObserver {
    
    public enum KNextStep {
        ENEXT_STEP_UNKNOWN,
        ENEXT_STEP_QUEUE,           //means that the next step is queue
        ENEXT_STEP_TICKET,          //means that the next step is ticket
        ENEXT_STEP_PROVIDER         //means that the next step is provider
    }
    
    protected KNextStep m_next_step;
    //
    protected MQ_Elm m_parent;

    //list of children
    protected List<MQ_Elm> m_children;

    protected int m_id;

    protected String m_name;

    protected String m_desc;

    //observer is added when it's about to load, deleted when it's finished loading
    protected List<MMQElmLoadingObserver> m_loading_observers;

    //only true when its children is loaded, don't consider grandchild
    protected boolean m_loaded;

    public MQ_Elm(int id, String name, String desc) {
        m_id = id;
        m_name = name;
        m_desc = desc;
        m_children = new ArrayList<MQ_Elm>();
        m_loaded = false;
        m_next_step = KNextStep.ENEXT_STEP_UNKNOWN;
        m_loading_observers = new ArrayList<MobileQueueModel.MMQElmLoadingObserver>(2);
    }
    
    /**
     * all child class who needs to provide more detailed info to UI besides id, name, desc should overload this method
     * for example, MQServiceQueue need to provide additional data, like processing_nr, next_nr, waiting time.
     * 
     * The agreed format for data representation is JSON, which means that UI has to know the concrete json key names
     * in order to correctly retrieve a value. It's a flaw in design, however, I can't figure out a better way to do it.
     * Since on UI, it only knows and uses a general interface/class MQ_Elm from model, which encapsulates all concrete sub classes
     * implementation on model side. However, the detailed info is closely associated with a certain sub-class. 
     * 
     * @return boolean
     */
    public String getDetailedInfo() {
        return null;
    }

    /**
     * all child class who needs to load data from internet needs to overload this method
     * 
     * VISIBILITY is set as Package Private here so that this method can't be called directly from UI, 
     * but it's available to model since they reside in the same package 
     * 
     * @return boolean
     */
    boolean loadData() {
        return false;
    }
    
    /**
     * this method should be called by UI after UI calls MobileQueueModel.loadSelectedItem()/loadData()
     * it is used by UI to decide which activity the UI should go further
     * so it should be overloaded by sub classes that handles loading new data after some UI event
     * @return
     */
    public KNextStep getNextStep() {
        return m_next_step;
    }
    
    protected boolean handleJSONData(JSONObject root) {
        return false;
    }
    
    protected boolean handleXMLData(Document root) {
        return false;
    }
    
    protected boolean doPostData(Map<String, String> pairs) {
        if (pairs == null || pairs.isEmpty())
            return false;
        MobileQueueNetter netter = new MobileQueueNetter(this);
        int sc = netter.postData(pairs);
        if (sc == HttpStatus.SC_OK) {
            String data = netter.readData();
            if (netter.isByJSON()) {
                    try {
                        JSONObject json = new JSONObject(data);
                        handleJSONData(json);
                    } catch (JSONException e) {
                        // 
                        e.printStackTrace();
                    }
            } else if (netter.isByXML()) {
                //TODO, check it later
                Document doc = null;//new Document();
                handleXMLData(doc);
            }
        }
        return true;
    }
    
    public boolean addChild(final MQ_Elm child) {
        if (child != null) {
            boolean ret = m_children.add(child);
            if (ret) {
                child.m_parent = this;
            }
        }
        return false;
    }
    
    public boolean insertChild(int index, final MQ_Elm child) {
        boolean ret = false;
        if (child != null) {
            int ofs = 0;
            if (index >= m_children.size())
                return addChild(child);
            else if (index >= 0)
                ofs = index;
            m_children.add(ofs, child);
            child.m_parent = this;
            ret = true;
        }
        return ret;
    }

    public MQ_Elm getParent() {
        return m_parent;
    }
    
    public boolean setParent(final MQ_Elm parent) {
        boolean ret = false;
        //in case that it'd be deleted
        MQ_Elm tmp = this;
        if (parent != null) {
            if (m_parent != null) {
                //what should we do here? how to properly re-parent this child             

                ret = m_parent.removeChild(this);
            }
            if (ret) {
                ret = parent.addChild(tmp);
            }
        }
        return ret;
    }

    public boolean removeChild(MQ_Elm child) {
        if (child != null && m_children.size() > 0) {
            return m_children.remove(child);
        }
        return false;
    }
    
    public MQ_Elm getChild(int index) {
        if (index >= 0 && index < m_children.size()) {
            return m_children.get(index);
        }
        return null;
    }

    public boolean equals(MQ_Elm obj) {
        boolean ret = false;
        if (obj != null) {
            if (this == obj) {
                ret = true;
            } else {
                if (m_id == obj.getId()) {
                    ret = true;
                }
            }
        }
        return ret;
    }
    
    
    public boolean findChild(final MQ_Elm child) {
        if (child != null && m_children.size() > 0) {
            return m_children.indexOf(child) > -1;
        }
        return false;
    }
    
    public MQ_Elm getElmById(int id, Class< ? > cls) {
        MQ_Elm ret = null;
        if (m_id == id && (cls == null || cls != null && this.getClass() == cls )) {
               ret = this;
        } else if (m_children.size() > 0) {
            for (MQ_Elm elm : m_children) {
                ret = elm.getElmById(id, cls);
                if (ret != null) 
                    break;
            }
        }
        return ret;
    }

    public boolean hasChild() {
        return !m_children.isEmpty();
    }
    
    public int childrenSize() {
        return m_children.size();
    }  
    
    public int getId() {
        return m_id;
    }

    public String getName() {
        return m_name;
    }

    public String getDesc() {
        return m_desc;
    }

    public void addLoadingObserver(MMQElmLoadingObserver obs) {
        if (obs != null) {
            m_loading_observers.add(obs);
        }
    }

    public boolean isDataReady() {
        //only true when its children is loaded, don't consider grandchild
        return m_loaded;
    }

    public final List<MQ_Elm> getChildren() {
        return m_children;
    }

    void onLoadingStarted() {
        for (MMQElmLoadingObserver obs : m_loading_observers) {
            obs.onLoadingStarted();
        }
        
    }

    void onLoadingProgressed(String progress) {
        for (MMQElmLoadingObserver obs : m_loading_observers) {
            obs.onProgressUpdated(progress);
        }
        
    }

    void onLoadingFinished() {
        for (MMQElmLoadingObserver obs : m_loading_observers) {
            obs.onLoadingFinished(false);
        }
        m_loading_observers.clear();
    }

    void onLoadingFailed() {
        for (MMQElmLoadingObserver obs : m_loading_observers) {
            obs.onLoadingFinished(true);
        }
        m_loading_observers.clear();
    }

}
