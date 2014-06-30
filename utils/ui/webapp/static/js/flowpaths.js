

function drawFlowpaths(svg, which, flowPaths) {
	var pair;
	for (pair in flowPaths) {
		var hosts = pair.replace(/:/g, '_').split('-');
		var hostId1 = which + '_host' + hosts[0];
		var hostId2 = which + '_host' + hosts[1];

		var host1 = d3.select('#'+hostId1 + ' polygon');
		var host2 = d3.select('#'+hostId2 + ' polygon');

		if (host1.empty() || host2.empty()) {
			console.log('no host with id: ' + hostId1 + ' or ' + hostId2);
			return;
		}

		var host1bb = host1.node().getBBox();
		var host2bb = host2.node().getBBox();

		var points = [
			bbCenter(host1bb)
		];

		var flowPath = flowPaths[pair];
		flowPath.forEach(function (s) {
			var switchId = which + '_switch-' + s.replace(/:/g, '_');
			switchbb = d3.select('#'+switchId + ' ellipse').node().getBBox();
			points.push(bbCenter(switchbb));
		});

		points.push(bbCenter(host2bb));

		var g = d3.select(svg).select('g').append('g')
            .classed('flowpath', true)
			.attr("id", which+'_'+pair.replace(/:/g, '_'));
		g.append("path")
	            .attr("d", line(points))
	            .classed('background', true);
		g.append("path")
	            .attr("d", line(points))
        	    .attr('stroke-dasharray', '8, 30')
				.append('svg:animate')
					.attr('attributeName', 'stroke-dashoffset')
					.attr('attributeType', 'xml')
					.attr('from', '500')
					.attr('to', '-500')
					.attr('dur', '20s')
					.attr('repeatCount', 'indefinite');

	}
}