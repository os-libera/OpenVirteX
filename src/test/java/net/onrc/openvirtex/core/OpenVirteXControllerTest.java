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
package net.onrc.openvirtex.core;

import net.onrc.openvirtex.core.cmd.CmdLineSettings;
import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OpenVirteXControllerTest extends TestCase {

    /**
     * Creates the test case.
     *
     * @param testName
     *            name of the test case
     */
    public OpenVirteXControllerTest(final String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static TestSuite suite() {
        return new TestSuite(OpenVirteXControllerTest.class);
    }

    /**
     * Tests that a call to getInstance on the controller fails with a
     * RuntimeException and that that the returned object is null.
     */
    public void testNull() {
        OpenVirteXController ctrl = null;
        try {
            ctrl = OpenVirteXController.getInstance();
        } catch (final RuntimeException e) {
            Assert.assertNull(ctrl);
        }

    }

    public void testNotNull() {
        final OpenVirteXController ovx = new OpenVirteXController(
                new CmdLineSettings());
        Assert.assertNotNull(OpenVirteXController.getInstance());
        Assert.assertEquals(ovx, OpenVirteXController.getInstance());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
