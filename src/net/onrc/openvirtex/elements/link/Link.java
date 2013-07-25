/**
 *  Copyright (c) 2013 Open Networking Laboratory
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 * 
 */

package net.onrc.openvirtex.elements.link;

/**
 * @author gerola
 * 
 */
public abstract class Link<T1> {
    private T1 srcPort;
    private T1 dstPort;

    /**
     * 
     */
    public Link() {
	super();
	this.srcPort = null;
	this.dstPort = null;
    }

    /**
     * @param srcPort
     * @param dstPort
     */
    public Link(final T1 srcPort, final T1 dstPort) {
	super();
	this.srcPort = srcPort;
	this.dstPort = dstPort;
    }

    public T1 getSrcPort() {
	return this.srcPort;
    }

    public void setSrcPort(final T1 srcPort) {
	this.srcPort = srcPort;
    }

    public T1 getDstPort() {
	return this.dstPort;
    }

    public void setDstPort(final T1 dstPort) {
	this.dstPort = dstPort;
    }

}
