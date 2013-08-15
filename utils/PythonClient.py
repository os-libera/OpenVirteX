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
  transport = TTransport.TFramedTransport(transport)
  # Wrap in a protocol
  protocol = TBinaryProtocol.TBinaryProtocol(transport)

  # Create a client to use the protocol encoder
  client = TenantServer.Client(protocol)
  # Connect!
  transport.open()
  #client.createVirtualNetwork(protocol, controllerIP, 6633, networkIP, 16)
  print client.createVirtualNetwork('tcp', '192.168.56.5', 10000, '192.168.0.0', 16)
  raw_input()
  print client.createVirtualSwitch(1, ["1"])
  print client.createVirtualSwitch(1, ["2"])
  print client.createVirtualSwitch(1, ["3"])
  print client.createVirtualSwitch(1, ["4", "5"])
  raw_input()
  #print client.createHost(1, 0, 1)
  #raw_input()
  pathString =  "1/2-2/2,2/3-3/2"
  print client.createVirtualLink(1, pathString)
  pathString =  "1/2-2/2,2/3-3/2,3/3-4/2,4/3-5/2"
  print client.createVirtualLink(1, pathString)
  raw_input()
  print client.startNetwork(1)

  '''
  sum = client.add(1,1)
  print '1+1=%d' % (sum)

  work = Work()

  work.op = Operation.DIVIDE
  work.num1 = 1
  work.num2 = 0

  try:
    quotient = client.calculate(1, work)
    print 'Whoa? You know how to divide by zero?'
  except InvalidOperation, io:
    print 'InvalidOperation: %r' % io

  work.op = Operation.SUBTRACT
  work.num1 = 15
  work.num2 = 10

  diff = client.calculate(1, work)
  print '15-10=%d' % (diff)

  log = client.getStruct(1)
  print 'Check log: %s' % (log.value)
  '''
  # Close!
  transport.close()

except Thrift.TException, tx:
  print '%s' % (tx.message)
