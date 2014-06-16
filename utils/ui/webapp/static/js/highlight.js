function highlightUsedElements() {
	d3.selectAll('#physical .host, #physical .switch, #physical .link, #physical .flowpath')
		.classed('used', false);
	var switchMapping = currentMappings.switchMapping;
	var linkMapping = currentMappings.linkMapping;

	// highlight the used flowpaths
	d3.selectAll('#virtual .flowpath')
		.classed('used', true)
		.each(function () {
			var physicalId = this.getAttribute('id').replace('virtual', 'physical');
			d3.select('#'+physicalId).classed("used", true);
		})


	// highlight the used hosts, links and nodes
	d3.selectAll('#virtual .host')
		.classed('used', true)
		.each(function () {
			var physicalId = this.getAttribute('id').replace('virtual', 'physical');
			d3.select('#'+physicalId).classed('used', true);
		});

	d3.selectAll('#virtual .switch')
		.classed('used', true)
		.each(function () {
			applyToPhysicalSwitchesMappedFrom(switchMapping, this.getAttribute('id'), function (sel) {
				sel.classed('used', true);
			});
		});

	d3.selectAll('#virtual .link')
		.classed('used', true)
		.each(function () {
			var id = this.getAttribute('id');
			if (id.match('virtual_host_link')) {
				id = id.replace('virtual', 'physical');
				d3.select('#' + id).classed('used', true);
			} else {
				applyToPhysicalLinksMappedFrom(linkMapping, id.replace('virtual_link-', ''), function (sel) {
					sel.classed('used', true);
				});
			}
		});


	// highlight the "internal physical links" of big switches
	var dpid;
	for (dpid in switchMapping) {
		switchMapping[dpid].links.forEach(function (linkId) {
			var id = '#physical_link-' + linkId;
			d3.select(id).classed('used', true);
		});
	}

	// highlight the switches referenced by physical links
	for (virtLinkId in linkMapping) {
		applyToPhysicalSwitchesReferencedByPhysicalLinksMappedFrom(linkMapping, virtLinkId, function (sel) {
			sel.classed('used', true);
		});
	}
}