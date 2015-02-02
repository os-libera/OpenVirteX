package net.onrc.openvirtex.routing.nat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.onrc.openvirtex.core.OpenVirtexShutdownHook;
import net.onrc.openvirtex.elements.address.OVXIPAddress;
import net.onrc.openvirtex.packet.IPv4;

public class Play {

    public static void main(String[] args) {
        ip();
//        map();

    }

    private static void map() {
        HashMap<String, ArrayList<String>> map = new HashMap<>();
        ArrayList<String> l = new ArrayList<>();
        l.add("a");
        l.add("b");
        l.add("c");
        l.add("d");
        map.put("1", l);
        Iterator<String> i = map.get("1").iterator();
        System.out.println(i.next());
        i.remove();

        System.out.println(map);
        System.out.println(i.next());
        i.remove();

        System.out.println(map);
    }

    private static void ip() {
        try {
            InetAddress address = InetAddress.getByName("192.168.1.1");
            System.out.println(address);
            System.out.println(address.getHostAddress());
            OVXIPAddress ovxipAddress = new OVXIPAddress(address.getHostAddress(), 1);
            System.out.println(ovxipAddress.toSimpleString());
            byte[] ip = IPv4.toIPv4AddressBytes(ovxipAddress.getIp());
            ip[3] = (byte) 254;
            System.out.println(InetAddress.getByAddress(ip).getHostAddress());

            ip = IPv4.toIPv4AddressBytes("10.0.0.0");
            ip[3] = (byte) 254;
            String gatewayIp;
            gatewayIp = InetAddress.getByAddress(ip).getHostAddress();
            System.out.println(gatewayIp);

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
