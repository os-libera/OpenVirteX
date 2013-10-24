/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package net.onrc.openvirtex.routing;

import net.onrc.openvirtex.exceptions.RoutingAlgorithmException;

public class RoutingAlgorithms {
    public enum RoutingType {

	NONE("manual"), SPF("spf");

	protected String value;

	private RoutingType(final String value) {
	    this.value = value;
	}

	public String getValue() {
	    return this.value;
	}

    }

    protected final RoutingType type;
    protected final Routable routing;
    protected final byte backups;
    
    public RoutingAlgorithms(final String type, final byte backups) throws RoutingAlgorithmException {
	if (type.equals(RoutingType.NONE.getValue())) {
	    this.type = RoutingType.NONE;
	    this.routing = new ManualRoute();
	} 
	else if (type.equals(RoutingType.SPF.getValue())) {
	    this.type = RoutingType.SPF;
	    this.routing = new ShortestPath();
	}
	else throw new RoutingAlgorithmException("The algorithm " + type + " is not supported. Supported values are " 
	+ RoutingType.NONE.getValue() + ", " + RoutingType.SPF.getValue());
	this.backups = backups;
    }

    public RoutingType getRoutingType() {
	return this.type;
    }
    
    public Routable getRoutable() {
	return this.routing;
    }

    public byte getBackups() {
        return backups;
    }

}
