function createTooltips(container) {
	var horzMid = container.node().getBBox().width / 2;
	var vertMid = container.node().getBBox().height / 2;

	// offset by the container transform. this is ugly
	var m = container.node().getCTM()
	vertMid -= m.f/m.a;

	container.selectAll('.host title, .switch title').each(function () {
		var title = d3.select(this);
		var g = d3.select(this.parentElement);
		var bbox;
		if (g.classed('host')) {
			bbox = g.select('polygon').node().getBBox();
		} else if (g.classed('switch')) {
			bbox = g.select('ellipse').node().getBBox();
		} else {
			bbox = g.node().getBBox();
		}

		var center = bbCenter(bbox);
		var tooltipId = g.attr('id') + '_tooltip';
		var tooltip = container
						.append('g')
						.attr('id', tooltipId)
						.moveToFront();
		var anchor = center.x < horzMid ? 'start' : 'end';

		var vertOffset = center.y < vertMid ? bbox.height : 0;

		var label = tooltip.append('text')
						.text(title.text())
						.attr('x', center.x)
						.attr('y', bbox.y + vertOffset)
						.attr('text-anchor', anchor)
						.classed('tooltip', true);
		bbox = label.node().getBBox();
		tooltip.insert('rect', '.tooltip')
			.attr('x', bbox.x - 4)
			.attr('y', bbox.y - 4)
			.attr('width', bbox.width + 8)
			.attr('height', bbox.height + 8)
			.classed('tooltip', true);
		title.remove();

		g
			.on('mouseover.tooltip', function () {
				d3.select('#' + tooltipId).classed('highlight', true);
			})
			.on('mouseout.tooltip', function () {
				d3.select('#' + tooltipId).classed('highlight', false);
			});
	});

	container.selectAll('title').remove();
}