package com.github.iusmac.sevensim;

import android.util.Log;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Named;
import javax.inject.Provider;

public final class Logger {
    private static final String TAG_PREFIX = "7SIM";
    private static final AtomicBoolean IS_LOGCAT_CHATTY_ENABLED = new AtomicBoolean();

    private final boolean mIsDebuggable;
    private final String mTag;

    @AssistedInject
    public Logger(final @Named("Debug") Provider<Boolean> debug, final @Assisted String tag) {
        // Log everything on debug builds or if explicitly enabled on run-time
        mIsDebuggable = BuildConfig.DEBUG || BuildConfig.DEBUG_OPT || debug.get();

        // Prefix all tags with app name to facilitate searching in massive log files
        mTag = TAG_PREFIX + "." + tag;

        // Allow the app to be "chatty" (send >5 logs/sec.) while debugging to avoid log suppression
        if (mIsDebuggable && IS_LOGCAT_CHATTY_ENABLED.compareAndSet(false, true)) {
            try {
                Runtime.getRuntime().exec(new String[] {
                    "logcat", "-P", "'" + android.os.Process.myPid() + "'"
                }).waitFor();
            } catch (InterruptedException | IOException e) {
                e("Failed to disable app log suppression in logcat.", e);
            }
        }
    }

    public boolean isVerboseLoggable() { return mIsDebuggable || Log.isLoggable(mTag, Log.VERBOSE); }
    public boolean isDebugLoggable() { return mIsDebuggable || Log.isLoggable(mTag, Log.DEBUG); }
    public boolean isInfoLoggable() { return mIsDebuggable || Log.isLoggable(mTag, Log.INFO); }
    public boolean isWarnLoggable() { return mIsDebuggable || Log.isLoggable(mTag, Log.WARN); }
    public boolean isErrorLoggable() { return mIsDebuggable || Log.isLoggable(mTag, Log.ERROR); }
    public boolean isWtfLoggable() { return mIsDebuggable || Log.isLoggable(mTag, Log.ASSERT); }

    public void v(String message, Object... args) {
        if (isVerboseLoggable()) {
            Log.v(mTag, args == null || args.length == 0 ? message : String.format(Locale.US,
                        message, args));
        }
    }

    public void d(String message, Object... args) {
        if (isDebugLoggable()) {
            Log.d(mTag, args == null || args.length == 0 ? message : String.format(Locale.US,
                        message, args));
        }
    }

    public void i(String message, Object... args) {
        if (isInfoLoggable()) {
            Log.i(mTag, args == null || args.length == 0 ? message : String.format(Locale.US,
                        message, args));
        }
    }

    public void w(String message, Object... args) {
        if (isWarnLoggable()) {
            Log.w(mTag, args == null || args.length == 0 ? message : String.format(Locale.US,
                        message, args));
        }
    }

    public void e(String message, Object... args) {
        if (isErrorLoggable()) {
            Log.e(mTag, args == null || args.length == 0 ? message : String.format(Locale.US,
                        message, args));
        }
    }

    public void e(String message, Throwable e) {
        if (isErrorLoggable()) {
            Log.e(mTag, message, e);
        }
    }

    public void wtf(String message, Object... args) {
        if (isWtfLoggable()) {
            Log.wtf(mTag, args == null || args.length == 0 ? message : String.format(Locale.US,
                        message, args));
        }
    }

    public void wtf(Throwable e) {
        if (isWtfLoggable()) {
            Log.wtf(mTag, e);
        }
    }

    /**
     * Factory to create {@link Logger} instances via the {@link AssistedInject} constructor.
     */
    @AssistedFactory
    public interface Factory {
        /** Create a {@link Logger} instance with a specific log tag. */
        Logger create(String tag);
    }
}
