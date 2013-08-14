#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#

import TenantServer
from ttypes import *

from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

try:

  # Make socket
  transport = TSocket.TSocket('localhost', 8080)

  # Buffering is critical. Raw sockets are very slow
  transport = TTransport.TBufferedTransport(transport)
  # Wrap in a protocol
  protocol = TBinaryProtocol.TBinaryProtocol(transport)

  # Create a client to use the protocol encoder
  client = TenantServer.Client(protocol)
  # Connect!
  transport.open()
  print client
  client.createVirtualNetwork(protocol, controllerIP, 6633, networkIP, 16)
  client.createVirtualSwitch(1, ["0", "1"])
  client.createHost(1, 0, 1)
  pathString =  "srcDPID/srcPort-dstDPID/dstPort,srcDPID/srcPort-dstDPID/dstPort,srcDPID/srcPort-dstDPID/dstPort"
  client.createVirtualLink(tenantId, pathString)
  client.startNetwork()
  #client.ping()
  #print 'ping()'

  # Close!
  transport.close()

except Thrift.TException, tx:
  print '%s' % (tx.message)
