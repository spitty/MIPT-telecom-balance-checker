/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.spitty.telecom;

import java.io.IOException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author spitty
 */
public class NotifySender {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(NotifySender.class);

    public static void main(String[] args) {
        final String message = "Hello";
        sendNotification(message);
    }

    public static void sendNotification(final String message) {
        LOGGER.debug("Notify with message \"{}\"", message);
        try {
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/notify-send", message);
            Process p = pb.start();
            p.waitFor();
        } catch (InterruptedException | IOException ex) {
            LOGGER.error("Error during notification sending", ex);
        }
    }
}
