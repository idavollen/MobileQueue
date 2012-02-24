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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eurina.android.mobilequeue.R;
import com.eurina.android.mobilequeue.model.MQ_Elm;
import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQElmLoadingObserver;

/**
 * 
 */
public class MQQueueView extends Activity implements OnClickListener, MMQElmLoadingObserver, OnItemSelectedListener {

    //ImageViews for displaying current processing nr
    private ImageView mProcessNr9;

    private ImageView mProcessNr1;

    private ImageView mProcessNr0;

    //ImageViews for displaying the next available nr
    private ImageView mNextNr9;

    private ImageView mNextNr1;

    private ImageView mNextNr0;
    
    //text view for average waiting minutes
    private TextView mAwtTxt;

    //
    private MQ_Elm mQueueElm;
    //this is the active button that is being clicked
    private MQ_Elm mCurrBtn;
    
    private MQProgressbar mProgressbar;
    
    private boolean mBtnReady;
    //flag used to disable background async task for updating queue info.
    private boolean mInBackground;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.queue_view);
        mInBackground = false;
        
        //instantiate progressbar
        mProgressbar = new MQProgressbar(findViewById(R.id.title_bar), null, null);

        mProcessNr0 = (ImageView) findViewById(R.id.mqvpnr0);
        mProcessNr1 = (ImageView) findViewById(R.id.mqvpnr1);
        mProcessNr9 = (ImageView) findViewById(R.id.mqvpnr9);
        mNextNr0 = (ImageView) findViewById(R.id.mqvnnr0);
        mNextNr1 = (ImageView) findViewById(R.id.mqvnnr1);
        mNextNr9 = (ImageView) findViewById(R.id.mqvnnr9);
        
        mAwtTxt = (TextView) findViewById(R.id.mqvvtnr);
        mQueueElm = MobileQueue.getElmByIndex(getIntent());
        mBtnReady = false;
        ImageView iv = (ImageView) findViewById(R.id.mqv_ttl_lbtn);
           iv.setOnClickListener(this);
        getQueueInfo(true);

    }   
    
    @Override
    protected void onResume() {
        super.onResume();
        mInBackground = false;
    }
    @Override
    protected void onPause() {
        super.onPause();
        mInBackground = true;
    }

    private void buildUpButtons() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.mqTblView);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(android.view.Gravity.RIGHT);
        
        for (MQ_Elm child : mQueueElm.getChildren()) {
            Button btn = new Button(this);
            btn.setText(child.getName());
            btn.setId(child.getId());
            btn.setOnClickListener(this);
            ll.addView(btn);
        }
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = 25;
        layout.addView(ll, rlp);
    }
    
    private boolean getQueueInfo(boolean immediate) {
        if (mInBackground) {
            return false;
        }
        boolean ret = false;
        MobileQueue.startTimerEvent(new Runnable() {

            public void run() {
                MobileQueue.updateQueueInfo(mQueueElm, MQQueueView.this);                
            }
        }, immediate? 0:MobileQueue.MQ_UPDATE_INTERVAL);
        return ret;
    }

    private boolean updateQueueInfo(int processNr, int nextNr, int awt) {
        boolean ret = false;
        
        ret = updateLedNr(processNr, true);
        if (ret) {
            ret = updateLedNr(nextNr, false);
            mAwtTxt.setText(Integer.toString(awt));
        }
        return ret;
    }

    private boolean updateLedNr(int nr, boolean forProcessing) {
        if (nr < 0) {
            return false;
        }
        int idx = 0;
        Resources res = getResources();

        //code below it's a bit stupid!!!
        //find if there exists some ways to optimize it, i.e eval()

        while (nr > 0) {
            int tmp = nr % 10;
            ImageView tmpImg = null;
            int resId = 0;

            switch (tmp) {
            case 0: {
                resId = R.drawable.cl0;
                if (!forProcessing) {
                    resId = R.drawable.dg0;
                }
            }
                break;
            case 1: {
                resId = R.drawable.cl1;
                if (!forProcessing) {
                    resId = R.drawable.dg1;
                }
            }
                break;
            case 2: {
                resId = R.drawable.cl2;
                if (!forProcessing) {
                    resId = R.drawable.dg2;
                }
            }
                break;
            case 3: {
                resId = R.drawable.cl3;
                if (!forProcessing) {
                    resId = R.drawable.dg3;
                }
            }
                break;
            case 4: {
                resId = R.drawable.cl4;
                if (!forProcessing) {
                    resId = R.drawable.dg4;
                }
            }
                break;
            case 5: {
                resId = R.drawable.cl5;
                if (!forProcessing) {
                    resId = R.drawable.dg5;
                }
            }
                break;
            case 6: {
                resId = R.drawable.cl6;
                if (!forProcessing) {
                    resId = R.drawable.dg6;
                }
            }
                break;
            case 7: {
                resId = R.drawable.cl7;
                if (!forProcessing) {
                    resId = R.drawable.dg7;
                }
            }
                break;
            case 8: {
                resId = R.drawable.cl8;
                if (!forProcessing) {
                    resId = R.drawable.dg8;
                }
            }
                break;
            case 9: {
                resId = R.drawable.cl9;
                if (!forProcessing) {
                    resId = R.drawable.dg9;
                }
            }
                break;
            }

            switch (idx) {
            case 0: {
                tmpImg = mProcessNr0;
                if (!forProcessing) {
                    tmpImg = mNextNr0;
                }
            }
                break;
            case 1: {
                tmpImg = mProcessNr1;
                if (!forProcessing) {
                    tmpImg = mNextNr1;
                }
            }
                break;
            case 2: {
                tmpImg = mProcessNr9;
                if (!forProcessing) {
                    tmpImg = mNextNr9;
                }
            }
                break;

            }
            if (tmpImg != null) {
                tmpImg.setImageDrawable(res.getDrawable(resId));
            }
            nr = nr / 10;
            idx++;
        }
        return true;
    }

    public void onClick(View arg0) {
        if (arg0.getId() == R.id.mqv_ttl_lbtn) {
            
        }
        // use the id to tell which button is clicked and then inform model of the concerned button to retrieve the right Q-nr
        for (MQ_Elm btn : mQueueElm.getChildren()) {
            if (btn.getId() == arg0.getId()) {
                mCurrBtn = btn;
                MobileQueue.loadSelectedElm(btn, this);
                return;
            }
        }
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

    
    private void handleElmLoaded(MQ_Elm elm) {

        if (elm != null) {
            if (elm == mCurrBtn) {
                //prepare Intent for showing MQQueueTicket activity
                MQ_Elm ticket = elm.getChild(0);
                if (ticket != null) {
                    MobileQueue.startActivityForElm(this, ticket, MQQueueTicket.class);
                    return;
                }
            } else if (elm == mQueueElm) {
                if (!mBtnReady) {
                    buildUpButtons();
                    mBtnReady = true;
                }
                String data = elm.getDetailedInfo();
                if (data != null) {
                    //extract info
                    JSONObject json;
                    try {
                        json = new JSONObject(data);
                        int pnr = json.getInt("PNR");
                        int nnr = json.getInt("NNR");
                        int awt = json.getInt("AWT");
                        updateQueueInfo(pnr, nnr, awt);
                        TextView tv = (TextView) findViewById(R.id.mqtitle);
                        tv.setText(mQueueElm.getName());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        getQueueInfo(false);
    }

    public void onLoadingFinished(boolean failed) {
        if (mProgressbar != null) {
            mProgressbar.onLoadingFinished(failed);
        }
    }

    public void onDataReady(MQ_Elm elm) {
        if (elm != null) {
            handleElmLoaded(elm);
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d("MQ", "Spinner1: position=" + position + " id=" + id);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        Log.d("MQ", "Spinner1: unselected");
    }

}
