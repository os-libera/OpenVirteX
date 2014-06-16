// TODO: encapsulate to VirtualTopology object with minimal interface

// used to store the current mouseover element
var mouseHighlightId;

function clearVirtualNetwork() {
	document.getElementById('virtual').innerHTML = '';
	restoreMouseHighlighting(true);
	mouseHighlightId = null;
	lastVirtualNetworkModel = null;
}


function mouseHost(host, doHighlight) {
	mouseHighlightId = doHighlight ? host.attr('id') : null;

	host.classed('highlight', doHighlight);
	var id = host.attr('id');

	id = id.replace('virtual', 'physical');
	d3.select('#'+id).classed('highlight', doHighlight);
}

function mouseSwitch(sw, switchMapping, doHighlight) {
	mouseHighlightId = doHighlight ? sw.attr('id') : null;

	var id = sw.attr('id');
	sw.classed('highlight', doHighlight);

	applyToInternalLinksOfPhysicalSwitchesMappedFrom(switchMapping, id, function (sel) {
		sel.classed('highlight', doHighlight);
	});

	applyToPhysicalSwitchesMappedFrom(switchMapping, id, function (sel) {
		sel.classed('highlight', doHighlight);
	});
}

function mouseLink(link, linkMapping, doHighlight) {
	mouseHighlightId = doHighlight ? link.attr('id') : null;

	// trying to get link highlighting to work even when flow path is on top
	// if (doHighlight) {
	// 	link.moveToFront();
	// } else {
	// 	link.moveToBack()
	// }

	var id = link.attr('id');
	if (id.match('host')) {
		id = id.replace('virtual', 'physical');
		d3.select('#'+id).classed('highlight', doHighlight);
		link.classed('highlight', doHighlight);
	} else {
		var linkId = id.replace('virtual_link-', '');
		link.classed('highlight', doHighlight);

		applyToPhysicalSwitchesReferencedByPhysicalLinksMappedFrom(linkMapping, linkId, function (sel) {
			sel.classed('highlight', doHighlight);
		}, true);

		applyToPhysicalLinksMappedFrom(linkMapping, linkId, function (sel) {
			sel.classed('highlight', doHighlight);
		}, true);
	}
}

function restoreMouseHighlighting(clearHighlight) {
	if (mouseHighlightId) {
		var node = d3.select('#' + mouseHighlightId);
		if (node.classed('host')) {
			mouseHost(node, !clearHighlight);
		} else if (node.classed('switch')) {
			mouseSwitch(node, currentMappings.switchMapping, !clearHighlight);
		} else if (node.classed('link')) {
			mouseLink(node, currentMappings.linkMapping, !clearHighlight);
		}
	}
}


function mousedownHost(host) {
	var style = window.getComputedStyle(document.getElementById('virtual'));
	var webkitMatrix = new WebKitCSSMatrix(style.webkitTransform).inverse();
	var center = bbCenter(host.select('polygon').node().getBBox());

	host.classed('linking', true);
	d3.select('#' + host.attr('id').replace('virtual', 'physical')).classed('linking', true);

	d3.select('#virtual-svg g')
		.append('line')
		.attr('id', 'linkVector')
		.attr('x1', center.x)
		.attr('x2', center.x)
		.attr('y1', center.y)
		.attr('y2', center.y);
	d3.select(document.body).classed('linking', true);

	d3.select('#virtual-svg').on('mousemove', function () {
		// find the mouse position relative to the svg screen
		var pt = d3.select('#virtual-svg').node().createSVGPoint();
		var mouse = d3.mouse(document.body);
		pt.x = mouse[0];
		pt.y = mouse[1] - window.scrollY;

		pt = pt.matrixTransform(this.querySelector('g').getScreenCTM().inverse());

		var ctm = this.querySelector('g').getCTM();

		pt.x += ctm.e;
		pt.x *= webkitMatrix.a;
		pt.x -= ctm.e;

		pt.y += ctm.f;
		pt.y *= webkitMatrix.a;
		pt.y -= ctm.f;

		d3.select('#linkVector')
			.attr("x2", pt.x)
			.attr("y2", pt.y);
	});


	d3.select(window).on('mouseup', function () {
		var target = d3.select('#virtual .host.highlight');
		if (!target.empty()) {
			var src = d3.select('#virtual .host.linking');
			// how would this ever happen?
			if (!src.empty()) {
				var srcMAC = src.attr('id').replace('virtual_host', '').replace(/_/g, ':');
				var targetMAC = target.attr('id').replace('virtual_host', '').replace(/_/g, ':');
				startPing(selectedVirtualNetwork(), srcMAC, targetMAC);
			}
		}


		d3.select('#linkVector').remove();
		d3.select('#virtual-svg').on('mousemove', null);
		d3.select(window).on('mouseup', null);
		d3.select(document.body).classed('linking', false);
		d3.selectAll('.linking').classed('linking', false);
	});
}

function installVirtualTopologyEventHandlers(tenantId, switchMapping, linkMapping) {
	d3.selectAll('#virtual .host').
		on('mouseover', function () {
			mouseHost(d3.select(this), true);
		}).
		on('mouseout', function () {
			mouseHost(d3.select(this), false);
		}).
		on('mousedown', function () {
			mousedownHost(d3.select(this));
		});

	d3.selectAll('#virtual .switch').
		on('mouseover', function () {
			if (linking()) {
				return;
			}
			mouseSwitch(d3.select(this), switchMapping, true);
		}).
		on('mouseout', function () {
			mouseSwitch(d3.select(this), switchMapping, false);
		}).
		on('click', function () {
			/*START-Ami: Color the nodes to green when selected in virtual topology*/
			d3.selectAll('.colorNode').classed('colorNode',false);
			d3.select(this).classed('colorNode',true);
			/*END-Ami: Color the nodes to green when selected in virtual topology*/
			showFlowtable(this.getAttribute('id'));
		});

	d3.selectAll('#virtual .link').
		on('click', function () {
			var id = this.getAttribute('id');
			var linkId = id.replace('virtual_link-', '');
			console.log(linkMapping[linkId]);
		}).
		on('mouseover', function () {
			if (linking()) {
				return;
			}
			mouseLink(d3.select(this), linkMapping, true);
		}).
		on('mouseout', function () {
			mouseLink(d3.select(this), linkMapping, false);
		});
}

function drawVirtualNetwork(virtualNetworkModel, tenantId, cb) {
	processPendingActions(virtualNetworkModel, tenantId);

	if (logDiff(lastVirtualNetworkModel, virtualNetworkModel)) {
		// clone it before the view marks it up
		lastVirtualNetworkModel = JSON.parse(JSON.stringify(virtualNetworkModel));
	} else {
		cb();
		return;
	}
    /*START-Ami: Clearing the node highlighting when virtual topology is changed and removing firewall image*/
	d3.selectAll(".colorNode").classed("colorNode",false);
	/*END-Ami: Clearing the node highlighting when virtual topology is changed and removing firewall image*/
	console.log('virtual topology changed. updating view.');

	virtualNetworkModel.topology.hosts = virtualNetworkModel.hosts;
	virtualNetworkModel.topology.hosts.forEach(function (host) {
		var physicalHostId = '#physical_host' + host.mac.replace(/:/g, '_');
		var physicalHost = d3.select(physicalHostId);
		if (!physicalHost.empty()) {
			var bbox = physicalHost.select('polygon').node().getBBox();
			var pos = bbCenter(bbox);
			pos.y *= -1;

			// TODO: PINNING
			// TODO: magic numbers ALSO SEE app.js for other pinning cases
			pos.x /= 72; pos.y /= 72;
			host.pos = pos.x + ',' + pos.y + '!';
		}
	});

	// pin the 1:1 switches
	virtualNetworkModel.topology.layout = {};
	virtualNetworkModel.topology.switches.forEach(function (dpid) {
		var physicalSwitches = virtualNetworkModel.switchMapping[dpid].switches;
		// find centroid of physical switch group
		var posTotals = {x:0, y:0};
		physicalSwitches.forEach(function (physicalSwitchDPID) {
			var physicalSwitchId = '#physical_switch-' + physicalSwitchDPID.replace(/:/g, '_');
			var physicalSwitch = d3.select(physicalSwitchId);
			if (!physicalSwitch.empty()) {
				var bbox = physicalSwitch.select('ellipse').node().getBBox();
				var pos = bbCenter(bbox);
				posTotals.x += pos.x;
				posTotals.y += pos.y;
			}
		});

		var centroid = {x: posTotals.x/physicalSwitches.length, y: posTotals.y/physicalSwitches.length};
		centroid.y *= -1;
		centroid.x /= 72; centroid.y /= 72;

		if (physicalSwitches.length == 1) {
			virtualNetworkModel.topology.layout[dpid] = centroid.x + ',' + centroid.y + '!';
		} else {
			// pinning the virtual switch to the centroid makes it match the clusters in the physical display
			// using the centroid as initial position allows it to float which creates slightly more abstract graphs
			virtualNetworkModel.topology.layout[dpid] = centroid.x + ',' + centroid.y ; //+ '!';
		}
	});


	// console.log(virtualNetworkModel)
	API.layoutTopology(virtualNetworkModel.topology, 'virtual', function (svg) {
		document.getElementById('virtual').innerHTML = '';

		drawSVGTopology('virtual', svg);
		drawFlowpaths(svg, 'virtual', virtualNetworkModel.flowPaths);
		showHostMACandIP('virtual', virtualNetworkModel.hosts);

		// scale to match the physical graph
		var physicalTopology = d3.select('#physical');

		// TODO: ugly. clean this up
		var physicalScale = parseFloat(physicalTopology.node().style['-webkit-transform'].match(/scale\((.*)\)/)[1]);
		// console.log(physicalScale)

		var physicalTopologyComputedStyle = window.getComputedStyle(physicalTopology.node());
		var physicalTopologySize = {width: parseFloat(physicalTopologyComputedStyle.width.replace('px','')) * physicalScale,
							 height: parseFloat(physicalTopologyComputedStyle.height.replace('px','')) * physicalScale};

		svg = d3.select(svg);
		var svgSize = {width: parseFloat(svg.attr('width').replace('pt', '')),
					   height: parseFloat(svg.attr('height').replace('pt', ''))}

		var scale = {x:physicalTopologySize.width/svgSize.width, y:physicalTopologySize.height/svgSize.height};

		var virtualContainer = d3.select('#virtualSVGContainer');
		var virtualContainerComputedStyle = window.getComputedStyle(virtualContainer.node());
		var virtualContainerSize = {width: parseFloat(virtualContainerComputedStyle.width.replace('px','')),
							 		height: parseFloat(virtualContainerComputedStyle.height.replace('px',''))};
		// console.log(virtualContainerSize);

		scale = Math.min(scale.x, scale.y);

		var xOff = (virtualContainerSize.width - svgSize.width*scale)/2;
		var yOff = (virtualContainerSize.height - svgSize.height*scale)/2;
		var transform = 'translate(' + xOff + 'px, ' + yOff + 'px)scale(' + scale + ',' + scale + ')';

		// var transform = 'scale(' + scale.x + ',' + scale.y + ')';

		// var transform = 'scale(' + scale + ')';
		// console.log(transform);
		d3.select('#virtual').style('-webkit-transform', transform);
		d3.select('#virtual').style('-webkit-transform-origin', '0% 0%');


		currentMappings.switchMapping = virtualNetworkModel.switchMapping;
		currentMappings.linkMapping = virtualNetworkModel.linkMapping;

		restoreMouseHighlighting();
		highlightUsedElements();
	    moveNodesToFront(svg);
	    moveUsedHostsToFront(svg);
	    createTooltips(svg.select('g'));
	    createTooltips(d3.select('#physical').select('g'));
		
		/*START-Ami: Positiong RHQ and HQ text, Add 26.7 to x and 6.3 to y of previous text node*/
	    if(tenantId==2){
	    for (virtualHostId in virtualHostConfiguration) {
	    	var virtualTopoWidth = d3.select('#physical-svg').node().getBBox().width / 2;
	    	var virtualTopoHeight = d3.select('#physical-svg').node().getBBox().height / 2;
	    	//console.log(middle);
		   // var anchor = label.attr('x') < middle ? 'start' : 'end';
			var virtualLayout = virtualHostConfiguration[virtualHostId];
			var polygon = d3.select("#"+virtualHostId + ' polygon');
			var polygonBounds = polygon.node().getBBox();
			if("#"+virtualHostId == "#virtual_host00_00_00_02_00_04" || "#"+virtualHostId == "#virtual_host00_00_00_09_00_01"){
				d3.select("#"+virtualHostId).append("svg:text")
				.attr("x",polygonBounds.x+80)
				.attr("y",polygonBounds.y+60)
				.style("fill",virtualLayout.fontColor)
				.style("font-size","30px")
				.attr('text-anchor', "end")
				.text(virtualLayout.text);
			}
			else if("#"+virtualHostId == "#virtual_host00_00_00_01_00_07"){
				d3.select("#"+virtualHostId).append("svg:text")
				.attr("x",polygonBounds.x+70)
				.attr("y",polygonBounds.y+70)
				.style("fill",virtualLayout.fontColor)
				.style("font-size","30px")
				.attr('text-anchor', "end")
				.text(virtualLayout.text);
			}
			else{
			d3.select("#"+virtualHostId).append("svg:text")
							.attr("x",function(){
								if(polygonBounds.x<virtualTopoWidth){return polygonBounds.x-10;}
								else{return polygonBounds.x+40;}})
							.attr("y",function(){
								if(polygonBounds.x>virtualTopoWidth){return polygonBounds.y+70;}
								else{return polygonBounds.y+30;}})
							.style("fill",virtualLayout.fontColor)
							.style("font-size","30px")
							.attr('text-anchor', "end")
							.text(virtualLayout.text);
			}
		}
	    }
		/*END-Ami: Positiong RHQ and HQ text*/
		
		/*START-Ami: Increasing the size of big switch and placing firewall image*/
		if(tenantId==1){
		d3.select("#virtual_switch-00_a4_23_05_00_00_00_01 ellipse").attr("rx",50).attr("ry",50);
		var polygon = d3.select("#virtual_host00_00_00_04_00_08 polygon");
		var polygonBounds = polygon.node().getBBox();
		d3.select("#firewallImage").style("display","block");
		//d3.select("#firewallImage").style("margin-right",10vw);
		//d3.select("#firewallImage").style("margin-bottom",10vh);
		}
		else{
			d3.select("#firewallImage").style("display","none");
		}
		/*END-Ami: Increasing the size of big switch and placing firewall image*/
		installVirtualTopologyEventHandlers(tenantId, virtualNetworkModel.switchMapping, virtualNetworkModel.linkMapping);
		// apply pending class
		if (virtualNetworkModel.pendingIds) {
			virtualNetworkModel.pendingIds.forEach(function (id) {
				d3.select('#' + id).classed('pending', true);
			});
		}

		cb();
	});
	
}

function syncVirtualNetwork(tenantId, cb) {
	async.parallel({
		topology: function (cb) {
			API.getVirtualTopology(tenantId, cb);
		},
		switchMapping: function (cb) {
			API.getVirtualSwitchMapping(tenantId, cb);
		},
		linkMapping: function (cb) {
			API.getVirtualLinkMapping(tenantId, cb);
		},
		hosts: function (cb) {
			API.listVirtualHosts(tenantId, cb);
		},
		flowPaths: function (cb) {
			API.getVirtualFlowpaths(tenantId, cb);
		}
	}, function (error, virtualNetworkModel) {
		if (error) {
			//alert(error);
		} else {
			drawVirtualNetwork(virtualNetworkModel, tenantId, cb);
		}
	});
}
