package com.gemlightbox.core.logo;

import com.gemlightbox.core.preference.core.CameraPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class LogoSettingsManager {
    private static LogoSettingsManager logoSettingsManager;

    public List<LogoItem> logoItemList = new ArrayList<>();
    private boolean logoEnable;

    public static LogoSettingsManager getInstance(CameraPreferences preference) {

        if (logoSettingsManager == null) {

            logoSettingsManager = new LogoSettingsManager();

            if (preference.getLogoList() != null) {
                logoSettingsManager.logoItemList = new Gson().fromJson(preference.getLogoList(), new TypeToken<ArrayList<LogoItem>>(){}.getType());
            }

            logoSettingsManager.setLogoEnable(preference.isLogoEnabled());
        }
        return logoSettingsManager;
    }

    public static void saveToPrefs(CameraPreferences preference) {
        if (logoSettingsManager != null) {
            preference.setLogoList(new Gson().toJson(logoSettingsManager.logoItemList));
            preference.setLogoEnabled(logoSettingsManager.logoEnable);
        }
    }

    public static void cleanInstance() {
        if (logoSettingsManager != null) {
            logoSettingsManager = null;
        }
    }

    public boolean isLogoEnable() {
        return logoEnable;
    }

    public void setLogoEnable(boolean logoEnable) {
        this.logoEnable = logoEnable;
    }
}
