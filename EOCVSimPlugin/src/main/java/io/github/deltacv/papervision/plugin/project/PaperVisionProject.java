package io.github.deltacv.papervision.plugin.project;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class PaperVisionProject {

    public long timestamp;
    public String path;
    public String name;
    public JsonElement json;

    private static final Gson gson = new Gson();

    public PaperVisionProject(long timestamp, String path, String name, JsonElement json) {
        this.timestamp = timestamp;
        this.path = path;
        this.name = name;
        this.json = json;
    }

    public static PaperVisionProject fromJson(String jsonString) {
        return gson.fromJson(jsonString, PaperVisionProject.class);
    }

    public String toJson() {
        return gson.toJson(this);
    }
}