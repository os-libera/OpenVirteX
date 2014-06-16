/*global d3, document*/

(function () {

var updateTimeout, retryTimeout;

function virtualNetworkSelectionChanged() {
	API.abortPendingCalls();
	clearTimeout(updateTimeout);
	clearTimeout(retryTimeout);

	clearHostMACandIp();

	clearVirtualNetwork();

	clearFlowtable();

	// clear the cached switch and link mappings
	clearMappings();

	// since no mappings, this clears highlighting in the physical view
	highlightUsedElements();

	startPolling();
}

d3.select(document.body).on('mousedown', function () {
	API.abortPendingCalls();
	clearTimeout(updateTimeout);
	clearTimeout(retryTimeout);
});

d3.select(document.body).on('mouseup', function () {
	startPolling();
});

// depends on global model. refactor
function updateHeader(model) {
	d3.select('#lastUpdate').text(new Date());

	// update the network selector popup
	var networks = d3.select('#virtualNetworkSelector #virtualNetworks')
		.selectAll('.virtualNetwork')
		.data(model.virtualNetworks, function (d) {return d;});
	networks.enter()
		.append('div')
		.classed('virtualNetwork', true)
		.attr('tenantid', function (d) {return d;})
		.text(function (d) {return virtualNetworkConfiguration[d]})
		.on('click', function () {
			virtualNetworkSelectionChanged();
			d3.select('#virtualNetworkSelector .virtualNetwork.selected').classed('selected', false);
			d3.select(this).classed('selected', true);
		})
		.append('div')
			.attr('class', 'eye');

	networks.exit().remove();
}

d3.select('#stopPing').on('click', function () {
	// TODO: need utility function for this
	var selectedVirtualNetwork = d3.select('#virtualNetworkSelector .virtualNetwork.selected');
	var tenantId = selectedVirtualNetwork.attr('tenantid');

	stopPing(tenantId);
});


function startPolling() {
	var retryMS = 10000;
	var updateRateMS = 1000;

	function sync() {
	  	clearTimeout(retryTimeout);

		// set the retry timeout
		// if something goes wrong below, we try again later
		retryTimeout = setTimeout(function () {
			console.log('sync timed out. retrying...');
			clearTimeout(updateTimeout);
			sync();
		}, retryMS);

		if (parseURLParameters().nopolling) {
			clearTimeout(retryTimeout);
		}

		syncPhysicalNetwork(function (model) {
			function complete() {
				syncFlowtable();
			  	clearTimeout(retryTimeout);
			  	updateTimeout = setTimeout(sync, updateRateMS);
			  	
				if (parseURLParameters().nopolling) {
					clearTimeout(updateTimeout);
				  	clearTimeout(retryTimeout);
				}
			}

			// var scrollTop = document.getElementById('flowtableContainer').scrollTop;
			updateHeader(model);
			// document.getElementById('flowtableContainer').scrollTop = scrollTop;

			var selectedVirtualNetwork = d3.select('#virtualNetworkSelector .virtualNetwork.selected');
			if (selectedVirtualNetwork.empty()) {
				selectedVirtualNetwork = d3.select('#virtualNetworkSelector .virtualNetwork');
				selectedVirtualNetwork.classed('selected', true);
			}

			if (selectedVirtualNetwork.empty()) {
				clearVirtualNetwork();
				complete();
			} else {
				syncVirtualNetwork(selectedVirtualNetwork.attr('tenantid'), function () {
					complete();
				});
			}
		});
	}

	sync();
	
}

var skin = parseURLParameters().s;
if (skin) {
	d3.select(document.body).classed(skin, true);
}

startPolling();
}());









