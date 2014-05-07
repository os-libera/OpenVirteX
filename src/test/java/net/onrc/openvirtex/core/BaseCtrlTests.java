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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Parent class for controller tests.
 */
public final class BaseCtrlTests {

    /**
     * Overrides default constructor to no-op private constructor.
     * Required by checkstyle.
     */
    private BaseCtrlTests() {
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(BaseCtrlTests.class.getName());
        // $JUnit-BEGIN$
        suite.addTest(OpenVirteXControllerTest.suite());
        // $JUnit-END$
        return suite;
    }

}
