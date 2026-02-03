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