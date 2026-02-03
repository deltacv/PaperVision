/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
