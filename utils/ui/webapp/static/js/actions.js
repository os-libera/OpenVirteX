/*

ENCAPSULATE!

*/

var allPendingActions = {};

function pendingActionsFor(tenantId) {
	var key = tenantId ? tenantId : 'physical';
	var pendingActions = allPendingActions[key];
	if (!pendingActions) {
		allPendingActions[key] = {};
		pendingActions = allPendingActions[key];
	}
	return pendingActions;
}

function processPendingActions(topology, tenantId) {
	var pendingActions = pendingActionsFor(tenantId);
	topology.pendingIds = [];
	if (pendingActions) {
		var action;
		for (action in pendingActions) {
			pendingActions[action](topology);
		}
	}
}

function updateVirtualTopologyView(tenantId) {
	var clone = JSON.parse(JSON.stringify(lastVirtualNetworkModel));
	drawVirtualNetwork(clone, tenantId, function () {
		// noop
	});
}

function linkUp(srcDPID, dstDPID, linkId) {
	var pendingActions = pendingActionsFor();
	var actionKey = srcDPID + '-' + dstDPID;
	if (!pendingActions[actionKey]) {
		pendingActions[actionKey] = function (topology) {
			var stillMissing = true;
			topology.links.forEach(function (link) {
				key = link.src.dpid + '-' + link.dst.dpid;
				if (key === actionKey) {
					stillMissing = false;
				}
			});

			if (stillMissing) {
				topology.pendingIds.push(linkId);
			} else {
				delete pendingActions[actionKey];
			}
		}
	}

	API.linkUp(srcDPID, dstDPID, function (err, result) {
		if (err) {
			//alert('something went wrong: ' + err);
		}
	});
}

function linkDown(srcDPID, dstDPID, linkId) {
	var pendingActions = pendingActionsFor();
	var actionKey = srcDPID + '-' + dstDPID;
	if (!pendingActions[actionKey]) {
		pendingActions[actionKey] = function (topology) {
			var stillThere = false;
			topology.links.forEach(function (link) {
				key = link.src.dpid + '-' + link.dst.dpid;
				if (key === actionKey) {
					stillThere = true;
				}
			});

			if (stillThere) {
				topology.pendingIds.push(linkId);
			} else {
				delete pendingActions[actionKey];
			}
		}
	}

	API.linkDown(srcDPID, dstDPID, function (err, result) {
		if (err) {
			//alert('something went wrong: ' + err);
		}
	});
}


function startPing(tenantId, srcMAC, targetMAC) {
	var pendingActions = pendingActionsFor(tenantId);
	var count=0;
	var actionKey = srcMAC + '-' + targetMAC;
	if (!pendingActions[actionKey]) {
		pendingActions[actionKey] = function (topology) {
			// TODO: inconsistent casing in the API
			// flowpaths use lower case, MACs are upper case
			var key = srcMAC.toLowerCase() + '-' + targetMAC.toLowerCase();
			if (!topology.flowPaths[key]) {
				topology.flowPaths[key] = [];
				var id = 'virtual_' + key.replace(/:/g, '_');
				topology.pendingIds.push(id);
				/*START-Ami: To stop red link after 5 seconds*
				setTimeout(function(){d3.select('#'+id).remove();},9000);
				/*END-Ami: To stop red link after 5 seconds*/
				/*START-Ami: To stop red link after 9 seconds*/
				count++;
				if(count==9){
					d3.select('#'+id).remove();
				}
				/*START-Ami: To stop red link after 9 seconds*/
			} else {
				delete pendingActions[actionKey];
			}
		};
	}

	updateVirtualTopologyView(tenantId);

	API.startPing(srcMAC, targetMAC, function (err, result) {
		if (err) {
			//alert('something went wrong: ' + err);
		}
	});
}

// TODO: should not be able to start flows when a stopPing action is pending
// NOTE: assumes that any flow in this network is stopping
function stopPing(tenantId) {
	var pendingActions = pendingActionsFor(tenantId);

	if (!pendingActions.stopPing) {
		pendingActions.stopPing = function (topology) {
			if (Object.keys(topology.flowPaths).length) {
				var key;
				for (key in topology.flowPaths) {
					// console.log(key);
					// add the flow id for this
					var id = 'virtual_' + key.replace(/:/g, '_');
					topology.pendingIds.push(id);
				}
			} else {
				delete pendingActions.stopPing;
			}
		}
	}

	updateVirtualTopologyView(tenantId);

	API.stopPing(tenantId, function (err, result) {
		if (err) {

		}
	});
}
