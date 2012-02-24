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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.eurina.android.mobilequeue.R;
import com.eurina.android.mobilequeue.model.MQ_Elm;
import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQElmLoadingObserver;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

/**
 * 
 */
public class MQQueueTicket extends Activity implements MMQElmLoadingObserver, OnClickListener {

    private TextView mDate;

    private TextView mPreceding;
    
    private TextView mTicketNr;

    private TextView mWaitingMin;
    
    private WebView mWebView;
    
    private AdView mAdView;

    private MQ_Elm mTicket;
    
    private MQProgressbar mProgressbar;    
    
    private boolean mInBackground;

    private String m_ticket_notes;

    private static final String MY_AD_UNIT_ID = "a14dbb16cc968c5";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.queue_ticket);
        mWebView = (WebView) findViewById(R.id.webview_ticket_note);
        mAdView = null;
        mInBackground = false;
        
        //instantiate progressbar
        mProgressbar = new MQProgressbar(findViewById(R.id.title_bar), null, null);
        
        
        mTicketNr = (TextView) findViewById(R.id.mqWlmTitle);
        mPreceding = (TextView) findViewById(R.id.mqPrecedFolk);
        mWaitingMin = (TextView) findViewById(R.id.mqWaitMin);
        mDate = (TextView) findViewById(R.id.mqTicketDate);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        mDate.setText(dateFormat.format(calendar.getTime()));
        try {
            mTicket = MobileQueue.getElmByIndex(getIntent());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            mTicket = null;
        }
        updateTicketUIInfo(mTicket);
        startQueueTicketUpdate();        
        //set OnClickListener for two buttons on titlebar        
        ImageView home = (ImageView) findViewById(R.id.title_bar_home);
        home.setClickable(true);
        home.setOnClickListener(this);
        
        ImageView more = (ImageView) findViewById(R.id.mq_ticket_list);
        
        List<MQ_Elm> tickets = MobileQueue.getMQTickets();
        if (tickets != null && tickets.size() > 1) {
            more.setClickable(true);
            more.setOnClickListener(this);
        }
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
        //try to start the queue updating service used by MQQueueWidget
        Intent service = new Intent(this, MQWidgetUpdatingService.class);
        startService(service);
    }

    private boolean startQueueTicketUpdate() {
        if (mInBackground)
            return false;
        boolean ret = false;
        MobileQueue.startTimerEvent(new Runnable() {            
            public void run() {
                MobileQueue.updateTicketInfo(mTicket, MQQueueTicket.this);                
            }
        }, MobileQueue.MQ_UPDATE_INTERVAL);
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
    
    private void handleElmLoaded(MQ_Elm elm) {
        if (elm != null) {
            updateTicketUIInfo(elm);
        }
        startQueueTicketUpdate();
    }

    private void updateTicketUIInfo(MQ_Elm elm) {
        if (elm == null) {
            Log.d("MQQUEUETICKET", "elm to updateTicketUIInfo() is NULL");
            return;
        }
        String data = elm.getDetailedInfo();
        if (data != null) {
            //extract info
            JSONObject json;
            try {
                json = new JSONObject(data);
                String status = json.getString("STS");
                int pnr = json.getInt("PNR");
                int mnr = json.getInt("MNR");
                int awt = json.getInt("AWT");
                m_ticket_notes = json.getString("NTS");
                
                mTicketNr.setText(Integer.toString(mnr));
                mPreceding.setText(mnr > pnr ? Integer.toString(mnr-pnr) : "OVERDUE!");
                mWaitingMin.setText(Integer.toString(awt));
                
                int visible = View.GONE;
                TextView note = (TextView) findViewById(R.id.mqTicketNotes);
                if (m_ticket_notes != null && m_ticket_notes.length() > 0) {                    
                    note.setClickable(true);
                    note.setOnClickListener(this);
                    visible = View.VISIBLE;
                }
                Log.d("MQTKT", "updateTicketUIInfo, visible = "+visible);
                note.setVisibility(visible);
                if (visible == View.GONE) {
                    Log.d("MQTKT", "showing Ads since there is no notes");
                    showAds();
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }        
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

    public void onClick(View view) {
        if (view.getId() == R.id.title_bar_home) {
            Intent intent = new Intent(this, MQQueueMain.class);
            startActivity(intent);
        } else if (view.getId() == R.id.mq_ticket_list) {
            MobileQueue.handleTicketsList(this);
        } else if (view.getId() == R.id.mqTicketNotes) {
            try {
                showTicketNotes(m_ticket_notes);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }


    private void showTicketNotes(String contents) throws UnsupportedEncodingException {
        // show ticket notes in the WebView for 30 seconds, afterwards, showing ad
        if (mWebView != null && contents != null && contents.length() > 0) {   
            toggleFrameView(false);
            //a bit ridiculous hacking here, see http://code.google.com/p/android/issues/detail?id=3552
            String style = "<style>body {font-size: 12px;} li {color: orange;}</style>";
            mWebView.loadData(URLEncoder.encode("<html><meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-16le\"><body>"+style+contents+"</body></html>", "utf-8").replaceAll("\\+"," "), "text/html", "utf-8");
            //start a timer for showing relevant ads
            MobileQueue.startTimerEvent(new Runnable() {                    
                public void run() {
                    showAds();                
                }
            }, 25);            
        }
    }
    
    private void showAds() {
        if (mAdView == null) {
            mAdView = new AdView(this, AdSize.BANNER, MY_AD_UNIT_ID);
            // Lookup your LinearLayout assuming it’s been given
            // the attribute android:id="@+id/mainLayout"
            FrameLayout layout = (FrameLayout)findViewById(R.id.mqTicketFrameLayout);
            // Add the adView to it
            layout.addView(mAdView);
        }
        // Initiate a generic request to load it with an ad
        mAdView.loadAd(new AdRequest());
        toggleFrameView(true);
    }


    private void toggleFrameView(boolean AdMob) {
        if (mAdView != null) {
            mAdView.setVisibility(AdMob ? View.VISIBLE : View.GONE);
        }
        if (mWebView != null) {
            mWebView.setVisibility(!AdMob ? View.VISIBLE : View.GONE);
        }
        
    }
}
