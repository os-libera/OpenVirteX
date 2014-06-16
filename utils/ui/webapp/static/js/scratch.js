// http://bl.ocks.org/mbostock/4062045


function drawTopology(which, topology) {

    // topology = fakeTopo();


    // create the graph model
    var graph = {
        nodes: [],
        links: []
    };

    var map = {};

    topology.switches.forEach(function(s) {
        if (typeof s === 'object') {
            map[s.name] = graph.nodes.length;
            graph.nodes.push({
                name: s.name,
                group: s.group
            })
        } else {
            map[s] = graph.nodes.length;
            graph.nodes.push({
                name: s
            });
        }
    });

    topology.links.forEach(function(l) {
        graph.links.push({
            source: map[l.src.dpid],
            target: map[l.dst.dpid]
        });
    });

    var width = 1000,
        height = 1000;

    // var color = d3.scale.category20();
    function color(group) {
        return [
            'white',
            'white',
            'white',
            '#0000FF',
            '#FF0000'
        ][group];
    }

    var force = d3.layout.force()
        .charge(-800)
        .linkDistance(5)
        .size([width, height]);

    var svg = d3.select("#" + which).append("svg")
        .attr('id', which + '-svg')
        .attr('preserveAspectRatio', true)
        .attr('viewBox', '0 0 1000 1000')
        .attr("width", width)
        .attr("height", height);



    force
        .nodes(graph.nodes)
        .links(graph.links)
        .start();

    var link = svg.selectAll(".link")
        .data(graph.links)
        .enter().append("line")
        .attr("class", "link")
        .style("stroke-width", function(d) {
            return Math.sqrt(d.value);
        });

    var node = svg.selectAll(".node")
        .data(graph.nodes)
        .enter().append("circle")
        .attr("class", "node")
        .attr("r", 5)
        .style("fill", function(d) {
            return color(d.group);
        })
        .call(force.drag);

    node.append("title")
        .text(function(d) {
            return d.name;
        });

    var safety = 0;
    while (force.alpha() > 0.01) { // You'll want to try out different, "small" values for this
        force.tick();
        if (safety++ > 1000) {
            break; // Avoids infinite looping in case this solution was a bad idea
        }
    }

    force.on("tick", function() {
        link.attr("x1", function(d) {
            return d.source.x;
        })
            .attr("y1", function(d) {
                return d.source.y;
            })
            .attr("x2", function(d) {
                return d.target.x;
            })
            .attr("y2", function(d) {
                return d.target.y;
            });

        node.attr("cx", function(d) {
            return d.x;
        })
            .attr("cy", function(d) {
                return d.y;
            });
    });
}


// to get rid of initial animation
// http://stackoverflow.com/questions/13463053/calm-down-initial-tick-of-a-force-layout

function name() {
    var name = '';
    var i;
    for (i = 0; i < arguments.length; i++) {
        var letter = String.fromCharCode('a'.charCodeAt(0) + arguments[i]);
        name += letter;
    }
    return name;
}

function dividedFatTree(numCore, aggCoreWidth, aggPerCore, leafAggWidth, leafPerAgg) {

    var i, j, k, l;
    var graph = [];

    // creates a hidden root node that helps twopi
    var fakeEdges = [];
    graph.push(fakeEdges);
    // for (i = 0; i < numCore; ++i) {
    //   fakeEdges.push({b:name(i),a:'root'});
    // }

    // core switches are fully interconnected
    var coreEdges = [];
    graph.push(coreEdges);
    for (i = 0; i < numCore; ++i) {
        for (j = i + 1; j < numCore; ++j) {
            coreEdges.push({
                a: name(i),
                b: name(j)
            });
        }
    }

    var aggEdges = [];
    graph.push(aggEdges);
    for (i = 0; i < numCore; ++i) {
        for (j = 0; j < aggPerCore; ++j) {
            for (k = 0; k < aggCoreWidth; k++) {
                var core = (i + k) % numCore;
                aggEdges.push({
                    a: name(i, j),
                    b: name(core)
                });
            }
        }
    }

    var leafEdges = [];
    graph.push(leafEdges);
    for (i = 0; i < numCore; ++i) {
        for (j = 0; j < aggPerCore; ++j) {
            for (k = 0; k < leafPerAgg; ++k) {
                for (l = 0; l < leafAggWidth; l++) {
                    var agg = (j + l) % aggPerCore;
                    leafEdges.push({
                        a: name(i, j, k),
                        b: name(i, agg)
                    });
                }
            }
        }
    }

    return graph;
}


function fakeTopo() {

    var topo = {
        switches: [],
        links: []
    };

    var graph = dividedFatTree(2, 2, 4, 4, 4);

    var nodes = {};
    var i;
    for (i = 0; i < graph.length; ++i) {
        group = graph[i];
        group.forEach(function(e) {
            if (!nodes[e.a]) {
                nodes[e.a] = (i + 1);
            }
            if (!nodes[e.b]) {
                nodes[e.b] = (i + 1);
            }
            topo.links.push({
                src: {
                    dpid: e.a
                },
                dst: {
                    dpid: e.b
                }
            })
        })
    }

    var name;
    for (name in nodes) {
        topo.switches.push({
            name: name,
            group: nodes[name]
        });
    }

    return topo;
}