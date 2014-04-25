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
package net.onrc.openvirtex.elements.datapath;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.core.cmd.CmdLineSettings;

import org.openflow.protocol.OFHello;

public class TranslatorTest extends TestCase {

    OpenVirteXController ctl = null;
    private XidTranslator<OVXSwitch> translator;

    public TranslatorTest(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TranslatorTest.class);
    }

    public void testTranslate() {
        final OVXSwitch vsw = new OVXSingleSwitch(1, 1);

        // make a south-bound message....something simple.
        final OFHello ofh = new OFHello();
        ofh.setXid(0);

        final int newXid = this.translator.translate(ofh.getXid(), vsw);
        Assert.assertEquals(newXid, XidTranslator.MIN_XID);
    }

    public void testUntranslate() {
        final OVXSwitch vsw = new OVXSingleSwitch(1, 1);

        final OFHello ofh = new OFHello();
        ofh.setXid(0);
        this.translator.translate(ofh.getXid(), vsw);

        final XidPair<OVXSwitch> pair = this.translator
                .untranslate(XidTranslator.MIN_XID);
        Assert.assertEquals(pair.getSwitch().getSwitchId(), vsw.getSwitchId());
        Assert.assertEquals(pair.getXid(), ofh.getXid());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ctl = new OpenVirteXController(new CmdLineSettings());
        this.translator = new XidTranslator<OVXSwitch>();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
