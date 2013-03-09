/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.spitty.telecom;

import java.io.IOException;
import org.slf4j.LoggerFactory;

/**
 * This class is used for send notifications via <a href="http://manpages.ubuntu.com/manpages/precise/en/man1/notify-send.1.html">notify-send</a>
 * 
 * @author spitty
 */
public class NotifySender {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(NotifySender.class);

    /**
     * Send a notification via 
     * <a href="http://manpages.ubuntu.com/manpages/precise/en/man1/notify-send.1.html">notify-send</a>
     * 
     * @param message {@link String} message to be shown
     */
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
