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

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecoveryDaemonClientMain extends WebSocketClient {

    public static final int MAX_CONNECTION_ATTEMPTS_BEFORE_EXITING = 3;

    public static final Logger logger = LoggerFactory.getLogger(RecoveryDaemonClientMain.class);

    public RecoveryDaemonClientMain(int port) throws URISyntaxException {
        super(new URI("ws://127.0.0.1:" + port));
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("Connected to recovery daemon server at port {}", uri.getPort());
    }

    @Override
    public void onMessage(String message) {
        try {
            RecoveryData recoveryData = RecoveryData.deserialize(message);
            Path recoveryPath = Paths.get(recoveryData.recoveryFolderPath);

            if (!Files.exists(recoveryPath)) {
                Files.createDirectories(recoveryPath);
            }

            Path recoveryFilePath = recoveryPath.resolve(recoveryData.recoveryFileName);
            Files.write(recoveryFilePath, recoveryData.projectData.toJson().getBytes());
        } catch (Exception e) {
            logger.error("Failed to save recovery data", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
    }

    @Override
    public void onError(Exception ex) {
    }

    public static void main(String[] args) {
        System.setProperty("apple.awt.UIElement", "true");

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 17112;

        RecoveryDaemonClientMain client = null;
        int connectionAttempts = 0;

        try {
            while(!Thread.interrupted()) {
                if(connectionAttempts >= MAX_CONNECTION_ATTEMPTS_BEFORE_EXITING) {
                    logger.warn("Failed to connect to recovery daemon after " + MAX_CONNECTION_ATTEMPTS_BEFORE_EXITING + " attempts. Exiting...");
                    System.exit(0);
                }

                if(client == null || client.isClosed()) {
                    client = new RecoveryDaemonClientMain(port);
                    client.connect();
                    connectionAttempts += 1;
                }

                Thread.sleep(100);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch(InterruptedException ignored) { }
    }
}