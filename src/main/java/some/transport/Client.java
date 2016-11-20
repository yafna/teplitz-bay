package some.transport;


import net.i2p.I2PException;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.NoRouteToHostException;

public class Client {
    private I2PSocket socket;
    public void start(String destinationString) {
        I2PSocketManager manager = I2PSocketManagerFactory.createManager();
        Destination destination;
        try {
            destination = new Destination(destinationString);
        } catch (DataFormatException ex) {
            System.out.println("Destination string incorrectly formatted.");
            return;
        }
        try {
            socket = manager.connect(destination);
        } catch (I2PException ex) {
            System.out.println("General I2P exception occurred!");
        } catch (ConnectException ex) {
            System.out.println("Failed to connect!");
        } catch (NoRouteToHostException ex) {
            System.out.println("Couldn't find host!");
        } catch (InterruptedIOException ex) {
            System.out.println("Sending/receiving was interrupted!");
        }
    }

    public String sendEcho(String str){
        try {
            //Write to server
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bw.write(str);
            //Flush to make sure everything got sent
            bw.flush();
            bw.close();
            //Read from server
            BufferedReader br2 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String s = null;
            while ((s = br2.readLine()) != null) {
                sb.append(s);
                System.out.println("Received from server: " + s);
            }
            br2.close();
            socket.close();
            return sb.toString();
        } catch (IOException ex) {
            System.out.println("Error occurred while sending/receiving!");
        }
        return null;
    }
}
