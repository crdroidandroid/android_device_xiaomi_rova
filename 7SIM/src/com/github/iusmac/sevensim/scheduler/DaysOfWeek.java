/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright (C) 2023 iusmac
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.iusmac.sevensim.scheduler;

import android.content.Context;
import android.icu.text.DateFormatSymbols;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.iusmac.sevensim.R;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import dagger.hilt.android.qualifiers.ApplicationContext;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/**
 * <p>This class is responsible for encoding a weekly repeat cycle in a {@link #getBits()}.
 *
 * <p>It also converts between those bits and the {@link DayOfWeek} int values for easier mutation
 * and querying.
 *
 * <p>The caller can obtain all days of the week represented by a weekly repeat schedule as
 * {@link DayOfWeek} int values directly by iterating over this class, which provides an
 * {@link Iterable} interface, or by calling the appropriate public APIs.
 *
 * <p>This class is both <strong>idempotent</strong> and <strong>immutable</strong>, thus also
 * <strong>thread-safe</strong>.
 *
 * @see DayOfWeek
 */
public final class DaysOfWeek implements Iterable<Integer>, Comparable<DaysOfWeek> {
    /**
     * The one-based array mapping days of the week from {@link DayOfWeek#SUNDAY} (index 1) to
     * {@link DayOfWeek#SATURDAY} (index 7) to the bit masks.
     */
    private static final int[] DAYS_OF_WEEK_BITS = new int[] {
        0, 1<<0, 1<<1, 1<<2, 1<<3, 1<<4, 1<<5, 1<<6
    };

    /** The sum of all days of the week bit masks. */
    private static final int ALL_DAYS_OF_WEEK_BITS = 0x7f;

    @GuardedBy("DaysOfWeek.class")
    private static Locale sDefaultLocaleCache;

    @GuardedBy("DaysOfWeek.class")
    private static String[] sDaysOfWeekNarrowStrings;

    /** An encoded form of a weekly repeat schedule. */
    private final int mBits;

    private final Context mContext;

    /**
     * @param context The context for accessing resources.
     * @param bits The {@link #getBits()} values representing the encoded weekly repeat schedule.
     * @param daysOfWeek Any or all of the {@link DayOfWeek} values.
     */
    @AssistedInject
    DaysOfWeek(final @ApplicationContext @NonNull Context context,
            final @Assisted @Nullable Integer bits,
            final @Assisted @Nullable @DayOfWeek Integer... daysOfWeek) {

        mContext = context;

        int bits_ = 0;
        if (bits != null) {
            bits_ = bits;
        } else if (daysOfWeek != null) {
            bits_ = convertDaysOfWeekToBits(daysOfWeek);
        }
        // Mask off the unused bits
        mBits = ALL_DAYS_OF_WEEK_BITS & bits_;
    }

    /**
     * Return {@link DayOfWeek} encoded in bits as if returned by {@link #getBits()}.
     *
     * @param daysOfWeek Any or all of the {@link DayOfWeek} values.
     */
    private int convertDaysOfWeekToBits(final @DayOfWeek Integer... daysOfWeek) {
        int bits = 0;
        for (int dayOfWeek : daysOfWeek) {
            final int bit = DAYS_OF_WEEK_BITS[dayOfWeek];
            bits = bits | bit;
        }
        return bits;
    }

    /**
     * Return {@code true} if the given day of the week is on, {@code false} otherwise.
     *
     * @param dayOfWeek Any of {@link DayOfWeek} values.
     */
    public boolean isBitOn(final @DayOfWeek int dayOfWeek) {
        final int bit = DAYS_OF_WEEK_BITS[dayOfWeek];
        return (mBits & bit) > 0;
    }

    /**
     * Return the weekly repeat schedule encoded as an integer.
     */
    public int getBits() { return mBits; }

    /**
     * Return {@code true} if at least one day of the week is enabled in the weekly repeat schedule,
     * {@code false} otherwise.
     */
    public boolean isRepeating() { return mBits != 0; }

    /**
     * Return {@code true} if all days of the week are enabled in this weekly repeat schedule,
     * {@code false} otherwise.
     */
    public boolean isFullWeek() { return mBits == ALL_DAYS_OF_WEEK_BITS; }

    /**
     * Return the total number of days of the week enabled in this weekly repeat schedule.
     */
    public int getCount() {
        return (int) IntStream
            .rangeClosed(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY)
            .filter(this::isBitOn).count();
    }

    /**
     * Get the distance between the previous day of the week represented by this weekly repeat
     * schedule and the given day of the week, which is always between 1 and 7 inclusive.
     *
     * @param compareDayOfWeek The {@link DayOfWeek} to compare against.
     * @return An Optional containing the number of days between the given day of the week and the
     * previous enabled day of the week, if any.
     */
    public OptionalInt getDistanceToPreviousDayOfWeek(final @DayOfWeek int compareDayOfWeek) {
        if (!isRepeating()) {
            return OptionalInt.empty();
        }

        int count = 1, previousDayOfWeek = compareDayOfWeek;
        do {
            previousDayOfWeek--;
            if (previousDayOfWeek < DayOfWeek.SUNDAY) {
                previousDayOfWeek = DayOfWeek.SATURDAY;
            }
            if (isBitOn(previousDayOfWeek)) {
                return OptionalInt.of(count);
            }
        } while (count++ < 7);

        return OptionalInt.empty();
    }

    /**
     * Get the distance between the next day of the week represented by this weekly repeat schedule
     * and the given day of the week, which is always between 0 and 6 inclusive.
     *
     * @param compareDayOfWeek The {@link DayOfWeek} to compare against.
     * @return An Optional containing the number of days between the given day of the week and the
     * next enabled day of the week, if any.
     */
    public OptionalInt getDistanceToNextDayOfWeek(final @DayOfWeek int compareDayOfWeek) {
        if (!isRepeating()) {
            return OptionalInt.empty();
        }

        int count = 0, nextDayOfWeek = compareDayOfWeek;
        do {
            if (isBitOn(nextDayOfWeek)) {
                return OptionalInt.of(count);
            }
            nextDayOfWeek++;
            if (nextDayOfWeek > DayOfWeek.SATURDAY) {
                nextDayOfWeek = DayOfWeek.SUNDAY;
            }
        } while (++count < 7);

        return OptionalInt.empty();
    }

    /**
     * Convert the days of the week represented by this weekly repeat schedule to comma-separated,
     * human-readable names of the days of the week ordered and formatted according to the default
     * locale.
     *
     * @param useLongNames If {@code true}, the un-abbreviated day of the week names are used, e.g.
     * Tuesday, Friday, Saturday, otherwise the abbreviated ones are used, e.g. Tue, Fri, Sat.
     * @return The formatted string list of days of the week names.
     */
    public @NonNull String toString(final boolean useLongNames) {
        if (!isRepeating()) {
            return "";
        }

        final String separator = mContext.getString(R.string.scheduler_day_of_week_separator);

        final StringBuilder builder = new StringBuilder(40);
        for (@DayOfWeek int dayOfWeek : this) {
            if (!isBitOn(dayOfWeek)) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(separator);
            }

            builder.append(getDisplayName(dayOfWeek, useLongNames));
        }
        return builder.toString();
    }

    /**
     * Get the textual representation of a {@link DayOfWeek}, such as "Fri" or "Friday"
     *
     * @param dayOfWeek Any of {@link DayOfWeek} values.
     * @param useLongName If {@code true}, the un-abbreviated day of the week names are used, e.g.
     * Tuesday, Friday, Saturday, otherwise the abbreviated ones are used, e.g. Tue, Fri, Sat.
     * @param locale The locale to use.
     */
    public @NonNull String getDisplayName(final @DayOfWeek int dayOfWeek, final boolean useLongName,
            final @NonNull Locale locale) {

        final Calendar calendar = Calendar.getInstance(locale);
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);

        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, useLongName ? Calendar.LONG :
                Calendar.SHORT, locale);
    }

    /**
     * Like {@link #getDisplayName(int,boolean,Locale)}, but use the default locale.
     */
    @NonNull
    public String getDisplayName(final @DayOfWeek int dayOfWeek, final boolean useLongName) {
        return getDisplayName(dayOfWeek, useLongName, Locale.getDefault());
    }

    /**
     * Get the textual representation of a {@link DayOfWeek}, such as "S", "M", etc.
     *
     * @param dayOfWeek Any of {@link DayOfWeek} values.
     * @return Single-character weekday name; e.g.: 'S', 'M', 'T', 'W', 'T', 'F', 'S'.
     */
    public synchronized static @NonNull String getNarrowDisplayName(final @DayOfWeek int dayOfWeek) {
        final Locale loc = Locale.getDefault();
        if (sDaysOfWeekNarrowStrings == null || !loc.equals(sDefaultLocaleCache)) {
            sDaysOfWeekNarrowStrings = DateFormatSymbols.getInstance(loc)
                .getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.NARROW);
            sDefaultLocaleCache = loc;
        }
        return sDaysOfWeekNarrowStrings[dayOfWeek];
    }

    /**
     * Return an iterator allowing iteration over all days of the week, ordered according to the
     * default locale.
     *
     * @return Iterator returning {@link DayOfWeek} int values.
     */
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            private final Calendar calendar = Calendar.getInstance(Locale.getDefault());
            private int remaining = 7;

            {
                // Set cursor to the first day of the week according to the default locale
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            }

            @Override
            public boolean hasNext() {
                return remaining != 0;
            }

            @Override
            public Integer next() {
                if (remaining == 0) {
                    throw new NoSuchElementException();
                }

                final int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

                // Move the cursor to the next day
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                remaining--;

                return dayOfWeek;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Extract the {@link DayOfWeek} value from a date-time object.
     *
     * @param ldt The date-time to get the corresponding {@link DayOfWeek} value from.
     */
    public static @DayOfWeek int getDayOfWeekFrom(final @NonNull LocalDateTime ldt) {
        return ldt.get(WeekFields.SUNDAY_START.dayOfWeek());
    }

    /**
     * Factory to create {@link DaysOfWeek} instances via the {@link AssistedInject} constructor.
     */
    @AssistedFactory
    public static abstract class Factory {
        /** To be implemented by Dagger. */
        abstract DaysOfWeek create(@Nullable Integer bits,
                @Nullable @DayOfWeek Integer... daysOfWeek);

        /** Create a {@link DaysOfWeek} instance representing an empty weekly repeat schedule. */
        public DaysOfWeek create() {
            return create(null, (Integer[]) null);
        }

        /** Create a {@link DaysOfWeek} instance using {@link #getBits} values representing the
         * encoded weekly repeat schedule. */
        public DaysOfWeek create(final int bits) {
            return create(bits, (Integer[]) null);
        }

        /** Create a {@link DaysOfWeek} instance representing any or all of the {@link DayOfWeek}
         * values. */
        public DaysOfWeek create(final @DayOfWeek int... daysOfWeek) {
            return create(null, IntStream.of(daysOfWeek).boxed().toArray(Integer[]::new));
        }

        /**
         * Create a {@link DaysOfWeek} instance representing any or all of the {@link DayOfWeek}
         * values. */
        public DaysOfWeek create(final @DayOfWeek Integer... daysOfWeek) {
            return create(null, daysOfWeek);
        }
    }

    /**
     * <p>Compare this instance against an other {@link DaysOfWeek} instance guaranteeing a natural
     * weekly repeat cycle order.
     *
     * <p>Note that, the comparison algorithm, when applied to multiple instances, will attempt to
     * first organize items representing the same amount of weekly repeating days into clusters,
     * then sort. For example,
     * <ul>
     *   <li>Cluster 1
     *     <ul>
     *       <li>Monday, Tuesday, Wednesday</li>
     *       <li>Thursday, Friday, Saturday</li>
     *     </ul>
     *   </li>
     *   <li>Cluster 2
     *     <ul>
     *       <li>Monday, Tuesday</li>
     *       <li>Wednesday, Thursday</li>
     *       <li>Friday, Saturday</li>
     *     </ul>
     *   </li>
     *   <li>Cluster 3
     *     <ul>
     *       <li>Monday</li>
     *       <li>Tuesday</li>
     *       <li>Wednesday</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>The more days are selected, the higher the cluster will appear in the list.
     */
    @Override
    public int compareTo(final DaysOfWeek daysOfWeek) {
        int ourBits = mBits;
        int theirBits = daysOfWeek.mBits;

        // The instances represent the same weekly repeat cycle
        if (ourBits == theirBits) {
            return 0;
        }

        switch (iterator().next()) {
            case DayOfWeek.MONDAY:
                // For the correct ordering, when the first day of the week is Monday according to
                // the current locale, we need to move the Sunday from the rightmost bit to the
                // leftmost bit after the Saturday
                ourBits >>= 1;
                if (isBitOn(DayOfWeek.SUNDAY)) {
                    ourBits |= 1<<6;
                }

                theirBits >>= 1;
                if (daysOfWeek.isBitOn(DayOfWeek.SUNDAY)) {
                    theirBits |= 1<<6;
                }
                break;

            case DayOfWeek.SATURDAY:
                // For the correct ordering, when the first day of the week is Saturday according to
                // the current locale, we need to move it from the leftmost bit to the rightmost bit
                // pushing the Sunday to the 1st bit position
                ourBits <<=1;
                if (isBitOn(DayOfWeek.SATURDAY)) {
                    ourBits ^= 1<<7 | 1<<0;
                }

                theirBits <<= 1;
                if (daysOfWeek.isBitOn(DayOfWeek.SATURDAY)) {
                    theirBits ^= 1<<7 | 1<<0;
                }
                break;
        }

        int result = -Integer.compare(Integer.bitCount(ourBits), Integer.bitCount(theirBits));
        if (result == 0) {
            result = ourBits < theirBits ? -1 : 1;
        }
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DaysOfWeek daysOfWeek = (DaysOfWeek) o;
        return mBits == daysOfWeek.mBits;
    }

    @Override
    public int hashCode() { return mBits; }

    /**
     * For debugging purpose. In production use {@link #toString(boolean)}.
     */
    @Override
    @NonNull
    public String toString() {
        final StringBuilder builder = new StringBuilder(19);
        builder.append("[");
        if (isBitOn(DayOfWeek.SUNDAY)) {
            builder.append(builder.length() > 1 ? " Su" : "Su");
        }
        if (isBitOn(DayOfWeek.MONDAY)) {
            builder.append(builder.length() > 1 ? " M" : "M");
        }
        if (isBitOn(DayOfWeek.TUESDAY)) {
            builder.append(builder.length() > 1 ? " T" : "T");
        }
        if (isBitOn(DayOfWeek.WEDNESDAY)) {
            builder.append(builder.length() > 1 ? " W" : "W");
        }
        if (isBitOn(DayOfWeek.THURSDAY)) {
            builder.append(builder.length() > 1 ? " Th" : "Th");
        }
        if (isBitOn(DayOfWeek.FRIDAY)) {
            builder.append(builder.length() > 1 ? " F" : "F");
        }
        if (isBitOn(DayOfWeek.SATURDAY)) {
            builder.append(builder.length() > 1 ? " Sa" : "Sa");
        }
        builder.append("]");
        return builder.toString();
    }
}
