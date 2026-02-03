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