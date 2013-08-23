package net.onrc.openvirtex.core;

public class OpenVirtexShutdownHook extends Thread {

    private final OpenVirteXController ctrl;

    public OpenVirtexShutdownHook(final OpenVirteXController ctrl) {
	this.ctrl = ctrl;
    }

    @Override
    public void run() {
	this.ctrl.terminate();
    }
}
