/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public RoutingAlgorithms(final String type, final byte backups)
            throws RoutingAlgorithmException {
        if (type.equals(RoutingType.NONE.getValue())) {
            this.type = RoutingType.NONE;
            this.routing = new ManualRoute();
        } else if (type.equals(RoutingType.SPF.getValue())) {
            this.type = RoutingType.SPF;
            this.routing = new ShortestPath();
        } else {
            throw new RoutingAlgorithmException("The algorithm " + type
                    + " is not supported." + "Supported values are "
                    + RoutingType.NONE.getValue() + ", "
                    + RoutingType.SPF.getValue());
        }
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
