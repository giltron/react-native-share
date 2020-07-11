package cl.json.social;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.content.pm.ResolveInfo;
import android.content.ComponentName;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.net.URLEncoder;
import java.util.ArrayList;

import cl.json.RNShareModule;
import cl.json.ShareFile;
import cl.json.ShareFiles;

/**
 * Created by disenodosbbcl on 23-07-16.
 */
public abstract class ShareIntent {

    protected final ReactApplicationContext reactContext;
    protected Intent intent;
    protected String chooserTitle = "Share";
    protected ShareFile fileShare;
    protected ReadableMap options;
    protected ShareFile stickerAsset;
    protected ShareFile backgroundAsset;

    public ShareIntent(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
        this.setIntent(new Intent(android.content.Intent.ACTION_SEND));
        this.getIntent().setType("text/plain");
    }

    public Intent excludeChooserIntent(Intent prototype, ReadableMap options) {
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        List<HashMap<String, String>> intentMetaInfo = new ArrayList<HashMap<String, String>>();
        Intent chooserIntent;

        Intent dummy = new Intent(prototype.getAction());
        dummy.setType(prototype.getType());
        List<ResolveInfo> resInfo = this.reactContext.getPackageManager().queryIntentActivities(dummy, 0);

        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                if (resolveInfo.activityInfo == null || options.getArray("excludedActivityTypes").toString().contains(resolveInfo.activityInfo.packageName))
                    continue;

                HashMap<String, String> info = new HashMap<String, String>();
                info.put("packageName", resolveInfo.activityInfo.packageName);
                info.put("className", resolveInfo.activityInfo.name);
                info.put("simpleName", String.valueOf(resolveInfo.activityInfo.loadLabel(this.reactContext.getPackageManager())));
                intentMetaInfo.add(info);
            }

            if (!intentMetaInfo.isEmpty()) {
                // sorting for nice readability
                Collections.sort(intentMetaInfo, new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> map, HashMap<String, String> map2) {
                        return map.get("simpleName").compareTo(map2.get("simpleName"));
                    }
                });

                // create the custom intent list
                for (HashMap<String, String> metaInfo : intentMetaInfo) {
                    Intent targetedShareIntent = (Intent) prototype.clone();
                    targetedShareIntent.setPackage(metaInfo.get("packageName"));
                    targetedShareIntent.setClassName(metaInfo.get("packageName"), metaInfo.get("className"));
                    targetedShareIntents.add(targetedShareIntent);
                }

                chooserIntent = Intent.createChooser(targetedShareIntents.remove(targetedShareIntents.size() - 1), "share");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
                return chooserIntent;
            }
        }

        return Intent.createChooser(prototype, "Share");
    }

    public void open(ReadableMap options) throws ActivityNotFoundException {
        this.options = options;

        if (ShareIntent.hasValidKey("subject", options)) {
            this.getIntent().putExtra(Intent.EXTRA_SUBJECT, options.getString("subject"));
        }

        if (ShareIntent.hasValidKey("email", options)) {
            System.out.println("has hasValidKey email");
            this.getIntent().putExtra(Intent.EXTRA_EMAIL, new String[] { options.getString("email") });
        }

           if (ShareIntent.hasValidKey("outlook", options)) {
                       System.out.println("has hasValidKey outlook");
//                     this.getIntent().putExtra(Intent.EXTRA_EMAIL, new String[] { options.getString("outlook") });
            }

        if (ShareIntent.hasValidKey("title", options)) {
            this.chooserTitle = options.getString("title");
        }

        String message = "";
        if (ShareIntent.hasValidKey("message", options)) {
            message = options.getString("message");
        }

        String socialType  = "";
        if (ShareIntent.hasValidKey("social", options)) {
            socialType = options.getString("social");
        }

        if (socialType.equals("sms")) {
            String recipient = options.getString("recipient");

            if (!recipient.isEmpty()) {
                this.getIntent().putExtra("address", recipient);
            }
        }

        if (socialType.equals("whatsapp")) {
            String whatsAppNumber = options.getString("whatsAppNumber");
            if (!whatsAppNumber.isEmpty()) {
                String chatAddress = whatsAppNumber + "@s.whatsapp.net";
                this.getIntent().putExtra("jid", chatAddress);
            }
        }

        if (socialType.equals("instagramstories")) {
            if (ShareIntent.hasValidKey("method", options)) {
                String method = options.getString("method");
                switch (method) {
                    case "shareBackgroundImage": {
                        if (ShareIntent.hasValidKey("backgroundImage", options)) {
                            this.backgroundAsset = new ShareFile(options.getString("backgroundImage"), "image/jpeg", "background", this.reactContext);
                            this.getIntent().setDataAndType(backgroundAsset.getURI(), backgroundAsset.getType());
                            this.getIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            if (ShareIntent.hasValidKey("attributionURL", options)) {
                                this.getIntent().putExtra("content_url", options.getString("attributionURL"));
                            }
                        } else {
                            throw new java.lang.IllegalArgumentException("backgroundImage is empty");
                        }
                        break;
                    }
                    case "shareStickerImage": {
                        if (ShareIntent.hasValidKey("stickerImage", options)) {
                            this.getIntent().setType("image/jpeg");
                            this.stickerAsset = new ShareFile(options.getString("stickerImage"), "image/jpeg", "sticker", this.reactContext);
                            this.getIntent().putExtra("interactive_asset_uri", stickerAsset.getURI());

                            if (ShareIntent.hasValidKey("attributionURL", options)) {
                                this.getIntent().putExtra("content_url", options.getString("attributionURL"));
                            }

                            if (ShareIntent.hasValidKey("backgroundTopColor", options)) {
                                this.getIntent().putExtra("top_background_color", options.getString("backgroundTopColor"));
                            }

                            if (ShareIntent.hasValidKey("backgroundBottomColor", options)) {
                                this.getIntent().putExtra("bottom_background_color", options.getString("backgroundBottomColor"));
                            }
                        } else {
                            throw new java.lang.IllegalArgumentException("stickerImage is empty");
                        }
                        break;
                    }
                    case "shareBackgroundAndStickerImage": {
                        if (ShareIntent.hasValidKey("backgroundImage", options) && ShareIntent.hasValidKey("stickerImage", options)) {
                            this.backgroundAsset = new ShareFile(options.getString("backgroundImage"), "image/jpeg", "background", this.reactContext);
                            this.stickerAsset = new ShareFile(options.getString("stickerImage"), "image/jpeg", "sticker", this.reactContext);

                            this.getIntent().setDataAndType(backgroundAsset.getURI(), backgroundAsset.getType());
                            this.getIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            this.getIntent().putExtra("interactive_asset_uri", stickerAsset.getURI());

                            if (ShareIntent.hasValidKey("attributionURL", options)) {
                                this.getIntent().putExtra("content_url", options.getString("attributionURL"));
                            }
                        } else {
                            throw new java.lang.IllegalArgumentException("backgroundImage or stickerImage is empty");
                        }
                        break;
                    }
                    default:
                        throw new java.lang.IllegalStateException("Unknown Instagram stories sharing mode: " + method);
                }
            }
        }

        if (ShareIntent.hasValidKey("urls", options)) {

            ShareFiles fileShare = getFileShares(options);
            if (fileShare.isFile()) {
                ArrayList<Uri> uriFile = fileShare.getURI();
                this.getIntent().setAction(Intent.ACTION_SEND_MULTIPLE);
                this.getIntent().setType(fileShare.getType());
                this.getIntent().putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriFile);
                this.getIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (!TextUtils.isEmpty(message)) {
                    this.getIntent().putExtra(Intent.EXTRA_TEXT, message);
                }
            } else {
                if (!TextUtils.isEmpty(message)) {
                    this.getIntent().putExtra(Intent.EXTRA_TEXT, message + " " + options.getArray("urls").getString(0));
                } else {
                    this.getIntent().putExtra(Intent.EXTRA_TEXT, options.getArray("urls").getString(0));
                }
            }
        } else if (ShareIntent.hasValidKey("url", options)) {
            this.fileShare = getFileShare(options);
            if (this.fileShare.isFile()) {
                Uri uriFile = this.fileShare.getURI();
                this.getIntent().setType(this.fileShare.getType());
                this.getIntent().putExtra(Intent.EXTRA_STREAM, uriFile);
                this.getIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (!TextUtils.isEmpty(message)) {
                    this.getIntent().putExtra(Intent.EXTRA_TEXT, message);
                }
            } else {
                if (!TextUtils.isEmpty(message)) {
                    this.getIntent().putExtra(Intent.EXTRA_TEXT, message + " " + options.getString("url"));
                } else {
                    this.getIntent().putExtra(Intent.EXTRA_TEXT, options.getString("url"));
                }
            }
        } else if (!TextUtils.isEmpty(message)) {
            this.getIntent().putExtra(Intent.EXTRA_TEXT, message);
        }
    }

    protected ShareFile getFileShare(ReadableMap options) {
         String filename = null;
        if (ShareIntent.hasValidKey("filename", options)) {
            filename = options.getString("filename");
        }
        if (ShareIntent.hasValidKey("type", options)) {
            return new ShareFile(options.getString("url"), options.getString("type"), filename, this.reactContext);
        } else {
            return new ShareFile(options.getString("url"), filename, this.reactContext);
        }
    }

    protected ShareFiles getFileShares(ReadableMap options) {
        ArrayList<String> filenames = new ArrayList<>();
        if (ShareIntent.hasValidKey("filenames", options)) {
            ReadableArray fileNamesReadableArray = options.getArray("filenames");
            for (int i = 0; i < fileNamesReadableArray.size(); i++) {
                filenames.add(fileNamesReadableArray.getString(i));
            }
        }

        if (ShareIntent.hasValidKey("type", options)) {
            return new ShareFiles(options.getArray("urls"), filenames, options.getString("type"), this.reactContext);
        } else {
            return new ShareFiles(options.getArray("urls"), filenames, this.reactContext);
        }
    }

    protected static String urlEncode(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URLEncoder.encode() failed for " + param);
        }
    }

    protected Intent[] getIntentsToViewFile(Intent intent, Uri uri) {
        PackageManager pm = this.reactContext.getPackageManager();

        List<ResolveInfo> resInfo = pm.queryIntentActivities(intent, 0);
        Intent[] extraIntents = new Intent[resInfo.size()];
        for (int i = 0; i < resInfo.size(); i++) {
            ResolveInfo ri = resInfo.get(i);
            String packageName = ri.activityInfo.packageName;

            Intent newIntent = new Intent();
            newIntent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
            newIntent.setAction(Intent.ACTION_VIEW);
            newIntent.setDataAndType(uri, intent.getType());
            newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            extraIntents[i] = new Intent(newIntent);
        }

        return extraIntents;
    }

    protected void openIntentChooser() throws ActivityNotFoundException {
        Activity activity = this.reactContext.getCurrentActivity();
        if (activity == null) {
            System.out.println("TargetChosenReceiver is null, openIntentChooser");
            TargetChosenReceiver.sendCallback(false, "Something went wrong");
            return;
        }
        Intent chooser;
        IntentSender intentSender = null;
        if (TargetChosenReceiver.isSupported()) {
            System.out.println("TargetChosenReceiver.isSupported");
            intentSender = TargetChosenReceiver.getSharingSenderIntent(this.reactContext);
            chooser = Intent.createChooser(this.getIntent(), this.chooserTitle, intentSender);
        } else {
            System.out.println("TargetChosenReceiver. is no supported");
            chooser = Intent.createChooser(this.getIntent(), this.chooserTitle);
        }
        chooser.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        if (ShareIntent.hasValidKey("showAppsToView", options) && ShareIntent.hasValidKey("url", options)) {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setType(this.fileShare.getType());

            Intent[] viewIntents = this.getIntentsToViewFile(viewIntent, this.fileShare.getURI());

            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, viewIntents);
        }

        if (ShareIntent.hasValidKey("excludedActivityTypes", options)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, getExcludedComponentArray(options.getArray("excludedActivityTypes")));
                activity.startActivityForResult(chooser, RNShareModule.SHARE_REQUEST_CODE);
            } else {
                activity.startActivityForResult(excludeChooserIntent(this.getIntent(),options), RNShareModule.SHARE_REQUEST_CODE);
            }
        } else {
            System.out.println("startActivityForResult");
            activity.startActivityForResult(chooser, RNShareModule.SHARE_REQUEST_CODE);
        }

        if (intentSender == null) {
            TargetChosenReceiver.sendCallback(true, true, "OK");
        }
    }

    public static boolean isPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    protected Intent getIntent() {
        return this.intent;
    }

    protected void setIntent(Intent intent) {
        this.intent = intent;
    }

    public static boolean hasValidKey(String key, ReadableMap options) {
        return options != null && options.hasKey(key) && !options.isNull(key);
    }

    protected abstract String getPackage();

    protected String getComponentClass() {
        return null;
    }

    protected abstract String getDefaultWebLink();

    protected abstract String getPlayStoreLink();

    private ComponentName[] getExcludedComponentArray(ReadableArray excludeActivityTypes){
        if (excludeActivityTypes == null){
            return null;
        }
        Intent dummy = new Intent(getIntent().getAction());
        dummy.setType(getIntent().getType());
        List<ComponentName> componentNameList = new ArrayList<>();
        List<ResolveInfo> resInfoList = this.reactContext.getPackageManager().queryIntentActivities(dummy, 0);
        for (int index = 0; index < excludeActivityTypes.size(); index++) {
            String packageName = excludeActivityTypes.getString(index);
            for(ResolveInfo resInfo : resInfoList) {
                if(resInfo.activityInfo.packageName.equals(packageName)) {
                    componentNameList.add(new ComponentName(resInfo.activityInfo.packageName, resInfo.activityInfo.name));
                }
            }
        }
        return componentNameList.toArray(new ComponentName[]{});
    }
}
