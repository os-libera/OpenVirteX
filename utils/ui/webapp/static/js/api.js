(function () {

var url = 'http://localhost:5000/';

var apiAborter = {
	abort: false
}

function complete(cb, doNotAbort) {
	// capture the current aborter object at this scope
	var aborter = apiAborter;
	return function(err, results) {
		if (aborter.abort && !doNotAbort) {
			console.log('aborting')
			return;
		}

		if (err) {
			if (err.statusText) {
				//alert('Proxy returned: ' + err.statusText);
			} else if (err.status) {
				//alert('Proxy returned: ' + err.status);
			} else {
				//alert('Something went wrong. Check the output of proxy.js.');
			}
			cb(err);
		} else {
			cb(null, results);
		}
	}
}

// TODO: conform naming to ovx API e.g. getX vs listX


API = {
	abortPendingCalls: function () {
		apiAborter.abort = true;
		apiAborter = {
			abort: false
		}
	},

	getPhysicalTopology: function (cb) {
		d3.json(url + 'getPhysicalTopology', complete(cb));
	},

	listVirtualNetworks: function (cb) {
		d3.json(url + 'listVirtualNetworks', complete(cb));
	},

	getVirtualTopology: function (tenantId, cb) {
		d3.json(url + 'getVirtualTopology?tenantId=' + tenantId, complete(cb));
	},

	getVirtualSwitchMapping: function (tenantId, cb) {
		d3.json(url + 'getVirtualSwitchMapping?tenantId=' + tenantId, complete(cb));
	},

	getVirtualLinkMapping: function (tenantId, cb) {
		d3.json(url + 'getVirtualLinkMapping?tenantId=' + tenantId, complete(cb));
	},

	listVirtualHosts: function (tenantId, cb) {
		d3.json(url + 'listVirtualHosts?tenantId=' + tenantId, complete(cb));
	},

	listPhysicalHosts: function (cb) {
		d3.json(url + 'listPhysicalHosts', complete(cb));
	},

	getPhysicalFlowpaths : function (cb) {
		// cb(null, []);
		d3.json(url + 'getPhysicalFlowpaths', complete(cb));
	},

	getVirtualFlowpaths : function (tenantId, cb) {
		// cb(null, []);
		d3.json(url + 'getVirtualFlowpaths?tenantId=' + tenantId, complete(cb));
	},

	getVirtualFlowtable : function (tenantId, vdpid, cb) {
		d3.json(url + 'getVirtualFlowtable?tenantId=' + tenantId + '&vdpid=' + vdpid, complete(cb));
	},

	getPhysicalFlowtable : function (dpid, cb) {
		d3.json(url + 'getPhysicalFlowtable?dpid=' + dpid, complete(cb));
	},

	stopPing: function (tenantId, cb) {
		d3.json(url + 'stopPing?tenantId=' + tenantId, complete(cb, true));
	},

	startPing: function (srcMAC, dstMAC, cb) {
		d3.json(url + 'startPing?src=' + srcMAC + '&dst=' + dstMAC, complete(cb, true));
	},

	linkUp: function (srcDPID, dstDPID, cb) {
		d3.json(url + 'linkup?src=' + srcDPID.toUpperCase() + '&dst=' + dstDPID.toUpperCase(), complete(cb, true));
	},

	linkDown: function (srcDPID, dstDPID, cb) {
		d3.json(url + 'linkdown?src=' + srcDPID.toUpperCase() + '&dst=' + dstDPID.toUpperCase(), complete(cb, true));
	},

	layoutTopology: function (topology, prefix, cb) {

        var urlParams = 'prefix=' + prefix;
        urlParams += '&fixedSwitchWidth=' + graphvizLayoutParameters[prefix].fixedSwitchWidth;
        urlParams += '&repositionableSwitchWidth=' + graphvizLayoutParameters[prefix].repositionableSwitchWidth;
        urlParams += '&hostWidth=' + graphvizLayoutParameters[prefix].hostWidth

        var aborter = apiAborter;

		d3.xml(url + 'layoutTopology?' + urlParams, "image/svg+xml", function (xml) {
				if (aborter.abort) {
					console.log('aborting')
					return;
				}

				if (xml) {
					cb(document.importNode(xml.documentElement, true));
				} else {
					//alert('Something went wrong in layoutTopology. Check the output of proxy.js.')
				}
			})
			.header('Content-Type', 'application/json')
			.send('POST', JSON.stringify(topology));
	}
};

}());

