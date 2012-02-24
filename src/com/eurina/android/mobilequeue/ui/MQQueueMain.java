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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.eurina.android.mobilequeue.R;
import com.eurina.android.mobilequeue.model.MQ_Elm;
import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQElmLoadingObserver;

/**
 * 
 */
public class MQQueueMain extends Activity 
                         implements OnChildClickListener, OnItemClickListener, MMQElmLoadingObserver, OnClickListener {
    private static final String NAME = "NAME";

    private static final String DESC = "DESC";

    private static final String INFO = "INFO";

    private static final String OBJ = "OBJ";

    private MobileQueue mMQ;
    
    private final boolean mMockUI = false;

    private ExpandableListAdapter mAdapter;
    
    private MQProgressbar mProgressbar;
    
    private enum ElmLoadingState {
        MQ_LoadingState_UKNOWN,
        MQ_LoadingSate_ROOT,
        MQ_LoadingSate_ELM
    };
    private ElmLoadingState mLoadingState = ElmLoadingState.MQ_LoadingState_UKNOWN;
    
    private int mUIView;
    
    private ImageButton mTicketsBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.eurina.android.mobilequeue.R.layout.mq_main);
        mMQ = new MobileQueue(getApplicationContext());
        
        //instantiate progressbar
        //mProgressbar = new MQProgressbar(findViewById(R.id.title_bar), (ProgressBar) findViewById(R.id.progress), null);        
        
        doLoadRootElm();
        
//        if (checkUpdate() && askUserActionForUpdate())
//            getUpdate()
       
            
    }
    
    @Override
    public void onDestroy() {
        mMQ.saveMQData();
        super.onDestroy();
    }
    
    private void doLoadRootElm() {
        //first load the root elm, assuming that the first child is for Favorites
        //and the second child is for Service Groups
        mLoadingState = ElmLoadingState.MQ_LoadingSate_ROOT;
        mUIView = 0;
        MobileQueue.loadRootItem(this);
        
        mTicketsBtn = (ImageButton) findViewById(R.id.mq_main_ticket_btn);
        if (mTicketsBtn != null) {
            mTicketsBtn.setOnClickListener(this);
            //mTicketsBtn.setClickable(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
    }
    
    @Override
    public void onNewIntent(Intent aIntent) {
        super.onNewIntent(aIntent);
        setContentView(com.eurina.android.mobilequeue.R.layout.mq_main);
        doLoadRootElm();
        List<MQ_Elm> tickets = MobileQueue.getMQTickets();
        if (tickets != null && tickets.size() > 0) {
            mTicketsBtn.setClickable(true);
        }
    }
    
    private boolean checkUpdate() {
        try {
            PackageManager manager = getPackageManager();
            PackageInfo info;
            info = manager.getPackageInfo(getPackageName(), 0);
            String version = String.format("CV:n%s, v%s", info.packageName, info.versionName);
            return MobileQueue.checkUpdate(version);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } 
        return false;
    }

    /**
     * callback from ExpandableListView when a child item is being clicked
     */
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        boolean ret = false;
        Object child = parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
        @SuppressWarnings("unchecked")
        HashMap<String, Object> cm = (HashMap<String , Object>) child;
        MQ_Elm se = (MQ_Elm) cm.get(MQQueueMain.OBJ);
        
        if (!mMockUI) {        
            if (se != null) {
                mLoadingState = ElmLoadingState.MQ_LoadingSate_ELM;
                mUIView = 0;
                MobileQueue.loadSelectedElm(se, this);
            }
        }
        
        return ret;

    }

        


    /**
     * callback from ListView when an item is being clicked
     */
    public void onItemClick(AdapterView< ? > paramAdapterView, View paramView, int paramInt, long paramLong) {
               
        @SuppressWarnings("unchecked")
        HashMap<String, Object> cm = (HashMap<String, Object>) paramAdapterView.getAdapter().getItem(paramInt);
        MQ_Elm se = (MQ_Elm) cm.get(MQQueueMain.OBJ);
        
        if (se != null) {
            mLoadingState = ElmLoadingState.MQ_LoadingSate_ELM;
            mUIView = 1;
            MobileQueue.loadSelectedElm(se, this);
        }
        
    }

    private void buildHistoryList(List<Map<String, Object>> groupData, List<List<Map<String, Object>>> childData, MQ_Elm favs) {
        Map<String, Object> curGroupMap = new HashMap<String, Object>();
        if (!mMockUI && favs != null) {
            curGroupMap.put(NAME, favs.getName());
            curGroupMap.put(DESC, favs.getDesc());
            
            int nrs = favs.childrenSize();
            
            List<Map<String, Object>> children = new ArrayList<Map<String, Object>>(nrs);
            
            for (int i = 0; i < nrs; i++) {
                MQ_Elm child = favs.getChild(i);
                if (child != null) {
                    Map<String, Object> curChildMap = new HashMap<String, Object>();
                    children.add(curChildMap);
                    curChildMap.put(NAME, child.getName());
                    curChildMap.put(INFO, child.getDesc());
                    curChildMap.put(OBJ, child); 
                }                
            }
            childData.add(children);
            
        } else {
            curGroupMap.put(NAME, getString(R.string.mq_sp_favs));
            curGroupMap.put(DESC, getString(R.string.mq_sp_favs_desc));
    
            List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
            //UDI
            Map<String, Object> curChildMap = new HashMap<String, Object>();
            children.add(curChildMap);
            curChildMap.put(NAME, "UDI");
            curChildMap.put(INFO, "Utlendingsdirektoratet (UDI) i Grønnland");
            curChildMap.put(OBJ, new Object());
            //childData.add(children);
            //Manglerud Postoffice
            curChildMap = new HashMap<String, Object>();
            children.add(curChildMap);
            curChildMap.put(NAME, "Manglerud post office");
            curChildMap.put(INFO, "Posten i Manglerudsenter");
            curChildMap.put(OBJ, new Object());
            //childData.add(children);
            //Trafikanten
            curChildMap = new HashMap<String, Object>();
            children.add(curChildMap);
            curChildMap.put(NAME, "Trafikanten");
            curChildMap.put(INFO, "Trafikanten billetsalg hallen i Oslo S");
            curChildMap.put(OBJ, new Object());
    
            childData.add(children);
        }
        groupData.add(curGroupMap);
    }

    private void buildServiceGroups(List<Map<String, Object>> groupData, List<List<Map<String, Object>>> childData, MQ_Elm grps) {
        Map<String, Object> curGroupMap = new HashMap<String, Object>();
        groupData.add(curGroupMap);
        if (!mMockUI && grps != null) {
            curGroupMap.put(NAME, grps.getName());
            curGroupMap.put(DESC, grps.getDesc());
            
            int nrs = grps.childrenSize();
            
            List<Map<String, Object>> children = new ArrayList<Map<String, Object>>(nrs);
            
            for (int i = 0; i < nrs; i++) {
                MQ_Elm child = grps.getChild(i);
                if (child != null) {
                    Map<String, Object> curChildMap = new HashMap<String, Object>();
                    children.add(curChildMap);
                    curChildMap.put(NAME, child.getName());
                    curChildMap.put(INFO, child.getDesc());
                    curChildMap.put(OBJ, child); 
                }                
            }
            childData.add(children);
        } else {
            curGroupMap.put(NAME, "Service Groups");
            curGroupMap.put(DESC, "Holding a list of available service groups");

            List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
            childData.add(children);
            //Nordea bank
            Map<String, Object> curChildMap = new HashMap<String, Object>();
            children.add(curChildMap);
            curChildMap.put(NAME, "Nordea banker");
            curChildMap.put(INFO, "Nordea bank Norge ASA, det største banken i skandinavian land.");
            //TODO
            //add the concerned class object from Model here for later reference
            curChildMap.put(OBJ, new Object());
            //Apotek1
            curChildMap = new HashMap<String, Object>();
            children.add(curChildMap);
            curChildMap.put(NAME, "Apotek1");
            curChildMap.put(INFO, "Vår kunnskap, din trygghet!");
            curChildMap.put(OBJ, new Object());
            //childData.add(children);
            //UDI
            curChildMap = new HashMap<String, Object>();
            children.add(curChildMap);
            curChildMap.put(NAME, "UDI");
            curChildMap.put(INFO, "Utlendingsdirektoratet (UDI) i Grønnland");
            curChildMap.put(OBJ, new Object());
            //childData.add(children);
            //Manglerud Postoffice
            curChildMap = new HashMap<String, Object>();
            children.add(curChildMap);
            curChildMap.put(NAME, "POSTEN");
            curChildMap.put(INFO, "Posten i Manglerudsenter");
            curChildMap.put(OBJ, new Object());
            //childData.add(children);
            //Trafikanten
            curChildMap = new HashMap<String, Object>();
            children.add(curChildMap);
            curChildMap.put(NAME, "Trafikanten");
            curChildMap.put(INFO, "Trafikanten billetsalg hallen");
            curChildMap.put(OBJ, new Object());
            //childData.add(children);
        }
        
    }

    private boolean handleSelectedChild(MQ_Elm child) {
        boolean ret = false;        
        
        if (child != null) {
            setContentView(com.eurina.android.mobilequeue.R.layout.mobilequeue);
            
            //instantiate progressbar
            mProgressbar = new MQProgressbar(findViewById(R.id.title_bar), null, null);
            
            TextView tv = (TextView) findViewById(R.id.mqtitle);        
            
            if (!mMockUI) {
                ret = updateListView(R.id.mqProviderView, child.getChildren());                  
            } else {
                ListView lv = (ListView) findViewById(R.id.mqProviderView);
                List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
                tv.setText("Posten group");
                
                HashMap<String, Object> curMap = new HashMap<String, Object>();
                curMap.put(MQQueueMain.NAME, "Manglerud posten");
                curMap.put(MQQueueMain.INFO, "sam@uio.no");
                curMap.put(MQQueueMain.OBJ, new Integer(15));
                data.add(curMap);
    
                curMap = new HashMap<String, Object>();
                curMap.put(MQQueueMain.NAME, "Sentrum posten");
                curMap.put(MQQueueMain.INFO, "yue@uio.no");
                curMap.put(MQQueueMain.OBJ, new Integer(25));
                data.add(curMap);
    
                curMap = new HashMap<String, Object>();
                curMap.put(MQQueueMain.NAME, "Kringsjå posten");
                curMap.put(MQQueueMain.INFO, "jon@uio.no");
                curMap.put(MQQueueMain.OBJ, new Integer(55));
                data.add(curMap);
    
                curMap = new HashMap<String, Object>();
                curMap.put(MQQueueMain.NAME, "Grønnland posten");
                curMap.put(MQQueueMain.INFO, "jon@uio.no");
                curMap.put(MQQueueMain.OBJ, new Integer(55));
                data.add(curMap);
                
                // Map data to views defined in simple_list_item_2.xml
                ListAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, new String[] { MQQueueMain.NAME, MQQueueMain.INFO }, new int[] { android.R.id.text1,
                        android.R.id.text2 });
                lv.setAdapter(adapter);
                //lv.setTextFilterEnabled(true);
                lv.setOnItemClickListener(this);
                ret = true;
            }
        }
        return ret;

    }

    public void onLoadingStarted() {
        if (mProgressbar != null) {
            mProgressbar.onLoadingStarted();
        }        
    }

    public void onProgressUpdated(String progress) {
        if (mProgressbar != null) {
            mProgressbar.onProgressUpdated(progress);
        }        
    }
    

    public void onDataReady(MQ_Elm elm) {
        if (elm != null) {
            if (mLoadingState == ElmLoadingState.MQ_LoadingSate_ELM) {
                handleElmLoaded(elm);
            } else if (mLoadingState == ElmLoadingState.MQ_LoadingSate_ROOT) {
                handleRootElmLoaded(elm);
            }  
        }    
        
    }

    public void onLoadingFinished(boolean failed) {        
        if (mProgressbar != null) {
            mProgressbar.onLoadingFinished(failed);
        }
         
    }
    
    private void handleRootElmLoaded(MQ_Elm root) {
        if (root != null && root.hasChild() && root.childrenSize() == 2) {
            List<Map<String, Object>> groupData = new ArrayList<Map<String, Object>>();
            List<List<Map<String, Object>>> childData = new ArrayList<List<Map<String, Object>>>();
            buildHistoryList(groupData, childData, root.getChild(0));
            buildServiceGroups(groupData, childData, root.getChild(1));
    
            // Set up our adapter
            mAdapter = new SimpleExpandableListAdapter(this, groupData, android.R.layout.simple_expandable_list_item_1, new String[] { NAME, DESC }, new int[] { android.R.id.text1,
                    android.R.id.text2 }, childData, android.R.layout.simple_expandable_list_item_2, new String[] { NAME, INFO }, new int[] { android.R.id.text1, android.R.id.text2 });
            ExpandableListView epv = (ExpandableListView) findViewById(com.eurina.android.mobilequeue.R.id.mqMainlist);
            if (epv != null) {
                epv.setAdapter(mAdapter);
                epv.setOnChildClickListener(this);
            }
        }
    }
    
    private void handleElmLoaded(MQ_Elm elm) {
        if (elm != null) {            
            if (elm.getNextStep() == MQ_Elm.KNextStep.ENEXT_STEP_QUEUE) {
                //has only one queue, then just jump the QueueView activity
                MobileQueue.startActivityForElm(this, elm.getChild(0), MQQueueView.class);               
            } else {
                if (mUIView == 1) {
                    //this provider has multiple queues, just list them up
                    updateListView(R.id.mqProviderView, elm.getChildren()); 
                } else if (mUIView == 0) {
                    handleSelectedChild(elm);
                }
            }
        }
    }

    private boolean updateListView(int resViewId, List<MQ_Elm> children) {
        boolean ret = false;
        ListView lv = (ListView) findViewById(resViewId);                
        
        if (lv != null && children != null && children.size() > 0) {
            List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
            for (MQ_Elm elm : children) {
                HashMap<String, Object> curMap = new HashMap<String, Object>();
                curMap.put(MQQueueMain.NAME, elm.getName());
                curMap.put(MQQueueMain.INFO, elm.getDesc());
                curMap.put(MQQueueMain.OBJ, elm);
                data.add(curMap);
            }
            // Map data to views defined in simple_list_item_2.xml
            ListAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, new String[] { MQQueueMain.NAME, MQQueueMain.INFO }, new int[] { android.R.id.text1,
                    android.R.id.text2 });
            lv.setAdapter(adapter);
            //lv.setTextFilterEnabled(true);
            lv.setOnItemClickListener(this);
        }
        return ret;
    }

    public void onClick(View v) {
        // ImageButton
        if (v != null && v.getId() == R.id.mq_main_ticket_btn) {
            MobileQueue.handleTicketsList(this);
        }
        
    }      
}
