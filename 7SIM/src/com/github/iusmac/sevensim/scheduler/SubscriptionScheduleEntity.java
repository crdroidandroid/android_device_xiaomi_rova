package com.github.iusmac.sevensim.scheduler;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalTime;
import java.util.Objects;

/**
 * This class is a data transfer object (DTO), representing a weekly repeat schedule for managing
 * the enabled state of a SIM subscription.
 */
@Entity(
    tableName = "subscription_schedules",
    indices = {
        @Index(value = {"sub_id", "sub_enabled", "enabled", "days_of_week_bits"})
    }
)
public final class SubscriptionScheduleEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long mId;

    @ColumnInfo(name = "sub_id")
    private int mSubscriptionId;

    @ColumnInfo(name = "sub_enabled")
    private boolean mSubscriptionEnabled;

    @ColumnInfo(name = "label")
    private String mLabel;

    @ColumnInfo(name = "enabled")
    private boolean mEnabled;

    @ColumnInfo(name = "days_of_week_bits")
    private DaysOfWeek mDaysOfWeek;

    @NonNull
    @ColumnInfo(name = "minutes_since_midnight")
    private LocalTime mTime;

    public long getId() {
        return mId;
    }

    public void setId(final long id) {
        mId = id;
    }

    public int getSubscriptionId() {
        return mSubscriptionId;
    }

    public void setSubscriptionId(final int subId) {
        mSubscriptionId = subId;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(final String label) {
        mLabel = label;
    }

    public boolean getSubscriptionEnabled() {
        return mSubscriptionEnabled;
    }

    public void setSubscriptionEnabled(final boolean subscriptionEnabled) {
        mSubscriptionEnabled = subscriptionEnabled;
    }

    public boolean getEnabled() {
        return mEnabled;
    }

    public void setEnabled(final boolean enabled) {
        mEnabled = enabled;
    }

    public DaysOfWeek getDaysOfWeek() {
        return mDaysOfWeek;
    }

    public void setDaysOfWeek(final DaysOfWeek daysOfWeek) {
        mDaysOfWeek = daysOfWeek;
    }

    public LocalTime getTime() {
        return mTime;
    }

    public void setTime(final LocalTime time) {
        mTime = time;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final SubscriptionScheduleEntity compareTo = (SubscriptionScheduleEntity) o;
        return compareTo.mId == mId
            && compareTo.mSubscriptionId == mSubscriptionId
            && compareTo.mSubscriptionEnabled == mSubscriptionEnabled
            && TextUtils.equals(compareTo.mLabel, mLabel)
            && compareTo.mEnabled == mEnabled
            && compareTo.mDaysOfWeek.equals(mDaysOfWeek)
            && compareTo.mTime.equals(mTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mSubscriptionId, mSubscriptionEnabled, mLabel, mEnabled,
                mDaysOfWeek, mTime);
    }

    @Override
    public String toString() {
        return "SubscriptionScheduleEntity {"
        + " id=" + mId
        + " subscriptionId=" + mSubscriptionId
        + " subscriptionEnabled=" + mSubscriptionEnabled
        + " enabled=" + mEnabled
        + " daysOfWeek=" + mDaysOfWeek
        + " time=" + mTime
        + " }";
    }
}
