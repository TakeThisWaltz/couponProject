package com.azazel.framework.view;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by JJ on 2015-05-15.
 */
public class ImageViewSwitch {
    private boolean value = true;
    private ImageView mImgView;
    private OnImageCheckedChangeListener mListener;

    private boolean enabled = true;

    public ImageViewSwitch(ImageView imgView, OnImageCheckedChangeListener listener) {
        this.mImgView = imgView;
        this.mListener = listener;
        this.mImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCheckedByEvent(!value);
            }
        });
    }

    public Object getTag(){
        return mImgView.getTag();
    }

    public void setTag(Object obj){
        this.mImgView.setTag(obj);
    }

    public void setVisibility(int visibility){
        this.mImgView.setVisibility(visibility);
    }

    public void setEnabled(boolean enabled){
        mImgView.setEnabled(enabled);
    }

    public void setChecked(boolean checked){
        value = checked;
        mImgView.setAlpha(value ? 1.0f : 0.3f);
        //mListener.onCheckedChanged(ImageViewSwitch.this, value);
    }

    public void setCheckedByEvent(boolean checked){
        value = checked;
        mImgView.setAlpha(value ? 1.0f : 0.3f);
        mListener.onCheckedChanged(ImageViewSwitch.this, value);
    }

    public boolean getChecked(){
        return value;
    }

    public Context getContext(){
        return mImgView.getContext();
    }


    public interface OnImageCheckedChangeListener {
        public void onCheckedChanged(ImageViewSwitch view, boolean isChecked);
    }
}