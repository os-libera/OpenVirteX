/*
 * ******************************************************************************
 *  Copyright 2019 Korea University & Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ******************************************************************************
 *  Developed by Libera team, Operating Systems Lab of Korea University
 *  ******************************************************************************
 */

package net.onrc.openvirtex.linkdiscovery;

import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.messages.OVXMessage;

/**
 * Created by Administrator on 2016-04-21.
 */
public interface LLDPEventHandler {
    public void handleLLDP(OVXMessage msg, Switch sw);
}
