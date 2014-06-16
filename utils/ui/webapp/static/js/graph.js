// RENAME to topology.js? OR does this become physical.js

/*

decomposeTopology(topology)

the global "layout" object contains <dpid,layout> dictionary where layout is:
  pos
  name
  subtreetype
  subtreeorientation

switches in the layout dictionary are assumed to be "core" switches

a core switch is one which connects to other core switches as well as a subgraph containing non-core switches
a non-core switch is one which connects only to other non-core switches

this function traverses the graph and returns a structure:

{
  coreTopology: {<topology>},
  subGraphs: [topology]
}

the layout algorithm for the physical network then:
  lays out each topology separately using an appropriate layout algorithm for the topology
  the layout algorithm determines placement of the switches and (trivial for the core switches) and
  also generates an <svg> element
    TBD: how to get all of the svg elements to have compatible view boxes

  finally, the svg elements are assembled with subGraphs positioned using either the orientation value from layout or some other method
    (e.g. bisecting the largest link angle for the core switch)
    a sub tree is oriented by placing the root directly over the corresponding root switch in the core switch diagram and then rotating
    such that the line passing through the root and centroid coincides with the orientation vector



  topology: {
    "links": [

    ],
    "switches": [

    ],
    "hosts": [

    ]
  }
*/

function Topology() {
    this.switches = [];
    this.links = [];
    this.hosts = [];
}

// NOTE: this will throw out any disconnected non-core switches
// NOTE: uses the global coreSwitchConfiguration
function decomposeTopology(topology) {
    var coreTopology = new Topology();
    var subGraphs = {};

    topology.switches.forEach(function (dpid) {
        if (coreSwitchConfiguration[dpid]) {
            // add this switch to the core topology
            coreTopology.switches.push(dpid);
            // start a subtree for this switch
            subGraphs[dpid] = new Topology();
        }
    });

    var linkMap = {};

    topology.links.forEach(function (link) {
        if (subGraphs[link.src.dpid] && subGraphs[link.dst.dpid]) {
            // a link between core switches (i.e. between subGraphs)
            coreTopology.links.push(link);
        } else {
            ['src','dst'].forEach(function (which) {
                if (linkMap[link[which].dpid]) {
                    linkMap[link[which].dpid].push(link);
                } else {
                    linkMap[link[which].dpid] = [link];
                }
            })
        }
    });

    var visitedSwitches = {};
    function gatherSwitchesAndLinksRecursive(subGraph, dpid) {
        if (visitedSwitches[dpid]) {
            return;
        }
        subGraph.switches.push(dpid);
        visitedSwitches[dpid] = true;

        if (linkMap[dpid]) {
            linkMap[dpid].forEach(function (link) {
                subGraph.links.push(link);
                gatherSwitchesAndLinksRecursive(subGraph, link.src.dpid);
                gatherSwitchesAndLinksRecursive(subGraph, link.dst.dpid);
            });
        }
    }

    var dpid;
    for (dpid in subGraphs) {
        gatherSwitchesAndLinksRecursive(subGraphs[dpid], dpid);
    }

    // console.log(JSON.stringify(subGraphs));

    return {
        coreTopology: coreTopology,
        subGraphs: subGraphs
    };

    return {coreTopology: topology};
}



