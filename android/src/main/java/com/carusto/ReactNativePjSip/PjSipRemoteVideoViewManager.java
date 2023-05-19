package com.carusto.ReactNativePjSip;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

public class PjSipRemoteVideoViewManager extends SimpleViewManager<PjSipRemoteVideo>  {

    private String LOCAL_VIDEO_CLASS = "PjSipRemoteVideoView";

    @Override
    public String getName() {
        return LOCAL_VIDEO_CLASS;
    }

    @ReactProp(name = "windowId")
    public void setWindowId(PjSipRemoteVideo view, int windowId) {
        view.setWindowId(windowId);
    }

    @ReactProp(name = "objectFit")
    public void setObjectFit(PjSipRemoteVideo view, String objectFit) {
        view.setObjectFit(objectFit);
    }

    @Override
    protected PjSipRemoteVideo createViewInstance(ThemedReactContext reactContext) {
        return new PjSipRemoteVideo(reactContext);
    }
}
