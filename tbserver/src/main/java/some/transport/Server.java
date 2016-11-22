package some.transport;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PException;
import net.i2p.client.I2PSession;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.util.I2PThread;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Server {
    @Getter
    private String myDestination;
    private Map<String, I2PSocket> connections = new HashMap<>();
    private I2PSocketManager manager;

    public Server() {
        init();
    }

    private void init(){
        manager = I2PSocketManagerFactory.createManager();
        I2PServerSocket serverSocket = manager.getServerSocket();
        I2PSession session = manager.getSession();
        myDestination = session.getMyDestination().toBase64();
        log.info("Started server with destination: " + myDestination);

        //Create socket to handle clients
        I2PThread t = new I2PThread(new ClientHandler(serverSocket));
        t.setName("clienthandler1");
        t.setDaemon(false);
        t.start();
    }


    public void disconnectWith(String destinationAddr) {
        connections.remove(destinationAddr);
    }

    public void connectTo(String destinationAddr) {
        Destination destination = null;
        try {
            destination = new Destination(destinationAddr);
        } catch (DataFormatException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        try {
            I2PSocket socket = manager.connect(destination);
            connections.put(destinationAddr, socket);
        } catch (I2PException ex) {
            log.error("General I2P exception occurred", ex);
        } catch (ConnectException ex) {
            log.error("Failed to connect", ex);
        } catch (NoRouteToHostException ex) {
            log.error("Couldn't find host", ex);
        } catch (InterruptedIOException ex) {
            log.error("Sending/receiving was interrupted", ex);
        }
    }

    public String sendEcho(String destinationAddr, String echoStr){
        I2PSocket socket = connections.get(destinationAddr);
        if(socket == null){
            log.error("Send echo failed to " + destinationAddr);
            return null;
        }
        try {
            //Write to server
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bw.write(echoStr);
            //Flush to make sure everything got sent
            bw.flush();
            bw.close();
            //Read from server
            BufferedReader br2 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String s = null;
            while ((s = br2.readLine()) != null) {
                sb.append(s);
                log.error("Received from server: " + s);
            }
            br2.close();
            socket.close();
            return sb.toString();
        } catch (IOException ex) {
            log.error("Error occurred while sending/receiving!", ex);
        }
        return null;
    }
}
