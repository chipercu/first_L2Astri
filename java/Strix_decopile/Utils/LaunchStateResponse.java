package Strix_decopile.Utils;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import java.util.NoSuchElementException;

public enum LaunchStateResponse {
    RESPONSE_LAUNCHED_NORMAL(2100, "Client launched from system binary"),
    RESPONSE_LAUNCHED_FROM_LAUNCHER(2101, "Client launched from launcher(updater)"),
    RESPONSE_LAUNCHED_ON_VIRTUAL_MACHINE(2102, "Client launched on virtual machine"),
    RESPONSE_LAUNCHED_ON_VIRTUAL_MACHIME_AND_FROM_LAUNCHER(2103, "Client launched on virtual machine from updater");

    private final int response;
    private final String description;

    private LaunchStateResponse(int response, String description) {
        this.response = response;
        this.description = description;
    }

    public int getResponse() {
        return this.response;
    }

    public String getDescription() {
        return this.description;
    }

    public static LaunchStateResponse valueOf(int id) {
        LaunchStateResponse[] values = values();
        int length = values.length;

        for(int i = 0; i < length; ++i) {
            LaunchStateResponse detectionResponse = values[i];
            if (detectionResponse.getResponse() == id) {
                return detectionResponse;
            }
        }

        throw new NoSuchElementException("Not find LaunchStateResponse by id: " + id);
    }
}
