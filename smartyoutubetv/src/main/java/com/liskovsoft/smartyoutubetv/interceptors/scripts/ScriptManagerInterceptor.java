package com.liskovsoft.smartyoutubetv.interceptors.scripts;

import android.content.Context;
import android.webkit.WebResourceResponse;

import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv.interceptors.RequestInterceptor;
import com.liskovsoft.smartyoutubetv.interceptors.ads.contentfilter.ContentFilter;
import com.liskovsoft.smartyoutubetv.webscripts.MainCachedScriptManager;
import com.liskovsoft.smartyoutubetv.webscripts.ScriptManager;

import java.io.InputStream;

import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class ScriptManagerInterceptor extends RequestInterceptor {
    private static final String TAG = ScriptManagerInterceptor.class.getSimpleName();
    private final ScriptManager mManager;
    private final ContentFilter mFilter;
    private boolean mFirstScriptDone;

    public ScriptManagerInterceptor(Context context) {
        super(context);

        mManager = new MainCachedScriptManager(context);
        mFilter = new ContentFilter(context);
    }

    @Override
    public boolean test(String url) {
        if (isFirstScript(url)) {
            return true;
        }

        if (isSecondScript(url)) {
            return true;
        }

        if (isLastScript(url)) {
            return true;
        }

        if (isStyle(url)) {
            return true;
        }

        return false;
    }

    @Override
    public WebResourceResponse intercept(String url) {
        Response response = getResponse(url);

        if (response == null || response.body() == null) {
            Log.e(TAG, "Can't inject custom scripts into " + url + ". Response is empty: " + response);
            return null;
        }

        ResponseBody body = response.body();

        InputStream result = body.byteStream();

        if (isFirstScript(url)) {
            result = applyInit(result);
            mFirstScriptDone = true;
            result = mFilter.filterFirstScript(result);
        } else if (isSecondScript(url)) {
            result = mFilter.filterSecondScript(result);
        } else if (isLastScript(url)) {
            if (!mFirstScriptDone) {
                result = applyInit(result);
            }

            result = applyLoad(result);
            result = mFilter.filterLastScript(result);
        } else if (isStyle(url)) {
            result = applyStyles(result);
            result = mFilter.filterStyles(result);
        }

        return createResponse(response.body().contentType(), result);
    }

    @Nullable
    private InputStream applyInit(InputStream result) {
        Log.d(TAG, "Begin onInitScripts");
        InputStream onInitScripts = mManager.getOnInitScripts();
        Log.d(TAG, "End onInitScripts");
        result = Helpers.appendStream(onInitScripts, result);
        return result;
    }

    @Nullable
    private InputStream applyLoad(InputStream result) {
        Log.d(TAG, "Begin onLoadScript");
        InputStream onLoadScripts = mManager.getOnLoadScripts();
        Log.d(TAG, "End onLoadScript");
        result = Helpers.appendStream(result, onLoadScripts);
        return result;
    }

    @Nullable
    private InputStream applyStyles(InputStream result) {
        Log.d(TAG, "Begin onStyles");
        InputStream styles = mManager.getStyles();
        Log.d(TAG, "End onStyles");
        result = Helpers.appendStream(result, styles);
        return result;
    }

    protected abstract boolean isFirstScript(String url);

    protected abstract boolean isSecondScript(String url);

    protected abstract boolean isLastScript(String url);

    protected abstract boolean isStyle(String url);
}
