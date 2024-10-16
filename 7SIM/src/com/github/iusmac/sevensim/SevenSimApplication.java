package com.github.iusmac.sevensim;

import android.app.Application;

import androidx.annotation.NonNull;

import dagger.hilt.android.HiltAndroidApp;

import javax.inject.Inject;
import javax.inject.Provider;

@HiltAndroidApp(Application.class)
public final class SevenSimApplication extends Hilt_SevenSimApplication {
    @Inject
    Provider<ApplicationInfo> mApplicationInfoProvider;

    @Inject
    Logger.Factory mLoggerFactory;

    @Inject
    Provider<NotificationManager> mNotificationManager;

    private boolean mHasAospPlatformSignature;
    private String mApplicationVersion;
    private boolean mIsSystemApplication;

    @Override
    public void onCreate() {
        super.onCreate();

        final Logger logger = mLoggerFactory.create(getClass().getSimpleName());

        // (Re-)create the notification channel to update string resources
        mNotificationManager.get().createForegroundNotificationChannel();
        mNotificationManager.get().createImportantNotificationChannel();

        final ApplicationInfo appInfo = mApplicationInfoProvider.get();
        mHasAospPlatformSignature = appInfo.hasAospPlatformSignature();
        mApplicationVersion = appInfo.getPackageVersionName();
        mIsSystemApplication = appInfo.isSystemApplication();

        logger.d("onCreate() : mHasAospPlatformSignature=%s,mApplicationVersion=%s," +
                "mIsSystemApplication=%s.", mHasAospPlatformSignature, mApplicationVersion,
                mIsSystemApplication);
    }

    /**
     * Return {@code true} if the application has been signed with the AOSP platform signature,
     * {@code false} otherwise.
     */
    public boolean hasAospPlatformSignature() {
        return mHasAospPlatformSignature;
    }

    /**
     * Return the string containing the package version.
     */
    public @NonNull String getPackageVersionName() {
        return mApplicationVersion;
    }

    /**
     * Return {@code true} if the application is classified by the OS as a "built-in system"
     * application, {@code false} otherwise.
     */
    public boolean isSystemApplication() {
        return mIsSystemApplication;
    }
}
