/*
 * Copyright (C) 2020 Muntashir Al-Islam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.muntashirakon.AppManager.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import io.github.muntashirakon.AppManager.AppManager;
import io.github.muntashirakon.AppManager.backup.BackupFlags;
import io.github.muntashirakon.AppManager.details.AppDetailsFragment;
import io.github.muntashirakon.AppManager.main.MainActivity;

public class AppPref {
    private static final String PREF_NAME = "preferences";

    private static final int PREF_SKIP = 5;

    /**
     * Preference keys. It's necessary to do things manually as the shared prefs in Android is
     * literary unusable.
     * <br/>
     * Keep these in sync with {@link #getDefaultValue(PrefKey)}.
     */
    public enum PrefKey {
        PREF_ADB_MODE_ENABLED_BOOL,
        PREF_APP_OP_SHOW_DEFAULT_BOOL,
        PREF_APP_OP_SORT_ORDER_INT,
        PREF_APP_THEME_INT,
        PREF_BACKUP_FLAGS_INT,
        PREF_COMPONENTS_SORT_ORDER_INT,
        PREF_CUSTOM_LOCALE_STR,
        PREF_ENABLE_KILL_FOR_SYSTEM_BOOL,
        PREF_GLOBAL_BLOCKING_ENABLED_BOOL,
        PREF_LAST_VERSION_CODE_LONG,
        PREF_MAIN_WINDOW_FILTER_FLAGS_INT,
        PREF_MAIN_WINDOW_SORT_ORDER_INT,
        PREF_OPEN_PGP_PACKAGE_STR,
        PREF_OPEN_PGP_USER_ID_STR,
        PREF_PERMISSIONS_SORT_ORDER_INT,
        PREF_ROOT_MODE_ENABLED_BOOL,
        PREF_SHOW_DISCLAIMER_BOOL,
        PREF_USAGE_ACCESS_ENABLED_BOOL;

        public static final String[] keys = new String[values().length];
        @Type
        public static final int[] types = new int[values().length];
        public static final List<PrefKey> prefKeyList = Arrays.asList(values());

        static {
            String keyStr;
            int typeSeparator;
            PrefKey[] keyValues = values();
            for (int i = 0; i < keyValues.length; ++i) {
                keyStr = keyValues[i].name();
                typeSeparator = keyStr.lastIndexOf('_');
                keys[i] = keyStr.substring(PREF_SKIP, typeSeparator).toLowerCase(Locale.ROOT);
                types[i] = inferType(keyStr.substring(typeSeparator + 1));
            }
        }

        public static int indexOf(PrefKey key) {
            return prefKeyList.indexOf(key);
        }

        @Type
        private static int inferType(@NonNull String typeName) {
            switch (typeName) {
                case "BOOL":
                    return TYPE_BOOLEAN;
                case "FLOAT":
                    return TYPE_FLOAT;
                case "INT":
                    return TYPE_INTEGER;
                case "LONG":
                    return TYPE_LONG;
                case "STR":
                    return TYPE_STRING;
                default:
                    throw new IllegalArgumentException("Unsupported type.");
            }
        }
    }

    @IntDef(value = {
            TYPE_BOOLEAN,
            TYPE_FLOAT,
            TYPE_INTEGER,
            TYPE_LONG,
            TYPE_STRING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public static final int TYPE_BOOLEAN = 0;
    public static final int TYPE_FLOAT = 1;
    public static final int TYPE_INTEGER = 2;
    public static final int TYPE_LONG = 3;
    public static final int TYPE_STRING = 4;

    private static AppPref appPref;

    public static AppPref getInstance() {
        if (appPref == null) {
            Context context = AppManager.getInstance();
            appPref = new AppPref(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
        }
        return appPref;
    }

    @NonNull
    public static Object get(PrefKey key) {
        int index = PrefKey.indexOf(key);
        AppPref appPref = getInstance();
        switch (PrefKey.types[index]) {
            case TYPE_BOOLEAN:
                return appPref.preferences.getBoolean(PrefKey.keys[index], (boolean) appPref.getDefaultValue(PrefKey.prefKeyList.get(index)));
            case TYPE_FLOAT:
                return appPref.preferences.getFloat(PrefKey.keys[index], (float) appPref.getDefaultValue(PrefKey.prefKeyList.get(index)));
            case TYPE_INTEGER:
                return appPref.preferences.getInt(PrefKey.keys[index], (int) appPref.getDefaultValue(PrefKey.prefKeyList.get(index)));
            case TYPE_LONG:
                return appPref.preferences.getLong(PrefKey.keys[index], (long) appPref.getDefaultValue(PrefKey.prefKeyList.get(index)));
            case TYPE_STRING:
                return Objects.requireNonNull(appPref.preferences.getString(PrefKey.keys[index], (String) appPref.getDefaultValue(PrefKey.prefKeyList.get(index))));
        }
        throw new IllegalArgumentException("Unknown key or type.");
    }

    public static boolean isAdbEnabled() {
        return getInstance().getBoolean(PrefKey.PREF_ADB_MODE_ENABLED_BOOL);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isGlobalBlockingEnabled() {
        return getInstance().getBoolean(PrefKey.PREF_GLOBAL_BLOCKING_ENABLED_BOOL);
    }

    public static boolean isRootEnabled() {
        return getInstance().getBoolean(PrefKey.PREF_ROOT_MODE_ENABLED_BOOL);
    }

    public static boolean isRootOrAdbEnabled() {
        return isRootEnabled() || isAdbEnabled();
    }

    public static void set(PrefKey key, Object value) {
        getInstance().setPref(key, value);
    }

    @NonNull
    private SharedPreferences preferences;
    @NonNull
    private SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    private AppPref(@NonNull SharedPreferences preferences) {
        this.preferences = preferences;
        editor = preferences.edit();
        init();
    }

    public boolean getBoolean(PrefKey key) {
        int index = PrefKey.indexOf(key);
        return preferences.getBoolean(PrefKey.keys[index], (boolean) getDefaultValue(PrefKey.prefKeyList.get(index)));
    }

    public int getInt(PrefKey key) {
        int index = PrefKey.indexOf(key);
        return preferences.getInt(PrefKey.keys[index], (int) getDefaultValue(PrefKey.prefKeyList.get(index)));
    }

    public void setPref(PrefKey key, Object value) {
        int index = PrefKey.indexOf(key);
        if (value instanceof Boolean) editor.putBoolean(PrefKey.keys[index], (Boolean) value);
        else if (value instanceof Float) editor.putFloat(PrefKey.keys[index], (Float) value);
        else if (value instanceof Integer) editor.putInt(PrefKey.keys[index], (Integer) value);
        else if (value instanceof Long) editor.putLong(PrefKey.keys[index], (Long) value);
        else if (value instanceof String) editor.putString(PrefKey.keys[index], (String) value);
        editor.apply();
        editor.commit();
    }

    private void init() {
        for (int i = 0; i < PrefKey.keys.length; ++i) {
            if (!preferences.contains(PrefKey.keys[i])) {
                switch (PrefKey.types[i]) {
                    case TYPE_BOOLEAN:
                        editor.putBoolean(PrefKey.keys[i], (boolean) getDefaultValue(PrefKey.prefKeyList.get(i)));
                        break;
                    case TYPE_FLOAT:
                        editor.putFloat(PrefKey.keys[i], (float) getDefaultValue(PrefKey.prefKeyList.get(i)));
                        break;
                    case TYPE_INTEGER:
                        editor.putInt(PrefKey.keys[i], (int) getDefaultValue(PrefKey.prefKeyList.get(i)));
                        break;
                    case TYPE_LONG:
                        editor.putLong(PrefKey.keys[i], (long) getDefaultValue(PrefKey.prefKeyList.get(i)));
                        break;
                    case TYPE_STRING:
                        editor.putString(PrefKey.keys[i], (String) getDefaultValue(PrefKey.prefKeyList.get(i)));
                }
            }
        }
        editor.apply();
    }

    @NonNull
    private Object getDefaultValue(@NonNull PrefKey key) {
        switch (key) {
            case PREF_BACKUP_FLAGS_INT:
                return BackupFlags.BACKUP_SOURCE | BackupFlags.BACKUP_DATA
                        | BackupFlags.BACKUP_RULES | BackupFlags.BACKUP_EXCLUDE_CACHE
                        | BackupFlags.BACKUP_SOURCE_APK_ONLY;
            case PREF_ROOT_MODE_ENABLED_BOOL:
            case PREF_ADB_MODE_ENABLED_BOOL:
            case PREF_ENABLE_KILL_FOR_SYSTEM_BOOL:
            case PREF_GLOBAL_BLOCKING_ENABLED_BOOL:
                return false;
            case PREF_APP_OP_SHOW_DEFAULT_BOOL:
            case PREF_USAGE_ACCESS_ENABLED_BOOL:
            case PREF_SHOW_DISCLAIMER_BOOL:
                return true;
            case PREF_LAST_VERSION_CODE_LONG:
                return 0L;
            case PREF_APP_THEME_INT:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            case PREF_MAIN_WINDOW_FILTER_FLAGS_INT:
                return MainActivity.FILTER_NO_FILTER;
            case PREF_MAIN_WINDOW_SORT_ORDER_INT:
                return MainActivity.SORT_BY_APP_LABEL;
            case PREF_CUSTOM_LOCALE_STR:
                return LangUtils.LANG_AUTO;
            case PREF_APP_OP_SORT_ORDER_INT:
            case PREF_COMPONENTS_SORT_ORDER_INT:
            case PREF_PERMISSIONS_SORT_ORDER_INT:
                return AppDetailsFragment.SORT_BY_NAME;
            case PREF_OPEN_PGP_PACKAGE_STR:
            case PREF_OPEN_PGP_USER_ID_STR:
                return "";
        }
        throw new IllegalArgumentException("Pref key not found.");
    }
}
