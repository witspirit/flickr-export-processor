package be.witspirit.flickr.exportprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public enum Transfer {
    COPY("Copy", "Copied", "Copying", Files::copy),
    MOVE("Move", "Moved", "Moving", Files::move)
    ;

    private static final Logger LOG = LoggerFactory.getLogger(Transfer.class);

    private String action;
    private String actionCompleted;
    private String actionInProgress;
    private TransferOperation operation;

    Transfer(String action, String actionCompleted, String actionInProgress, TransferOperation operation) {
        this.action = action;
        this.actionCompleted = actionCompleted;
        this.actionInProgress = actionInProgress;
        this.operation = operation;
    }

    public void transfer(Path source, Path destination) {
        LOG.debug(actionInProgress+" {} -> {}...", source, destination);
        if (destinationAlreadyPresent(destination, source)) {
            LOG.debug("Destination {} already exists. Skipping...", destination);
        } else {
            try {
                operation.transfer(source, destination);
                LOG.debug(actionCompleted+" {} -> {}", source, destination);
            } catch (IOException e) {
                throw new RuntimeException("Failed to "+action.toLowerCase() + source + " -> " + destination, e);
            }
        }
    }

    private boolean destinationAlreadyPresent(Path destination, Path source) {
        if (!Files.exists(destination)) {
            return false;
        }

        try {
            long sourceSize = Files.size(source);
            long destinationSize = Files.size(destination);

            if (destinationSize == sourceSize) {
                return true;
            }
            throw new RuntimeException("Destination "+destination+" already present, but different from "+source);
        } catch (IOException e) {
            throw new RuntimeException("Failed to obtain file size for determining destination mismatch for "+destination, e);
        }
    }

    @FunctionalInterface
    private interface TransferOperation {
        void transfer(Path source, Path destination) throws IOException;
    }
}
