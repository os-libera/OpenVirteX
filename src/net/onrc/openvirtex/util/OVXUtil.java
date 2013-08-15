package net.onrc.openvirtex.util;

public class OVXUtil {
  
     
    public static int NUMBITSNEEDED(int x) {
        int counter = 0;
        while (x !=0) { 
            x >>= 1;
            counter++;
        }
        return counter;
    }
}
