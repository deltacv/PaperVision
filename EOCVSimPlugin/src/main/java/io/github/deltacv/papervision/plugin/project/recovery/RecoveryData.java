package io.github.deltacv.papervision.plugin.project.recovery;

import com.google.gson.Gson;

public class RecoveryData {

    public String recoveryFolderPath;
    public String recoveryFileName;
    public RecoveredProject projectData;

    private static final Gson gson = new Gson();

    public RecoveryData(String recoveryFolderPath, String recoveryFileName, RecoveredProject projectData) {
        this.recoveryFolderPath = recoveryFolderPath;
        this.recoveryFileName = recoveryFileName;
        this.projectData = projectData;
    }

    public static String serialize(RecoveryData data) {
        return gson.toJson(data);
    }

    public static RecoveryData deserialize(String json) {
        return gson.fromJson(json, RecoveryData.class);
    }
}