function clearHostMACandIp() {
	d3.selectAll('.hostLabel').text('');
	d3.selectAll('.host rect').remove();
}

function showHostMACandIP(which, hosts) {
	var middle = d3.select('#physical-svg').node().getBBox().width / 2;
	hosts.forEach(function (host) {
		if (host.ipAddress && host.ipAddress != '0.0.0.0') {
			var hostId = '#' + which + '_host' + host.mac.replace(/:/g, '_');
			var label = d3.select(hostId + ' text');
			var anchor = label.attr('x') < middle ? 'start' : 'end';
			label.classed('hostLabel', true).text(host.ipAddress)
				.attr('text-anchor', anchor)
				.each(function () {

					var polygon = d3.select(hostId + ' polygon');
					var polygonBounds = polygon.node().getBBox();

					var label = d3.select(this);
					var dx = polygonBounds.width/2;
					label.attr('dx', anchor === 'end' ? dx : -dx);
					var textBounds = label.node().getBBox();

					d3.select(this.parentElement).insert('rect', 'text')
						.attr('x', textBounds.x - 2)
						.attr('y', polygonBounds.y + 6)
						.attr('width', textBounds.width + 4)
						.attr('height', polygonBounds.height - 12)
				});
		}
	});
}