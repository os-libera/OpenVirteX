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
/**
 *    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
 *    University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.openflow.protocol;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

/**
 * Extra info for how to treat OFMatch as a JavaBean
 *
 * For some (inane!) reason, using chained setters in OFMatch breaks a lot of
 * the JavaBean defaults.
 *
 * We don't really use OFMatch as a java bean, but there are a lot of nice XML
 * utils that work for free if OFMatch follows the java bean paradigm.
 *
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 *
 */

public class OFMatchBeanInfo extends SimpleBeanInfo {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        final List<PropertyDescriptor> descs = new LinkedList<PropertyDescriptor>();
        final Field[] fields = OFMatch.class.getDeclaredFields();
        String name;
        for (final Field field : fields) {
            final int mod = field.getModifiers();
            if (Modifier.isFinal(mod) || // don't expose static or final fields
                    Modifier.isStatic(mod)) {
                continue;
            }

            name = field.getName();
            final Class<?> type = field.getType();

            try {
                descs.add(new PropertyDescriptor(name, this.name2getter(
                        OFMatch.class, name), this.name2setter(OFMatch.class,
                        name, type)));
            } catch (final IntrospectionException e) {
                throw new RuntimeException(e);
            }
        }

        return descs.toArray(new PropertyDescriptor[0]);
    }

    private Method name2setter(final Class<OFMatch> c, final String name,
            final Class<?> type) {
        final String mName = "set" + this.toLeadingCaps(name);
        Method m = null;
        try {
            m = c.getMethod(mName, new Class[] { type });
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    private Method name2getter(final Class<OFMatch> c, final String name) {
        final String mName = "get" + this.toLeadingCaps(name);
        Method m = null;
        try {
            m = c.getMethod(mName, new Class[] {});
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    private String toLeadingCaps(final String s) {
        final char[] array = s.toCharArray();
        array[0] = Character.toUpperCase(array[0]);
        return String.valueOf(array, 0, array.length);
    }
}
