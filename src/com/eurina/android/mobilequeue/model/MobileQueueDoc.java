
package com.eurina.android.mobilequeue.model;

import java.util.Calendar;

import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQElmLoadingObserver;

/**
 * this class represents a virtual MQ document, which holds and maintain the internal data structure
 *  
 * @author ET33168
 *
 */

class MobileQueueDoc {

    private MQRootElm m_doc_root;

    MobileQueueDoc() {
        m_doc_root = new MQRootElm();
    }    

    MQ_Elm loadDocRoot(MMQElmLoadingObserver obs) {
        if (!m_doc_root.isDataReady()) {
            m_doc_root.addLoadingObserver(obs);
            m_doc_root.loadDisplayGroups();
        }
        return m_doc_root;
    }

    void onAppExit() {
        if (m_doc_root != null) {
            m_doc_root.saveData();
        }        
    }

    void updateFavoriteItem(MQ_Elm queue) {
        if (queue != null && m_doc_root != null) {
            MQDisplayGroup favs = m_doc_root.getFavsGroup();
            if (favs != null) {
                int size = favs.childrenSize(), i;
                
                for (i = 0; i < size; i++) {
                    MQFavoriteItem itm = (MQFavoriteItem) favs.getChild(i);
                    if (itm.getId() == (favs.m_id - queue.m_id)) {
                        itm.setAccessed(Calendar.getInstance().getTime());
                        itm.setQueue(queue);
                        break;
                    }
                }
                if (i == size) {                    
                    MQFavoriteItem fitm = new MQFavoriteItem(favs, queue);
                }
                m_doc_root.saveData();
            }
        }
        
    }

    MQ_Elm getElmById(int queueId, Class<?> cls) {
        return m_doc_root.getElmById(queueId, cls);
    }

}
