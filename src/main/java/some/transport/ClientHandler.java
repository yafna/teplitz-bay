package some.transport;

import net.i2p.I2PException;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

public class ClientHandler implements Runnable {

    public ClientHandler(I2PServerSocket socket) {
        this.socket = socket;
    }

    public void run() {
        while(true) {
            try {
                I2PSocket sock = this.socket.accept();
                if(sock != null) {
                    //Receive from clients
                    BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    //Send to clients
                    String line = br.readLine();
                    br.close();
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
                    if(line != null) {
                        System.out.println("Received from client: " + line);
                        bw.write(line);
                        bw.flush(); //Flush to make sure everything got sent
                    }
                    bw.close();
                    sock.close();
                }
            } catch (I2PException ex) {
                System.out.println("General I2P exception!");
            } catch (ConnectException ex) {
                System.out.println("Error connecting!");
            } catch (SocketTimeoutException ex) {
                System.out.println("Timeout!");
            } catch (IOException ex) {
                System.out.println("General read/write-exception!");
            }
        }
    }

    private I2PServerSocket socket;

}
