package org.tron.walletserver;

import java.net.InetSocketAddress;
import java.net.Socket;

public class HttpUtils {

    /*
     * Overriding default InetAddress.isReachable() method to add 2 more arguments port and timeout value
     *
     * Address: www.google.com
     * port: 80 or 443
     * timeout: 2000 (in milliseconds)
     */
    public static long crunchifyAddressReachable(String address, int port, int timeout) {
        try {

            long time = System.currentTimeMillis();
            try (Socket crunchifySocket = new Socket()) {
                // Connects this socket to the server with a specified timeout value.
                crunchifySocket.connect(new InetSocketAddress(address, port), timeout);
            }
            // Return true if connection successful
            return (System.currentTimeMillis() - time);
        } catch (Exception exception) {
            // Return false if connection fails
            return -1;
        }
    }

}
