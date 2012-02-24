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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

/**
 * Define a simple widget that shows the real time info on a queue ticket
 */
public class MQWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "MQ_PVD";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        int appWidgetId = INVALID_APPWIDGET_ID;

        Log.d(MQWidgetProvider.TAG, "onUpdate(,appWidgetIds) "+appWidgetIds.length);
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            ComponentName appWidgetProvider = new ComponentName(context, MQWidgetProvider.class);
            appWidgetIds = appWidgetManager.getAppWidgetIds(appWidgetProvider);
        }
        int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            appWidgetId = appWidgetIds[i];
            Log.d(MQWidgetProvider.TAG, "onUpdate: "+appWidgetId);

            Intent intent = new Intent(context, MQWidgetUpdatingService.class);
            intent.putExtra(EXTRA_APPWIDGET_ID, appWidgetId);
            context.startService(intent);
        }
        
        //scheduleNextUpdate(context, MQWidgetProvider.MQ_UPDATE_INTERVAL);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        int oc = intent.getIntExtra(MQWidgetProvider.TAG, -1);
        if (oc == 1) {
            //handle updating
        } else {
            super.onReceive(context, intent);
        }        
    }
    
    private void scheduleNextUpdate(Context context, long inIntervalInMS)
    {
        Log.d(MQWidgetProvider.TAG, "scheduleNextUpdate");
        Intent updateIntent = new Intent(context, this.getClass());
        // A content URI for this Intent may be unnecessary.
        updateIntent.setData(Uri.parse("content://" + context.getPackageName() + "/mobilequeue"));
        updateIntent.putExtra(MQWidgetProvider.TAG, 1);
        PendingIntent updatePendingIntent =
            PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // The update frequency should be user configurable.
        Time time = new Time();
        time.set(System.currentTimeMillis() + inIntervalInMS);
        time.minute = 0;
        time.second = 0;
        long nextUpdate = time.toMillis(false);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, nextUpdate, updatePendingIntent);
    }


    /**
     * since we've already got our own async function for net data loading/updating
     * therefore, we'll NOT extends IntentService here.
     * @author ET33168
     *
     */
//    public static class UpdateService extends Service {
//        private static final int MQ_BTN_DIR_LEFT = -1;
//        private static final int MQ_BTN_DIR_RIGHT = 1;
//        
//        
//        @Override
//        public void onCreate() {      
//        }
//
//        @Override
//        public IBinder onBind(Intent intent) {
//            return null;
//        }
//        
//        @Override
//        public int onStartCommand(Intent intent, int flags, int startId) {
//            final int incomingWgtId = intent.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
//            int act = intent.getIntExtra("BTN", 0);
//            Log.d("MQSVR", "onStartCommand incomingWgtId = "+incomingWgtId+", BTN = "+act+" startId = "+startId+", flags = "+flags);
//            //TODO, now just use the latest ticket
//            List<MQ_Elm> tickets = MobileQueue.getMQTickets();
//            if (tickets != null && tickets.size() > 0) {
//                MQ_Elm ticket = tickets.get(tickets.size() - 1);
//                MobileQueue.updateTicketInfo(ticket, new MMQElmLoadingObserver() {
//                    public void onLoadingStarted() {
//                        // TODO Auto-generated method stub                        
//                    }
//
//                    public void onProgressUpdated(String progress) {
//                        // TODO Auto-generated method stub                        
//                    }
//
//                    public void onLoadingFinished(boolean failed) {
//                        // TODO Auto-generated method stub                        
//                    }
//
//                    public void onDataReady(MQ_Elm elm) {
//                        if (elm != null) {
//                            updateTicketUIInfo(elm, incomingWgtId);
//                        }                        
//                    }                    
//                });
//            }
//            return START_NOT_STICKY;
//        }
//        
//        private void updateTicketUIInfo(MQ_Elm elm, int incomingWgtId) {
//            String data = elm.getDetailedInfo();
//            if (data != null) {
//                //extract info
//                JSONObject json;
//                try {
//                    json = new JSONObject(data);
//                    String status = json.getString("STS");
//                    int pnr = json.getInt("PNR");
//                    int mnr = json.getInt("MNR");
//                    int awt = json.getInt("AWT");
//                    
//                    RemoteViews views = buildUpdate(this, incomingWgtId);
//                    updateTicketInfo(views, elm.getName(), Integer.toString(mnr), Integer.toString(mnr - pnr), Integer.toString(awt));
//                } catch (JSONException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }        
//        }
//
//
//        /**
//         * Build a widget update to show the real time info to a queue ticket
//         * @param incomingWgtId 
//         */
//        private RemoteViews buildUpdate(Context context, int incomingWgtId) {
//
//            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mq_widget);
//            
//            List<MQ_Elm> tickets = MobileQueue.getMQTickets();
//            
//            if (tickets == null || tickets.size() <= 0) {
//                views.setViewVisibility(R.id.mqWgtNoTicket, View.VISIBLE);
//                views.setViewVisibility(R.id.mqWgtTicketInfo, View.GONE);
//            } else {
//                views.setViewVisibility(R.id.mqWgtNoTicket, View.GONE);
//                views.setViewVisibility(R.id.mqWgtTicketInfo, View.VISIBLE);
//                
//                //updateTicketInfo(null, null, null, null);        
//    
//                // Create an Intent to launch MQQueueTicket activity
//                Intent intent = new Intent(context, MQQueueTicket.class);
//                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
//                views.setOnClickPendingIntent(R.id.widget, pendingIntent);
//                
//                if (tickets.size() == 1) {
//                    views.setViewVisibility(R.id.wgtTicketLeft, View.GONE);
//                    views.setViewVisibility(R.id.wgtTicketRight, View.GONE);
//                } else {
//                    views.setViewVisibility(R.id.wgtTicketLeft, View.VISIBLE);
//                    views.setViewVisibility(R.id.wgtTicketRight, View.VISIBLE);
//                    // Create an Intent to handle < and > buttons
//                    setBtnEvtIntent(views, context, incomingWgtId, UpdateService.MQ_BTN_DIR_LEFT);
//                    setBtnEvtIntent(views, context, incomingWgtId, UpdateService.MQ_BTN_DIR_RIGHT);
//                }                
//            }
//            return views;
//        }
//        
//        private void setBtnEvtIntent(RemoteViews views, Context context, int incomingWgtId, int btndir) {
//            Intent lintent = new Intent(context, MQWidgetProvider.class);
//            lintent.setData(Uri.parse("content://" + context.getPackageName() + "/appwgtid/"+incomingWgtId+"/mqwgt/btn_dir/" + btndir));
//            lintent.putExtra(EXTRA_APPWIDGET_ID, incomingWgtId);
//            lintent.putExtra("BTN", btndir);
//            PendingIntent lpendingIntent = PendingIntent.getService(context, 0, lintent, PendingIntent.FLAG_UPDATE_CURRENT);
//            views.setOnClickPendingIntent(btndir == UpdateService.MQ_BTN_DIR_LEFT? R.id.wgtTicketLeft:R.id.wgtTicketRight, lpendingIntent);
//        }
//
//        private void updateTicketInfo(RemoteViews views, CharSequence ticketName, CharSequence ticketNr, CharSequence proceedFolk, CharSequence waitMin) {
//            if (views != null) {
//                views.setTextViewText(R.id.mqWgtTicketName, ticketName);
//                views.setTextViewText(R.id.mqWgtTicketNr, ticketNr);
//                views.setTextViewText(R.id.mqWgtPrecedFolk, proceedFolk);
//                views.setTextViewText(R.id.mqWgtWaitMin, waitMin); 
//            }
//            // Push update for this widget to the home screen
//            ComponentName thisWidget = new ComponentName(this, MQWidgetProvider.class);
//            AppWidgetManager manager = AppWidgetManager.getInstance(this);
//            manager.updateAppWidget(thisWidget, views);
//        }
//    }
}
