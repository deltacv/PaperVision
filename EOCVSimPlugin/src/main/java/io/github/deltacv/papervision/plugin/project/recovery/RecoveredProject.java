package io.github.deltacv.papervision.plugin.project.recovery;

import com.google.gson.Gson;
import io.github.deltacv.papervision.plugin.project.PaperVisionProject;

public class RecoveredProject {
    public String originalProjectPath;
    public long date;
    public String hash;
    public PaperVisionProject project;

    private static final Gson gson = new Gson();

    public RecoveredProject(String originalProjectPath, long date, String hash, PaperVisionProject project) {
        this.originalProjectPath = originalProjectPath;
        this.date = date;
        this.hash = hash;
        this.project = project;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return "RecoveredProject{" +
                "path='" + originalProjectPath + '\'' +
                ", date=" + date +
                ", hash='" + hash + '\'' +
                '}';
    }

    public static RecoveredProject fromJson(String json) {
        return gson.fromJson(json, RecoveredProject.class);
    }
}
