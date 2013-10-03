/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
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
