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
package net.onrc.openvirtex;

import junit.framework.Test;
import junit.framework.TestSuite;
import net.onrc.openvirtex.api.service.handlers.tenant.APITests;
import net.onrc.openvirtex.core.BaseCtrlTests;
import net.onrc.openvirtex.elements.BaseMapTests;
import net.onrc.openvirtex.elements.address.BaseIPTests;
import net.onrc.openvirtex.elements.datapath.BaseTranslatorTests;

/**
 * Parent class for tests.
 */
public final class AllTests {

    /**
     * Overrides default constructor to no-op private constructor.
     * Required by checkstyle.
     */
    private AllTests() {
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(AllTests.class.getName());
        // $JUnit-BEGIN$
        suite.addTest(BaseCtrlTests.suite());
        suite.addTest(BaseMapTests.suite());
        suite.addTest(BaseIPTests.suite());
        suite.addTest(BaseTranslatorTests.suite());
        suite.addTest(APITests.suite());
        // $JUnit-END$
        return suite;
    }

}
