/*
 * Copyright (C) 2009 The Android Open Source Project
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

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import java.util.Calendar;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.eurina.android.mobilequeue.R;
import com.eurina.android.mobilequeue.model.MQ_Elm;
import com.eurina.android.mobilequeue.model.MobileQueueModel.MMQElmLoadingObserver;

/**
 * Define a simple widget updating service that retrieves the real time info on a queue ticket
 */
public class MQWidgetUpdatingService extends Service {
    private static final int MQ_BTN_DIR_LEFT = -1;
    private static final int MQ_BTN_DIR_RIGHT = 1;
    private static final int MQ_UPDATE_INTERVAL = 2;    //in minutes
    
    private static final String TAG = "MQSVR";
    private static final String MQ_INTENT_EXTRA_BTN_NAME = "BTN";
    private static final String MQ_INTENT_EXTRA_TI_NAME = "TI";
    private Handler mHandler;
    


    @Override
    public void onCreate() {  
        mHandler = new Handler();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int incomingWgtId = intent.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
        int btn = intent.getIntExtra(MQWidgetUpdatingService.MQ_INTENT_EXTRA_BTN_NAME, 0);
        Log.d(MQWidgetUpdatingService.TAG, "onStartCommand incomingWgtId = "+incomingWgtId+", BTN = "+btn+" startId = "+startId+", flags = "+flags);
        if (incomingWgtId != INVALID_APPWIDGET_ID) {
            int tidx = intent.getIntExtra(MQWidgetUpdatingService.MQ_INTENT_EXTRA_TI_NAME, 0);
            updateMQTicket(incomingWgtId, btn, tidx);
        } else {
            updateAllMQTickets();
        }
        scheduleNextUpdate(this, MQWidgetUpdatingService.MQ_UPDATE_INTERVAL);
        return START_NOT_STICKY;
    }
    
    private void updateAllMQTickets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName appWidgetProvider = new ComponentName(this, MQWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(appWidgetProvider);
        int N = appWidgetIds.length;
        Log.d(MQWidgetUpdatingService.TAG,"updateAllMQTickets N = "+N);
        for (int i = 0; i < N; i++) {
          int appWidgetId = appWidgetIds[i];
          updateMQTicket(appWidgetId, 0, 0);
        }        
    }

    private void updateMQTicket(final int incomingWgtId, int btn, int tidx) {
        Log.d(MQWidgetUpdatingService.TAG, "onStartCommand incomingWgtId = "+incomingWgtId+", BTN = "+btn);
        //TODO, now just use the latest ticket
        List<MQ_Elm> tickets = MobileQueue.getMQTickets();
        if (tickets != null && tickets.size() > 0) {
            final int tktIdx;
            if (btn == MQWidgetUpdatingService.MQ_BTN_DIR_LEFT) {
                tidx--; 
                if (tidx < 0)
                    tidx = 0;
            } else if (btn == MQWidgetUpdatingService.MQ_BTN_DIR_RIGHT) {
                tidx++;
                if (tidx >= tickets.size())
                    tidx = tickets.size() - 1;
            }     
            tktIdx = tidx;

            MQ_Elm ticket = null;
            if (tidx >= 0 && tidx < tickets.size()) {
                ticket = tickets.get(tidx);
            }
            if (ticket != null) {
                MobileQueue.updateTicketInfo(ticket, new MMQElmLoadingObserver() {
                    public void onLoadingStarted() {
                        // TODO Auto-generated method stub                        
                    }

                    public void onProgressUpdated(String progress) {
                        // TODO Auto-generated method stub                        
                    }

                    public void onLoadingFinished(boolean failed) {
                        // TODO Auto-generated method stub                        
                    }

                    public void onDataReady(final MQ_Elm elm) {
                        if (elm != null) {
                            mHandler.post(new Runnable() {
                                
                                public void run() {
                                    updateTicketUIInfo(elm, incomingWgtId, tktIdx);
                                }
                            });                            
                        }                        
                    }                    
                });
            }
        } else {
            RemoteViews views = buildUpdateReviews(this, incomingWgtId, 0);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(incomingWgtId, views);
        }        
    }

    private void scheduleNextUpdate(Context context, long inIntervalInMS) {
        Log.d(MQWidgetUpdatingService.TAG, "scheduleNextUpdate");
        Intent updateIntent = new Intent(context, this.getClass());
        // A content URI for this Intent may be unnecessary.
        updateIntent.setData(Uri.parse("content://" + context.getPackageName() + "/mobilequeue"));
        PendingIntent updatePendingIntent =
            PendingIntent.getService(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar cal = Calendar.getInstance();
        // add 2 minutes to the calendar object
        cal.add(Calendar.MINUTE, MQWidgetUpdatingService.MQ_UPDATE_INTERVAL);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, cal.getTimeInMillis(), updatePendingIntent);
    }


    private void updateTicketUIInfo(MQ_Elm elm, int incomingWgtId, int tidx) {
        String data = elm.getDetailedInfo();
        Log.d(MQWidgetUpdatingService.TAG, "updateTicketUIInfo, incomingWgtId = "+incomingWgtId+", data = "+data);
        if (data != null) {
            //extract info
            JSONObject json;
            try {
                json = new JSONObject(data);
                //String status = json.getString("STS");
                int pnr = json.getInt("PNR");
                int mnr = json.getInt("MNR");
                int awt = json.getInt("AWT");

                RemoteViews views = buildUpdateReviews(this, incomingWgtId, tidx);
                Log.d(MQWidgetUpdatingService.TAG, "called buildUpdateReviews");
                updateTicketInfo(incomingWgtId, views, elm.getName(), Integer.toString(mnr), Integer.toString(mnr - pnr), Integer.toString(awt));
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }        
    }


    /**
     * Build a widget update to show the real time info to a queue ticket
     * @param incomingWgtId 
     * @param tidx 
     */
    private RemoteViews buildUpdateReviews(Context context, int incomingWgtId, int tidx) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mq_widget);

        List<MQ_Elm> tickets = MobileQueue.getMQTickets();
        Intent intent = null;

        if (tickets == null || tickets.size() <= 0) {
            views.setViewVisibility(R.id.mqWgtNoTicket, View.VISIBLE);
            views.setViewVisibility(R.id.mqWgtTicketInfo, View.GONE);
            intent = new Intent(context, MQQueueMain.class);
        } else {
            views.setViewVisibility(R.id.mqWgtNoTicket, View.GONE);
            views.setViewVisibility(R.id.mqWgtTicketInfo, View.VISIBLE);

            //updateTicketInfo(null, null, null, null);        

            // Create an Intent to launch MQQueueTicket activity
            intent = new Intent(context, MQQueueTicket.class); 
            int elmIdx = MobileQueue.findSelectedItem(tickets.get(tidx));
            intent.putExtra(MobileQueue.INDEX, elmIdx);

            if (tickets.size() == 1) {
                views.setViewVisibility(R.id.wgtTicketLeft, View.GONE);
                views.setViewVisibility(R.id.wgtTicketRight, View.GONE);
            } else {
                views.setViewVisibility(R.id.wgtTicketLeft, View.VISIBLE);
                views.setViewVisibility(R.id.wgtTicketRight, View.VISIBLE);
                // Create an Intent to handle < and > buttons
                setBtnEvtIntent(views, context, tidx, incomingWgtId, MQWidgetUpdatingService.MQ_BTN_DIR_LEFT);
                setBtnEvtIntent(views, context, tidx, incomingWgtId, MQWidgetUpdatingService.MQ_BTN_DIR_RIGHT);
            }                
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.wgtTicketInfoDiv, pendingIntent);
        return views;
    }

    private void setBtnEvtIntent(RemoteViews views, Context context, int tidx, int incomingWgtId, int btndir) {
        Intent svcIntent = new Intent(context, MQWidgetUpdatingService.class);
        svcIntent.setData(Uri.parse("content://" + context.getPackageName() + "/appwgtid/"+incomingWgtId+"/mqwgt/btn_dir/" + btndir));
        svcIntent.putExtra(EXTRA_APPWIDGET_ID, incomingWgtId);
        svcIntent.putExtra(MQWidgetUpdatingService.MQ_INTENT_EXTRA_TI_NAME, tidx);
        svcIntent.putExtra(MQWidgetUpdatingService.MQ_INTENT_EXTRA_BTN_NAME, btndir);
        PendingIntent lpendingIntent = PendingIntent.getService(context, 0, svcIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(btndir == MQWidgetUpdatingService.MQ_BTN_DIR_LEFT? R.id.wgtTicketLeft : R.id.wgtTicketRight, lpendingIntent);
    }

    private void updateTicketInfo(int incomingWgtId, RemoteViews views, CharSequence ticketName, CharSequence ticketNr, CharSequence proceedFolk, CharSequence waitMin) {
        if (views != null) {
            views.setTextViewText(R.id.mqWgtTicketName, ticketName);
            views.setTextViewText(R.id.mqWgtTicketNr, ticketNr);
            views.setTextViewText(R.id.mqWgtPrecedFolk, proceedFolk);
            views.setTextViewText(R.id.mqWgtWaitMin, waitMin); 
        }
        // Push update for this widget to the home screen
        //ComponentName thisWidget = new ComponentName(this, MQWidgetUpdatingService.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(incomingWgtId, views);
        Log.d(MQWidgetUpdatingService.TAG, "called manager.updateAppWidget()");
    }
}
