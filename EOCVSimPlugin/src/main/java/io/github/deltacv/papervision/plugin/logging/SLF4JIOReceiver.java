package io.github.deltacv.papervision.plugin.logging;

import com.github.serivesmejia.eocvsim.util.JavaProcess;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Scanner;

public class SLF4JIOReceiver implements JavaProcess.ProcessIOReceiver {

    private static int instanceCount = 0;

    private final Logger logger;

    public SLF4JIOReceiver(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void receive(InputStream out, InputStream err) {
        instanceCount += 1;

        new Thread(() -> {
            Scanner sc = new Scanner(out);
            while (sc.hasNextLine()) {
                logger.info(sc.nextLine());
            }
        }, "SLFJ4IOReceiver-in-" + instanceCount).start();

        new Thread(() -> {
            Scanner sc = new Scanner(err);
            while (sc.hasNextLine()) {
                logger.error(sc.nextLine());
            }
        }, "SLF4JIOReceiver-out-" + instanceCount).start();
    }

}
