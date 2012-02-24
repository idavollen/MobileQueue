
package com.eurina.android.mobilequeue.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import com.eurina.android.mobilequeue.R;

class MQUtil {
    static int mChoose = -1;
    
    
    public interface MMQTicketDialogListener {
        abstract public void onExit(int aCommond, Object aData);
    }
    
    public static void showMQTicketListInDailg(Context aContext, final CharSequence[] aTickets, final MMQTicketDialogListener aListener) {
        if (aTickets == null || aTickets.length <= 0 ||
            aContext == null || aListener == null) {
            return;
        }
        mChoose = -1;
        new AlertDialog.Builder(aContext)
        .setIcon(R.drawable.alert_dialog_icon)
        .setTitle("Mobile Queue Tickets")//R.string.alert_dialog_single_choice)
        .setSingleChoiceItems(aTickets, mChoose, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked on a radio button do some stuff */
                Log.i("MQ", aTickets[whichButton]+" is being clicked");
                mChoose = whichButton;
            }
        })
        .setPositiveButton(R.string.mqticket_dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked Yes so do some stuff */
                Log.i("MQ", "OK button is clicked");
                //TODO
                aListener.onExit(whichButton, new Integer(mChoose));                
            }
        })
        .setNegativeButton(R.string.mqticket_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                /* User clicked No so do some stuff */
                aListener.onExit(whichButton, null);
            }
        })
       .create().show();
    }
    
}