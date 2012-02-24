
package com.eurina.android.mobilequeue.ui;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eurina.android.mobilequeue.R;


class MQProgressbar {
    private View mTitleBar;
    private TextView mTitle;
    private ProgressBar mProgress;

    private Animation mSlideIn;
    private Animation mSlideOut;
    
    public MQProgressbar(View titlebar, ProgressBar pbar, TextView title) {
        mTitleBar = titlebar;
        mTitle = title;
        mProgress = pbar;
        
        mSlideIn = AnimationUtils.loadAnimation(mTitleBar.getContext(), R.anim.slide_in);
        mSlideOut = AnimationUtils.loadAnimation(mTitleBar.getContext(), R.anim.slide_out);
    }

    public void onLoadingStarted() {
//        mTitleBar.startAnimation(mSlideIn);
//        if (mProgress != null) {
//            mProgress.setVisibility(View.VISIBLE);
//        }
    }

    public void onProgressUpdated(String progress) {        
//        if (mTitle != null) {
//            mTitle.setText(progress);
//        }        
    }

    
    public void onLoadingFinished(boolean failed) {
//        if (failed)
//            onProgressUpdated("Loading failed");
//        mTitleBar.startAnimation(mSlideOut);  
//        if (mProgress != null) {
//            mProgress.setVisibility(View.INVISIBLE);
//        }
    }        
}

