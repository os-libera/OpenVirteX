function clearFlowtable() {
	selectedFlowpath = null;
	selectedFlowtableSwitchId = null;

	// show that this element is selected
	d3.select('.flowTableDisplayed').classed('flowTableDisplayed', false);

	var table = d3.select('#flowtableContents');
	table.html('');

	syncFlowtable();
}

function showFlowtable(id, cb) {
	function complete(flowTable) {
		if (logDiff(lastFlowtable, flowTable)) {
			console.log('flowtable changed. updating view.');
			lastFlowtable = JSON.parse(JSON.stringify(flowTable));
			// updateFlowtable modifies flowTable
			updateFlowtable(id, flowTable);
		}

		if (cb) {
			cb();
		}
	}

	if (id.match('physical')) {
		var dpid = id.replace('physical_switch-', '').replace(/_/g, '');
		API.getPhysicalFlowtable(dpid, function (err, flowtable) {
			if (!err) {
				complete(flowtable);
			}
		});
	} else {
		// TODO: need utility method
		var tenantId = d3.select('#virtualNetworkSelector .virtualNetwork.selected').attr('tenantid');
		var vdpid = id.replace('virtual_switch-', '').replace(/_/g, '');
		API.getVirtualFlowtable(tenantId, vdpid, function (err, flowtable) {
			complete(flowtable);
		});
	}
}

function updateFlowtable(switchId, flowtable) {
	clearFlowtable();
	selectedFlowtableSwitchId = switchId;
	//START-Ami: Fix for node color
	d3.selectAll('.colorNode').classed('colorNode', false);
	d3.select('#'+switchId).classed('colorNode', true);
	//END-Ami: Fix for node color
	var table = d3.select('#flowtableContents');

	function rowEnter(d) {
		var row = d3.select(this);

		row.classed('firstAction', d && d.wildcards);

		row.on('click', function () {
			if (d.dl_src) {
				// highlight the selected row in the flowtable
				d3.selectAll('#flowtable .selected').classed('selected', false);
				d3.select(this).classed('selected', true);

				var srcMAC = d.dl_src.replace(/:/g, '_');
				var dstMAC = d.dl_dst.replace(/:/g, '_');
				selectedFlowpath =  srcMAC + '-' + dstMAC;
				syncFlowtable();
			}
		})

		if (!d) {
			row.classed('noFlows', true);
			row.append('div')
				.classed('flowtableCell', true)
				.text('No flows');
			return;
		}

		// row.append('div')
		// 	.classed('flowtableCell', true)
		// 	.classed('wildcards', true)
		// 	.text(function (d) {
		// 		return d.wildcards;
		// 	});

		row.append('div')
			.classed('flowtableCell', true)
			.classed('dl_vlan', true)
			.text(function (d) {
				return d.dl_vlan;
			});

		row.append('div')
			.classed('flowtableCell', true)
			.classed('dl_vlan_pcp', true)
			.text(function (d) {
				return d.dl_vlan_pcp;
			});

		row.append('div')
			.classed('flowtableCell', true)
			.classed('in_port', true)
			.text(function (d) {
				return d.in_port;
			});

		row.append('div')
			.classed('flowtableCell', true)
			.classed('dl_src', true)
			.text(function (d) {
				return d.dl_src;
			});

		row.append('div')
			.classed('flowtableCell', true)
			.classed('dl_dst', true)
			.text(function (d) {
				return d.dl_dst;
			});

		row.append('div')
			.classed('flowtableCell', true)
			.classed('actions', true)
			.text(function (d) {
				var result;
				var key;
				for (key in d.actions) {
					var line = key + '=' + d.actions[key];
					result = !result ? '' : result + ', ';
					result += line;
				}

				return result;
			});
	}

	if (flowtable.length === 0) {
		flowtable.push(null);
	}

	var rows = [];
	flowtable.forEach(function (tableEntry) {
		if (tableEntry) {
			var fields = tableEntry.match;
			if (tableEntry.actionsList) {
				tableEntry.actionsList.forEach(function (action) {
					fields.actions = action;
					rows.push(fields);
					fields = {};
				});
			} else {
				rows.push(fields);
			}
		} else {
			rows.push(null);
		}

	});


	table.selectAll('.flowtableEntry')
		.data(rows)
		.enter()
		.append('div')
		.classed('flowtableEntry', true)
		.each(rowEnter);
}

function syncFlowtable() {
	function syncSelections() {
		d3.selectAll('.flowpath.selected').classed('selected', false);
		d3.selectAll('.host.selected').classed('selected', false);

		if (selectedFlowpath) {
			var srcMAC = selectedFlowpath.split('-')[0];
			var dstMAC = selectedFlowpath.split('-')[1];

			var physicalFlowpath = d3.select('#physical_' + selectedFlowpath);
			if(!physicalFlowpath.empty()) {
				// highlight the flowpaths
				physicalFlowpath.classed('selected', true);

				physicalFlowpath.moveToFront();

				// highlight the connected hosts
				d3.select('#physical_host' + srcMAC).classed('selected', true);
				d3.select('#physical_host' + dstMAC).classed('selected', true);
			}

			var virtualFlowpath = d3.select('#virtual_' + selectedFlowpath);
			if (!virtualFlowpath.empty()) {
				// highlight the flowpaths
				virtualFlowpath.classed('selected', true);

				virtualFlowpath.moveToFront();

				// highlight the connected hosts
				d3.select('#virtual_host' + srcMAC).classed('selected', true);
				d3.select('#virtual_host' + dstMAC).classed('selected', true);
			}
		}
	}

	syncSelections();
	if (selectedFlowtableSwitchId) {
		showFlowtable(selectedFlowtableSwitchId, syncSelections);
	}


}
