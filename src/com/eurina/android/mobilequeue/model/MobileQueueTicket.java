
package com.eurina.android.mobilequeue.model;

import java.util.Calendar;

public class MobileQueueTicket {
    MQServiceQueue mQueue;

    Calendar mDate;

    int mNr;

    public MobileQueueTicket(int id, String name, String desc) {
    }

    public boolean addChild(final MobileQueueTicket child) {
        return false;
    }

}