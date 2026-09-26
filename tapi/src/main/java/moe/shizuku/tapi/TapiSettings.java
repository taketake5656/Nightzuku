package moe.shizuku.tapi;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists the TAPI (Termux API bridge) toggle.
 */
public final class TapiSettings {

    private static final String PREFS_NAME = "tapi";
    private static final String KEY_ENABLED = "enabled";

    private static SharedPreferences preferences;

    private TapiSettings() {
    }

    public static synchronized void init(Context context) {
        if (preferences == null) {
            preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public static boolean isEnabled() {
        return preferences != null && preferences.getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(boolean enabled) {
        if (preferences == null) {
            throw new IllegalStateException("TapiSettings.init() must be called first");
        }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
