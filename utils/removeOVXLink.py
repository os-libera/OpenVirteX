#!/usr/bin/env python

from argparse import ArgumentParser
import ovxctl
import sys
import json

if __name__ == '__main__':
    
    parser = ArgumentParser()
    parser.add_argument('tenant_id', help='Virtual network tenant ID')
    parser.add_argument('src_dpid', help='Virtual source dpid')
    parser.add_argument('dst_dpid', help='Virtual dest dpid')
    parser.add_argument('--host', default='localhost', help='host where OpenVirteX is running (default="localhost")')
    parser.add_argument('--port', default=8080, type=int, help='port where OpenVirteX is running (default=8080)')
    parser.add_argument('--ovx_user', default='admin', help='OpenVirteX user (default="admin")')
    parser.add_argument("-n", action="store_true",  dest="no_passwd", default=False,
                    help="Run with no password; default true")
    args = parser.parse_args()

    # Make sure everything is lower case
    tenant_id = int(args.tenant_id)
    src_dpid = args.src_dpid.lower()
    dst_dpid = args.dst_dpid.lower()

    # Get virtual topology
    req = { "tenantId": tenant_id }
    result = ovxctl.connect(args, "status", "getVirtualTopology", data=req, passwd=ovxctl.getPasswd(args))

    # Parse virtual topology to get the link IDs
    for link in result['links']:
        link_src_dpid = link['src']['dpid'].lower()
        link_dst_dpid = link['dst']['dpid'].lower()
        if ((link_src_dpid == src_dpid) and (link_dst_dpid == dst_dpid)) or\
          ((link_src_dpid == dst_dpid) and (link_dst_dpid == src_dpid)):
            link_id = int(link['linkId'])
            req = { "tenantId" : tenant_id, "linkId" : link_id }
            result = ovxctl.connect(args, "tenant", "disconnectLink", data=req, passwd=ovxctl.getPasswd(args))
            print "Link (link_id %s) has been disconnected from the virtual network (tenant_id %s)" % (link_id, tenant_id)
            # Assuming both directions have identical link IDs
            break
