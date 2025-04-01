package com.darilink;

import android.app.Application;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class DariLinkApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dexgdhqub");
        config.put("api_key", "873648929983358");
        config.put("api_secret", "d7urCPLVKWphp2TOQ4vusBI_TWM");
        config.put("secure", "true");

        MediaManager.init(this, config);
    }
}