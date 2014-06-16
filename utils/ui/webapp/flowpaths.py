import proxy
import json, urllib2, sys, copy

def buildRequest(data, url, cmd):
    j = { "id" : "ovxctl",  "method" : cmd , "jsonrpc" : "2.0" }
    h = {"Content-Type" : "application/json-rpc"}
    if data is not None:
        j['params'] = data
    return urllib2.Request(url, json.dumps(j), h)

def parseResponse(data):
    j = json.loads(data)
    if 'error' in j:
        print j
        sys.exit(1)
    return j['result']

def connect(cmd, data=None, passwd=None):
    try:
        url = proxy.url
        passman = urllib2.HTTPPasswordMgrWithDefaultRealm()
        passman.add_password(None, url, proxy.user, proxy.password)
        authhandler = urllib2.HTTPBasicAuthHandler(passman)
        opener = urllib2.build_opener(authhandler)
        req = buildRequest(data, url, cmd)
        ph = opener.open(req)
        return parseResponse(ph.read())
    except urllib2.URLError as e:
        print e
        sys.exit(1)
    except urllib2.HTTPError as e:
        if e.code == 401:
            print "Authentication failed: invalid password"
            sys.exit(1)
        elif e.code == 504:
            print "HTTP Error 504: Gateway timeout"
            sys.exit(1)
        else:
            print e
    except RuntimeError as e:
        print e



def fetchVirtualFlowpaths(tid):
    virtualTopo = connect('getVirtualTopology', data={'tenantId': int(tid)})
    switches = virtualTopo['switches']
    slinks = convertTopology(virtualTopo['links'])
    if slinks is not None:
        return computeFlowPaths(switches, slinks, tid)
    return {}

def fetchPhysicalFlowpaths():
    phyTopo = connect('getPhysicalTopology')
    switches = phyTopo['switches']
    slinks = convertTopology(phyTopo['links'])
    if slinks is not None:
        return computeFlowPaths(switches, slinks)
    return {}

def computeFlowPaths(switches, slinks, tid = 0):
    paths = {}
    if tid == 0:
        flowtables = connect('getPhysicalFlowtable', data={})
    else:
        flowtables = connect('getVirtualFlowtable', {'tenantId': tid})
    for sw in switches:
        if len(flowtables[sw]) == 0:
            continue
        for entry in flowtables[sw]:
            path = []
            # this is an edge flow
            if '%s-%s' % (sw, entry['match']['in_port']) not in slinks:
                try:
                    path = [sw] + findPath(sw, copy.deepcopy(entry), slinks, tid, flowtables)
                    paths['%s-%s' % (entry['match']['dl_src'], entry['match']['dl_dst'])] = path
                except:
                    print "Could not find connected flowpath for %s on %s" % (entry, sw)
                    continue
    return paths

def findPath(sw, inflow, slinks, tid, flowtables):
    path = []
    (neoflow, outport) = processFlow(inflow)
    while ('%s-%s' % (sw, outport)) in slinks:
        nextHop = slinks['%s-%s' % (sw, outport)]
        path.append(nextHop['dpid'])
        neoflow['match']['in_port'] = int(nextHop['port'])
        inflow = findFlow(flowtables[nextHop['dpid']], neoflow)
        (neoflow, outport) = processFlow(inflow)
        sw = nextHop['dpid']
    return path

def findFlow(table, flow):
    f_match = lowerDict(flow['match'])
    for entry in table:
        t_match = lowerDict(entry['match'])
        if hash(repr(sorted(t_match.items()))) == hash(repr(sorted(f_match.items()))):
            return entry

def processFlow(flow):
    if flow is None:
        raise Exception("Next Hop flow not found")
    actions = flow['actionsList']
    for act in actions:
        if act['type'] != 'OUTPUT':
            if act['type'].lower() in flow['match']:
                flow['match'][act['type'].lower()] = act[act['type'].lower()]
        else:
            port = act['port']
    return (flow, port)

def convertTopology(links):
    if links is not []:
        return { "-".join([link['src']['dpid'], link['src']['port'] ]) : link['dst'] for link in links }
    return None

def lowerDict(d):
    ret = {}
    for k,v in d.iteritems():
        if type(v) is str or type(v) is unicode:
            ret[k] = v.lower()
        else:
            ret[k] = v
    return ret

virtualFlowpaths = {}
def getVirtualFlows(tid):
    global virtualFlowpaths

    # comment out to cache
    virtualFlowpaths = {}

    if not tid in virtualFlowpaths:
        virtualFlowpaths[tid] = fetchVirtualFlowpaths(tid)
    return virtualFlowpaths[tid]

physicalFlowpaths = None;
def getPhysicalFlows():
    global physicalFlowpaths

    # comment out to cache
    physicalFlowpaths = None

    if physicalFlowpaths is None:
        physicalFlowpaths = fetchPhysicalFlowpaths()

    return physicalFlowpaths

