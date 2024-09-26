package io.github.deltacv.papervision.plugin;

import com.google.gson.JsonElement;
import io.github.deltacv.papervision.engine.client.response.JsonElementResponse;
import io.github.deltacv.papervision.engine.client.response.OkResponse;
import io.github.deltacv.papervision.platform.lwjgl.PaperVisionApp;
import io.github.deltacv.papervision.plugin.gui.CloseConfirmWindow;
import io.github.deltacv.papervision.plugin.ipc.EOCVSimIpcEngineBridge;
import io.github.deltacv.papervision.plugin.ipc.message.DiscardCurrentRecoveryMessage;
import io.github.deltacv.papervision.plugin.ipc.message.EditorChangeMessage;
import io.github.deltacv.papervision.plugin.ipc.message.GetCurrentProjectMessage;
import io.github.deltacv.papervision.plugin.ipc.message.SaveCurrentProjectMessage;
import io.github.deltacv.papervision.serialization.PaperVisionSerializer;
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

public class EOCVSimIpcPaperVisionMain implements Callable<Integer> {

    @CommandLine.Option(names = {"-p", "--port"}, description = "Engine IPC server port")
    public int port;

    @CommandLine.Option(names = {"-q", "--queryproject"}, description = "Asks the engine for the current project")
    public boolean queryProject;

    private PaperVisionApp app;

    private Logger logger = LoggerFactory.getLogger(EOCVSimIpcPaperVisionMain.class);

    @Override
    public Integer call() {
        logger.info("IPC port {}", port);

        EOCVSimIpcEngineBridge bridge = new EOCVSimIpcEngineBridge(port);

        app = new PaperVisionApp(
                false, bridge, this::paperVisionUserCloseListener
        );

        app.getPaperVision().getOnUpdate().doOnce(() -> {
            if(queryProject) {
                app.getPaperVision().getEngineClient().sendMessage(new GetCurrentProjectMessage().onResponse((response) -> {
                    if(response instanceof JsonElementResponse) {
                        JsonElement json = ((JsonElementResponse) response).getValue();

                        app.getPaperVision().getOnUpdate().doOnce(() -> {}
                                // PaperVisionSerializer.INSTANCE.deserializeAndApply(json, app.getPaperVision())
                        );
                    }
                }));
            }

            app.getPaperVision().getNodeEditor().getOnEditorChange().doOnce(() ->
                    app.getPaperVision().getEngineClient().sendMessage(new EditorChangeMessage(
                            PaperVisionSerializer.INSTANCE.serializeToTree(
                                    app.getPaperVision().getNodes().getInmutable(), app.getPaperVision().getLinks().getInmutable()
                            )
                    ))
            );
        });

        app.start();

        return 0;
    }

    private boolean paperVisionUserCloseListener() {
        new CloseConfirmWindow((action) -> {
            switch (action) {
                case YES:
                    app.getPaperVision().getEngineClient().sendMessage(new SaveCurrentProjectMessage(
                            PaperVisionSerializer.INSTANCE.serializeToTree(
                                    app.getPaperVision().getNodes().getInmutable(), app.getPaperVision().getLinks().getInmutable()
                            )
                    ).onResponse((response) -> {
                        if(response instanceof OkResponse) {
                            System.exit(0);
                        }
                    }));
                    break;
                case NO:
                    app.getPaperVision().getEngineClient().sendMessage(new DiscardCurrentRecoveryMessage().onResponse((response) -> {
                        if(response instanceof OkResponse) {
                            System.exit(0);
                        }
                    }));
                    break;
                    default: // NO-OP
            }

            return Unit.INSTANCE;
        }).enable();

        return false;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new EOCVSimIpcPaperVisionMain()).execute(args);
        System.exit(exitCode);
    }

}