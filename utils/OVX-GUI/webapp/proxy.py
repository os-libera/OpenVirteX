from flask import Flask, make_response, request
import pygraphviz as pgv
import urllib2, base64, json, sys, copy
import flowpaths

app = Flask(__name__)

#http://networkx.lanl.gov/pygraphviz/pygraphviz.pdf

#url = 'http://ovx.onlab.us:8080/status'
#url = 'http://192.168.56.102:8080/status'
#link_url = 'http://192.168.56.102:5000/%s/%s/%s'
#stopPing_url = 'http://192.168.56.102:5000/stopPing/%s'
#startPing_url = 'http://192.168.56.102:5000/hostPing/%s/%s'
#link_url = 'http://ovx.onlab.us:5000/%s/%s/%s'
#stopPing_url = 'http://ovx.onlab.us:5000/stopPing/%s'
#startPing_url = 'http://ovx.onlab.us:5000/hostPing/%s/%s'
user = 'ui'
password = 'whatever'



mock = {}
# lines = open('mock.txt').read().splitlines()
# keys = []
# values = []
# for line in lines:
#     if len(values) < len(keys):
#         values.append(line)
#     else:
#         keys.append(line)
# while len(keys):
#     mock[keys.pop()] = values.pop()


def get(method, params = {}):
    global mock

    payload = {
        'jsonrpc': '2.0',
        'id': 1,
        'method': method,
        'params': params
    };

    # print payload

    try:
        if str(payload) in mock:
            replyBody = mock[str(payload)]
            # print replyBody
            reply = make_response(replyBody)
            reply.headers.add('Access-Control-Allow-Origin', '*')
            return reply
    except e:
        print e

    request = urllib2.Request(url)
    b64string = base64.encodestring('%s:%s' % (user, password)).replace('\n', '')
    request.add_header("Authorization", "Basic %s" % b64string)
    request.data =  json.dumps(payload, sort_keys=True,separators=(',', ':'))

    try:
        response = urllib2.urlopen(request)
    except urllib2.HTTPError, e:
        print e
        print 'hello'
        reply = make_response('')
        reply.status_code = e.code
    else:
        try:
            responseBody = response.read()
            jsonBody = json.loads(responseBody)
            replyBody = json.dumps(jsonBody['result'], sort_keys=True,separators=(',', ':'))
            # print replyBody
            reply = make_response(replyBody)
        except:
            print "Unexpected error calling OVX api:", sys.exc_info()[0]
            reply = make_response('')
            reply.status_code = 500

    reply.headers.add('Access-Control-Allow-Origin', '*')
    return reply

@app.route('/getPhysicalTopology')
def getPhysicalTopology():
    return get('getPhysicalTopology')

@app.route('/getVirtualTopology')
def getVirtualTopology():
    return get('getVirtualTopology', {'tenantId': int(request.args.get('tenantId'))})

@app.route('/listVirtualNetworks')
def listVirtualNetworks():
    return get('listVirtualNetworks')

@app.route('/getVirtualSwitchMapping')
def getVirtualSwitchMapping():
    return get('getVirtualSwitchMapping', {'tenantId': int(request.args.get('tenantId'))})

@app.route('/getVirtualLinkMapping')
def getVirtualLinkMapping():
    return get('getVirtualLinkMapping', {'tenantId': int(request.args.get('tenantId'))})

@app.route('/listVirtualHosts')
def listVirtualHosts():
    return get('getVirtualHosts', {'tenantId': int(request.args.get('tenantId'))})

@app.route('/getVirtualFlowtable')
def getVirtualFlowtable():
    return get('getVirtualFlowtable', {'tenantId': int(request.args.get('tenantId')), 'vdpid': int(request.args.get('vdpid'), 16)})

@app.route('/getPhysicalFlowtable')
def getPhysicalFlowtable():
    return get('getPhysicalFlowtable', {'dpid': int(request.args.get('dpid'), 16)})


@app.route('/listPhysicalHosts')
def listPhysicalHosts():
    return get('getPhysicalHosts')

@app.route('/layoutTopology', methods=['POST', 'OPTIONS'])
def layoutTopology():
    if (request.method == 'OPTIONS'):
        reply = make_response('')
        reply.headers.add('Access-Control-Allow-Origin', '*')
        reply.headers.add('Access-Control-Allow-Headers', 'content-type')
        return reply

    else:
        topo = request.json
        prefix = request.args.get('prefix')
        fixedSwitchWidth = float(request.args.get('fixedSwitchWidth'))
        repositionableSwitchWidth = float(request.args.get('repositionableSwitchWidth'))
        hostWidth = float(request.args.get('hostWidth'))


# NB
# the physical graphs are directed (separate linkId for each direction)
# virtual graphs are undirected but have two link entries for each linkId, one for each direction
# setting graphviz to directed=False,strict=True causes it to drop half of the edges
# this ends up working fine because the double edges in the virtual graph are redundant
# and since the virtual link map contains the physical links in both directions, the full path
# will end up being shown regardless of which direction gets dropped due to strict=True
        G = pgv.AGraph(directed=False,strict=True,splines=True)
        G.node_attr['shape']='circle'
        G.node_attr['label']=' '
        G.node_attr['fixedsize']='true'

        try:
            for switch in topo['switches']:
                id = switch.replace(':', '_')
                if 'layout' in topo and switch in topo['layout']:
                    pos = topo['layout'][switch]
                    if pos.find('!') != -1:
                        G.add_node(switch, color='white', width=fixedSwitchWidth, id=prefix+'_switch-'+id, pos=pos)
                    else:
                        G.add_node(switch, color='white', width=repositionableSwitchWidth, id=prefix+'_switch-'+id, pos=pos)
                else:
                    G.add_node(switch, color='white', width=repositionableSwitchWidth, id=prefix+'_switch-'+id)

            for link in topo['links']:
                id = str(link['linkId'])
                G.add_edge(link['src']['dpid'], link['dst']['dpid'], id=prefix+'_link-'+id)

            for host in topo['hosts']:
                id = host['mac'].replace(':', '_')
                if 'pos' in host:
                    G.add_node(host['mac'], shape='square', width=hostWidth, color='white', id=prefix+'_host'+id, pos=host['pos'])
                else:
                    G.add_node(host['mac'], shape='square', width=hostWidth, color='white', id=prefix+'_host'+id)
                G.add_edge(host['mac'], host['dpid'], id=prefix+'_host_link'+id)

            # print G

            if prefix == 'physical':
                # G.layout(prog='neato', args='-Goverlap=True -y -Gmaxiter=1')
                G.layout(prog='neato', args='-Goverlap=True -y -Gepsilon=.0000001')
            else:
                G.layout(prog='neato', args='-Goverlap=True')

            svg = G.draw(format='svg')
            # print svg
            reply = make_response(svg)
        except:
            print "Unexpected error building graph:", sys.exc_info()[0]
            reply = make_response('')
            reply.status_code = 500

        reply.headers.add('Access-Control-Allow-Origin', '*')
        return reply

@app.route('/getVirtualFlowpaths')
def fetchVirtualFlowpaths():
    print 'fetchVirtualFlowpaths'
    replyBody = json.dumps(flowpaths.getVirtualFlows(int(request.args.get('tenantId'))), sort_keys=True,separators=(',', ':'))
    reply = make_response(replyBody)
    reply.headers.add('Access-Control-Allow-Origin', '*')
    return reply

@app.route('/getPhysicalFlowpaths')
def fetchPhysicalFlowpaths():
    print 'fetchPhysicalFlowpaths'
    replyBody = json.dumps(flowpaths.getPhysicalFlows(), sort_keys=True,separators=(',', ':'))
    reply = make_response(replyBody)
    reply.headers.add('Access-Control-Allow-Origin', '*')
    return reply

@app.route('/linkdown')
def linkdown():
    src = request.args.get('src')
    dst = request.args.get('dst')
    print link_url % ("linkdown", src, dst)
    req = urllib2.Request(link_url % ("linkdown", src, dst))
    try:
        response = urllib2.urlopen(req)
    except urllib2.URLError, e:
        print e
        reply = make_response('')
        reply.status_code = 500

    reply = make_response('{"status": "ok"}')
    reply.headers.add('Access-Control-Allow-Origin', '*')
    return reply

@app.route('/linkup')
def linkup():
    src = request.args.get('src')
    dst = request.args.get('dst')
    print link_url % ("linkup", src, dst)
    req = urllib2.Request(link_url % ("linkup", src, dst))
    try:
        response = urllib2.urlopen(req)
    except urllib2.URLError, e:
        print e
        reply = make_response('')
        reply.status_code = e.code

    reply = make_response('{"status": "ok"}')
    reply.headers.add('Access-Control-Allow-Origin', '*')
    return reply

@app.route('/stopPing')
def stopPing():
    req = urllib2.Request(stopPing_url % (request.args.get('tenantId')))
    try:
        print 'stopPing'
        response = urllib2.urlopen(req)
    except urllib2.URLError, e:
        print e
        reply = make_response('')
        reply.status_code = 500

    reply = make_response('{"status": "ok"}')
    reply.headers.add('Access-Control-Allow-Origin', '*')
    return reply

@app.route('/startPing')
def startPing():
    src = request.args.get('src')
    dst = request.args.get('dst')
    req = urllib2.Request(startPing_url % (src, dst))
    try:
        response = urllib2.urlopen(req)
    except urllib2.URLError, e:
        print e
        reply = make_response('')
        reply.status_code = 500

    reply = make_response('{"status": "ok"}')
    reply.headers.add('Access-Control-Allow-Origin', '*')
    return reply


if __name__ == '__main__':
    app.run(debug=True,threaded=True)
