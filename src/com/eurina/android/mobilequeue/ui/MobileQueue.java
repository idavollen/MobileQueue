/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eurina.android.mobilequeue.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.eurina.android.mobilequeue.R;
import com.eurina.android.mobilequeue.model.MQ_Elm;
import com.eurina.android.mobilequeue.model.MobileQueueModel;
import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQElmLoadingObserver;
import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQLocaleManager;
import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQPrefsManager;

/**
 * 
 */
public class MobileQueue {
    class MQPrefsManagerImpl implements MMQPrefsManager {
        
        private SharedPreferences mPrefs = null;
        
        private static final String MQ_PREFS_FILE = "prefs.mq";
        
        public MQPrefsManagerImpl(final Context application) {
            if (application == null)
                throw new IllegalArgumentException("Application context can not be null!");
            mPrefs = application.getSharedPreferences(MQ_PREFS_FILE, Context.MODE_PRIVATE);
        }

        public boolean setIntPrefs(String key, int val) {
            return putInternalIntPrefs(key, val);
        }

        public int getIntPrefs(String key) {
            int val = mPrefs.getInt(key, 0);
            return val;
        }

        public boolean setStrPrefs(String key, String val) {
            return putInternalStrPrefs(key, val);
        }

        public String getStrPrefs(String key) {
            //PREFERRED_HOSTNAME
            String val = mPrefs.getString(key, null);
            return val;
        }
        
        private boolean putInternalIntPrefs(String key, int val) {
            boolean ret = false;
            if (key != null) {
                SharedPreferences.Editor wrt = mPrefs.edit();
                wrt.putInt(key, val);
                ret = wrt.commit();
            }
            return ret;
        }
        
        private boolean putInternalStrPrefs(String key, String val) {
            boolean ret = false;
            if (key != null) {
                SharedPreferences.Editor wrt = mPrefs.edit();
                wrt.putString(key, val);
                ret = wrt.commit();
            }
            return ret;
        }
        
    }
    
    class MQLocaleManagerImpl implements MMQLocaleManager {
        private Context mContext;

        public MQLocaleManagerImpl(final Context context) {
            mContext = context;
        }
        
        public String getLocaleStr(LOC_Key loc_enum) {
            String ret = "";
            int resId = -1;
            switch (loc_enum) {
                case E_UI_VIEW_SERVICE_GROUP_CAPTION:
                    resId = R.string.mq_model_ui_service_group_caption;
                    break;
                case E_UI_VIEW_SERVICE_GROUP_DETAILS:
                    resId = R.string.mq_model_ui_service_group_details;
                    break;
                case E_UI_VIEW_FAVORITES_CAPTION:
                    resId = R.string.mq_model_ui_favorites_caption;
                    break;
                case E_UI_VIEW_FAVORITES_DETAILS:
                    resId = R.string.mq_model_ui_favorites_details;
                    break;
                case E_UI_TICKET_LABEL_DUE_STR:
                    resId = R.string.mq_model_ui_ticket_lable_due_str;
                    break;
                case E_UI_TICKET_LABEL_OVERDUE_STR:
                    resId = R.string.mq_model_ui_ticket_lable_overdue_str;
                    break;
                
                default:
                    break;
            }
            if (resId != -1) {
                ret = mContext.getString(resId);
            }
            return ret;
        }

        public FileOutputStream getFileForWritting(String filename) throws FileNotFoundException {
            FileOutputStream fOut = null;
            if (filename != null) {
                fOut = mApplication.openFileOutput(filename, Context.MODE_PRIVATE);
            }
            return fOut;
        }

        public FileInputStream getFileForReading(String filename) throws FileNotFoundException {
            FileInputStream fIn = null;
            if (filename != null) {
                fIn = mApplication.openFileInput(filename);
            }
            return fIn;
        }
        
    }
    
    interface MQOperator {
        public abstract Object doAction(Object arg, MMQElmLoadingObserver obs);
    }

    static class MQBackgroundLoader extends AsyncTask<MQ_Elm, String, MQ_Elm>  {

        private MMQElmLoadingObserver mObserver;

        private MobileQueueModel mModel;
        
        private MQOperator mOperator;

        public MQBackgroundLoader(MMQElmLoadingObserver obs, MQOperator mqOperator) {
            mObserver = obs;
            mModel = MobileQueue.getMQModel();
            mOperator = mqOperator;
        }

        @Override
        protected MQ_Elm doInBackground(MQ_Elm... paramArrayOfParams) {
            MQ_Elm elm = paramArrayOfParams[0];
            if (mModel != null && mOperator != null) {
                elm = (MQ_Elm) mOperator.doAction(elm, mObserver);
            }
            return elm;
        }

        /**
         * When finished, display the newly-loaded/opened elm
         * retrieve the json attributes by calling elm.getJSONContents()
         */
        @Override
        protected void onPostExecute(MQ_Elm elm) {
            if (mObserver != null) {
                mObserver.onDataReady(elm);
            }
        }  
    }
    
    //this is the MQ_Elm object pointer to the currently operated item in treeview/listview
    //this static variable will be used between various activities, 
    //but it should be updated ONLY in this class.
    //it might be more efficient than passing object in Parcelable between activities
    private static final ArrayList<MQ_Elm> mCurElms = new ArrayList<MQ_Elm>(); 

    public static final String INDEX = "IDX";

    private static MobileQueueModel mModel = null;
    
    private Context mApplication;
    
    //default value is one minute
    public static int MQ_UPDATE_INTERVAL = 1*60;
    
    private static final int SAM_D_LEE = 9595;
    
    private MMQPrefsManager mPrefsMan;
    
    private MMQLocaleManager mLocaleMan;
    
    public MobileQueue(Context application) {
        mApplication = application;
        if (mModel == null) {
            mPrefsMan = new MQPrefsManagerImpl(mApplication);
            mLocaleMan = new MQLocaleManagerImpl(mApplication);
            mModel = MobileQueueModel.createModel(mPrefsMan, mLocaleMan);
            //TODO, what's the right key name here
            //MobileQueue.MQ_UPDATE_INTERVAL = mPrefsMan.getIntPrefs("MQ_UPDATE_INTERVAL");
        }
    }

    public static MobileQueueModel getMQModel() {
        return mModel;
    }

    public static boolean checkUpdate(String swVersion) {
        if (swVersion != null) {
            return mModel.checkUpdate(swVersion);
        }
        return false;
      
    }
    
    public static boolean startActivityForElm(Activity inActivity, MQ_Elm se, Class<?> cls) {
        boolean ret = false;
        if (se != null && cls != null) {
            int val = MobileQueue.findSelectedItem(se);
            if (val != -1) {
                Intent intent = new Intent(inActivity, cls);
                Bundle bdl = new Bundle();
                bdl.putInt(MobileQueue.INDEX, val);
                intent.putExtras(bdl);
                inActivity.startActivity(intent);
                ret = true;
            }            
        }
        return ret;
    }
    
    public static int findSelectedItem(MQ_Elm se) {
        int idx = -1;
        if (se != null) {
            idx = mCurElms.indexOf(se);
            
            if (idx == -1) {
                if (mCurElms.add(se))
                    idx = mCurElms.size()-1;
            }
        }
        return idx;
    }
    
    public static final MQ_Elm getElmByIndex(Intent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("Param Intent can't be null");
        }
        
        Bundle extras = intent.getExtras();
        if (extras == null) {
            throw new IllegalArgumentException("No extras associated with this intent!");
        }
        int index = extras.getInt(MobileQueue.INDEX);
        MQ_Elm elm = null;
        if (index >= 0 && index < mCurElms.size()) {
            elm = mCurElms.get(index);
        }
        return elm;
    }
    
    static public void loadSelectedElm(MQ_Elm elm, MMQElmLoadingObserver obs) {
        MQBackgroundLoader loader = new MQBackgroundLoader(obs, new MQOperator() {            
            public Object doAction(Object arg, MMQElmLoadingObserver obs) {
                MQ_Elm elm= mModel.loadItem((MQ_Elm) arg, obs);
                return elm;
            }
        }
        );
        loader.execute(elm);
        //try to use AsyncTask.get() to see if it works in the same way.
        //return loader.getResults();
        /**
         * the code above won't work since method execute is not in blocking mode
         * getResults() just return null immediately.
         * therefore we have to program in an event-driven mode, that is,
         * in method AsyncTask::onPostExecute(), caller of this method has to be notified
         * of the results.
         */
    }
    
    static public void loadRootItem(MMQElmLoadingObserver obs) {
        MQBackgroundLoader loader = new MQBackgroundLoader(obs, new MQOperator() {            
            public Object doAction(Object arg, MMQElmLoadingObserver obs) {
                MQ_Elm elm = mModel.loadRootItem(obs);
                return elm;
            }
        }
        );
        loader.execute((MQ_Elm)null);
        //see explanations from method over
        //return loader.getResults();
    }

    static public boolean updateTicketInfo(MQ_Elm elm, MMQElmLoadingObserver obs) {
        boolean ret = false;
        MQBackgroundLoader loader = new MQBackgroundLoader(obs, new MQOperator() {

            public Object doAction(Object arg, MMQElmLoadingObserver obs) {
                mModel.updateTicket((MQ_Elm) arg, null, obs);
                return arg;
            }
        }
        );
        loader.execute(elm);
        return ret;
    }
    
    static public boolean updateQueueInfo(MQ_Elm queue, MMQElmLoadingObserver obs) {
        boolean ret = false;
        MQBackgroundLoader loader = new MQBackgroundLoader(obs, new MQOperator() {
            
            public Object doAction(Object arg, MMQElmLoadingObserver obs) {
                mModel.updateQueue((MQ_Elm) arg, null, obs);
                return arg;
            }
        }
        );
        loader.execute(queue);
        return ret;
    }
    
    static public boolean startTimerEvent(final Runnable operation, int seconds) {
        boolean ret = false;
        if (operation != null && seconds >= 0) {
            Handler hdl = new Handler(){
                /* (non-Javadoc)
                 * @see android.os.Handler#handleMessage(android.os.Message)
                 */
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                    case MobileQueue.SAM_D_LEE:
                        operation.run();
                        break;
                    }
                    super.handleMessage(msg);
                }
            };
            if (seconds == 0) {
                Message msg = hdl.obtainMessage();
                msg.what = MobileQueue.SAM_D_LEE;
                hdl.sendMessage(msg);
            }
            else {
                hdl.postDelayed(operation, seconds*1000);
            }
        }
        return ret;
    }
    
    static public List<MQ_Elm> getMQTickets() {
        if (mModel == null)
            return null;
        return mModel.getTickets();
    }
    
    static public void handleTicketsList(final Activity activity) {
        final List<MQ_Elm> tickets = MobileQueue.getMQTickets();
        
        if (tickets== null || tickets.size() <= 0) {
            return;
        }
        
        final CharSequence arys[]  = new CharSequence[tickets.size()];
        int i = 0;
        for (MQ_Elm elm : tickets) {
            arys[i++] = elm.getName();
        }
        MQUtil.showMQTicketListInDailg(activity, arys, new MQUtil.MMQTicketDialogListener() {
            
            public void onExit(int aCommond, Object aData) {
                if (aCommond == DialogInterface.BUTTON_POSITIVE && aData != null) {
                    Integer idx = (Integer)aData;
                    if (idx != null && idx.intValue() > -1) {
                        MobileQueue.startActivityForElm(activity, tickets.get(idx.intValue()), MQQueueTicket.class);
                    }
                }                    
            }
        });
    }

    public void saveMQData() {
        mModel.saveMQData();
    }
}
