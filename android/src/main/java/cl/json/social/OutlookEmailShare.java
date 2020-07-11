package cl.json.social;

import android.content.ActivityNotFoundException;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;

/**
 * Created by dex
 */
public class OutlookEmailShare extends SingleShareIntent {

    private static final String PACKAGE = "com.microsoft.office.outlook";
    private static final String PLAY_STORE_LINK = "market://details?id=com.microsoft.office.outlook";


    public OutlookEmailShare(ReactApplicationContext reactContext) {
        super(reactContext);
    }
    @Override
    public void open(ReadableMap options) throws ActivityNotFoundException {
        super.open(options);
        System.out.println('calling open for com.microsoft.office.outlook');
        //  extra params here
        this.openIntentChooser();
    }
    @Override
    protected String getPackage() {
        return PACKAGE;
    }

    @Override
    protected String getDefaultWebLink() {
        return null;
    }

    @Override
    protected String getPlayStoreLink() {
        return PLAY_STORE_LINK;
    }
}

