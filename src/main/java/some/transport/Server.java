package some.transport;

import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

import java.io.IOException;

public class Server {

    final private Peer peer;

    public Server(int portToBootstrap, int port) throws IOException {
        peer = new PeerMaker(Number160.createHash(port)).setPorts(port).makeAndListen();
        FutureBootstrap fb = peer.bootstrap().setBroadcast().setPorts(portToBootstrap).start();
        fb.awaitUninterruptibly();
        if (fb.getBootstrapTo() != null) {
            peer.discover().setPeerAddress(fb.getBootstrapTo().iterator().next()).start().awaitUninterruptibly();
        }
    }


    public String get(String name) throws IOException, ClassNotFoundException {
        FutureDHT futureDHT = peer.get(Number160.createHash(name)).start();
        futureDHT.awaitUninterruptibly();
        if (futureDHT.isSuccess()) {
            return futureDHT.getData().getObject().toString();
        }
        return "not found";
    }

    public void store(String name, String ip) throws IOException {
        peer.put(Number160.createHash(name)).setData(new Data(ip)).start().awaitUninterruptibly();
    }

    public Data getData(String name) throws IOException, ClassNotFoundException {
        FutureDHT futureDHT = peer.get(Number160.createHash(name)).start();
        futureDHT.awaitUninterruptibly();
        if (futureDHT.isSuccess()) {
            return futureDHT.getData();
        }
        return null;
    }

    public FutureDHT getFutureDHT(String name) throws IOException, ClassNotFoundException {
        return peer.get(Number160.createHash(name)).start();
    }

    public void store(String name, Data data) throws IOException {
        peer.put(Number160.createHash(name)).setData(data).start().awaitUninterruptibly();
    }
}
