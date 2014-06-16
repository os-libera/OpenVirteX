function findLink(links, dpid) {
	links.forEach(function (link) {
		if (link.src.dpid == dpid || link.dst.dpid == dpid)
			console.log(link)
	})
}


function syncPhysicalNetwork(cb) {

	function installPhysicalTopologyEventHandlers(topology) {
		d3.selectAll('#physical .switch').
			on('click', function () {
				/*START-Ami: Fixing the node highlighting*/
				var id="#"+this.getAttribute('id');
				d3.selectAll(".colorNode").classed("colorNode",false);
				d3.select(id).classed("colorNode",true);
				/*END-Ami: Fixing the node highlighting*/
				showFlowtable(this.getAttribute('id'));
			});

		d3.selectAll('#physical .real.link').
			on('dblclick', function () {
				var id = this.getAttribute('id');
				var linkId = id.replace('physical_link-', '');
				// ignore host uplinks
				if (!linkId.match('host')) {
					var linkMap = {};
					// would be better if the map were prebuilt
					topology.links.forEach(function (link) {
						linkMap[link.linkId] = link;
					});
					var link = linkMap[linkId];
					if (link) {
						// TODO: pending action
						if (d3.select(this).classed('inactive')) {
							linkUp(link.src.dpid, link.dst.dpid, id);
						} else {
							linkDown(link.src.dpid, link.dst.dpid, id);
						}
					}
				}
			});
	}

	function apiCall(method) {
		return function(cb) {
			API[method](function (error, results) {
				if (error) {
					cb(error);
				} else {
					cb(null, results);
				}
			});
		}
	}

	function updateModel(cb) {
		async.parallel({
			physicalTopology: apiCall('getPhysicalTopology'),
			virtualNetworks: apiCall('listVirtualNetworks'),
			hosts: apiCall('listPhysicalHosts'),
			flowPaths: apiCall('getPhysicalFlowpaths')
		}, function (error, model) {
			if (!error) {
				model.physicalTopology.hosts = model.hosts;
				cb(model);
			}
		});
	}

	function markActiveLinks(activeLinks, cachedLinks) {
		var linkMap = {};
		activeLinks.forEach(function (link) {
			linkMap[link.src.dpid + '-' + link.dst.dpid] = link;
		});
		cachedLinks.forEach(function (link) {
			var activeLink = linkMap[link.src.dpid + '-' + link.dst.dpid];
			if (activeLink) {
				link.linkId = activeLink.linkId;
				link.active = true;
			}
		});
		/*/**Ami:START-Code to color core and edge switches differently*/
	  	for (dpid in coreSwitchConfiguration) {
			var layout = coreSwitchConfiguration[dpid];
			if (layout.name) {
				var physicalSwitchID = 'g#physical_switch-' + dpid.replace(/:/g, '_');
				//var virtualSwitchID = 'g#virtual_switch-' + dpid.replace(/:/g, '_');
				//d3.select(physicalSwitchID).classed('core-switch',true);
				//d3.select(physicalSwitchID+'.used').classed('core-switch-used',true);
		}
		}
		/*Ami:END-Code to color core and edge switches differently**/
	}

	function showInactiveLinks(svg, links) {
		links.forEach(function (link) {
			if (!link.active) {
				var id = '#physical_link-' + link.linkId;
				d3.select(id).classed('inactive', true);
			}
		});
	}


	updateModel(function (physicalNetworkModel) {

		// get the map
		var mapSvg = d3.select('#map');
		var physicalTopologyMap = d3.select(document.getElementById('physicalTopologyMap'));
		// only happens the first time
		if (physicalTopologyMap.empty()) {
			mapSvg.attr('preserveAspectRatio', true); // move to html?
			mapSvg.attr('viewBox', '0 0 ' + mapSvg.node().clientWidth + ' ' + mapSvg.node().clientHeight);
			physicalTopologyMap = createMap(mapSvg, 'physicalTopologyMap');
		}

		if (physicalNetworkModel) {
			processPendingActions(physicalNetworkModel.physicalTopology);

			// cache the physical links if not already cached
			if (!localStorage.physicalLinks) {
				localStorage.physicalLinks = JSON.stringify(physicalNetworkModel.physicalTopology.links);
			}
			var newLinks = physicalNetworkModel.physicalTopology.links;
			physicalNetworkModel.physicalTopology.links = JSON.parse(localStorage.physicalLinks);
			markActiveLinks(newLinks, physicalNetworkModel.physicalTopology.links);

			// var newPhysicalNetworkResult = JSON.stringify(physicalNetworkModel);
			// if (newPhysicalNetworkResult === lastPhysicalNetworkModel) {
			// 	cb(physicalNetworkModel);
			// 	return;
			// } else {
			// 	lastPhysicalNetworkModel = newPhysicalNetworkResult;
			// }

			if (logDiff(lastPhysicalNetworkModel, physicalNetworkModel)) {
				// clone it before the view marks it up
				lastPhysicalNetworkModel = JSON.parse(JSON.stringify(physicalNetworkModel));
			} else {
				cb(lastPhysicalNetworkModel);
				return;
			}


			console.log('physical topology changed. updating view.');

			// would like to get rid of the global
			model = physicalNetworkModel;


			// CREATE THE LAYOUT STRUCTURE USED BY API.layoutTopology
			function getSwitchPositions(topology, finalLayout, initialLayout, layoutType) {
				var switchPositions = {};

				function getProjection(pos) {
					var pos = layoutType === 'latlng' ? projection(pos) : pos;
					// console.log(dpid)
					// console.log(pos)
					pos[0] -= mapSvg.node().clientHeight/2;
					pos[1] -= mapSvg.node().clientWidth/2;
					// pos is in inches. 72 is default.
					pos[0] /= 72;
					pos[1] /= 72;
					return pos;
				}

				function formatPos(pos, initial) {
					var pos = getProjection(pos);
					var formatted = pos[0] + ',' + pos[1];
					if (!initial) {
						formatted += '!';
					}
					return formatted;
				}

				topology.switches.forEach(function (dpid) {
					var finalPos = finalLayout[dpid] && finalLayout[dpid].pos;
					var initialPos = initialLayout[dpid] && initialLayout[dpid].pos;

					if (finalPos) {
						switchPositions[dpid] = formatPos(finalPos);
					} else if (initialPos) {
						switchPositions[dpid] = formatPos(initialPos, true);
					}
				});

				return switchPositions;
			}

			// console.log(layoutProjection)

			var decomposedTopology = decomposeTopology(physicalNetworkModel.physicalTopology);

			var subgraphLayout = {};
			var rootDpid;
			for (rootDpid in decomposedTopology.subGraphs) {
				var subGraph = decomposedTopology.subGraphs[rootDpid];
				subGraph.switches.forEach(function (dpid) {
					subgraphLayout[dpid] = coreSwitchConfiguration[rootDpid];
				});
			}

			var topology = physicalNetworkModel.physicalTopology;
			// var topology = decomposedTopology.coreTopology;

			// ALIGNMENT WITH MAP BOX
			var mapBBox = physicalTopologyMap.node().getBBox()
			var tlPos = [mapBBox.x,mapBBox.y];
			var brPos = [mapBBox.x + mapBBox.width,mapBBox.y + mapBBox.height];
			coreSwitchConfiguration['tl'] = {pos: projection.invert(tlPos)};
			coreSwitchConfiguration['br'] = {pos:projection.invert(brPos)};
			physicalNetworkModel.physicalTopology.switches.push('tl');
			physicalNetworkModel.physicalTopology.switches.push('br');


			var switchPositions = getSwitchPositions(topology, coreSwitchConfiguration, subgraphLayout, 'latlng');
			topology.layout = switchPositions;


			API.layoutTopology(topology, 'physical', function (svg) {

				drawSVGTopology('physical', svg);

				// add labels from the globalLayout TODO: move to layoutTopology?
				var dpid;
				for (dpid in coreSwitchConfiguration) {
					var layout = coreSwitchConfiguration[dpid];
					if (layout.name) {
						var selector = '#physical_switch-' + dpid.replace(/:/g, '_') + ' text';
						d3.select(selector).text(layout.name);
						/*var physicalSwitchID = 'g#physical_switch-' + dpid.replace(/:/g, '_');
						d3.select(physicalSwitchID).classed('core-switch',true);
						d3.select(physicalSwitchID+'.used').classed('core-switch-used',true);*/
					}
				}

				showInactiveLinks(svg, physicalNetworkModel.physicalTopology.links);

				drawFlowpaths(svg, 'physical', physicalNetworkModel.flowPaths);
				showHostMACandIP('physical', physicalNetworkModel.hosts);


				svg = d3.select(svg);

				// SIZE TO MATCH MAP BOX

				svg.attr('viewBox', physicalTopologyMap.attr('viewBox'));

				// ALIGNMENT WITH MAP BOX
				var tl = document.querySelector('#physical_switch-tl ellipse');
				var br = document.querySelector('#physical_switch-br ellipse');
				var tlCenter = bbCenter(tl.getBBox());
				var brCenter = bbCenter(br.getBBox());
				// console.log(tlCenter);
				// console.log(brCenter);

				var svgBBox = svg.node().getBBox();
				// console.log(svgBBox);
				var xforms = svg.select('g').attr('transform');
				var parts  = /translate\(\s*([^\s,)]+)[ ,]([^\s,)]+)/.exec(xforms);
				var firstX = parts[1],
				    firstY = parts[2];

				var physical = d3.select('#physical');
				// var topOffset = mapBBox.y - svgBBox.y - 6;
				// var leftOffset = mapBBox.x - svgBBox.x - 6;
				var topOffset = mapBBox.y - tlCenter.y - firstY;
				var leftOffset = mapBBox.x - tlCenter.x - firstX-10;
				// console.log(topOffset)
				// console.log(leftOffset)


				// console.log(containerSize.width);
				// console.log(svgSize.width * scale);

				physical.style('top', topOffset + 'px');
				physical.style('left', leftOffset + 'px');

				tl.style.display = 'none';
				br.style.display = 'none';



				// scale down the topology and map views to fix the container
				var containerComputedStyle = window.getComputedStyle(document.getElementById('physicalContainer'));
				var containerSize = {width: containerComputedStyle.width.replace('px',''),
									 height: containerComputedStyle.height.replace('px','')};

				// TODO: the scaling is dependent on how far the graph has been slid over to align with the map
				// but I have not figured out the relationship. 100 works for most cases?
				var svgSize = {width: parseFloat(svg.attr('width').replace('pt', '')) + 100,
							   height: parseFloat(svg.attr('height').replace('pt', '')) + 100}

				var scale = Math.min(containerSize.width/svgSize.width, containerSize.height/svgSize.height);

				d3.select('#physical').style('-webkit-transform', 'scale(' + scale + ')');
				mapSvg.style('-webkit-transform', 'scale(' + scale + ')');

				d3.select('#contents').style('visibility', 'visible');

				// uses the cached values
				restoreMouseHighlighting();
				highlightUsedElements();
			    moveNodesToFront(svg);
			    moveUsedHostsToFront(svg);
			    createTooltips(svg.select('g'));

				installPhysicalTopologyEventHandlers(topology);

				if (topology.pendingIds) {
					topology.pendingIds.forEach(function (id) {
						d3.select('#' + id).classed('pending', true);
					});
				}

				cb(physicalNetworkModel);
			});

		}
	});
}