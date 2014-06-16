var projection;

function createMap(containerNode, mapId) {

	var width = containerNode.node().clientWidth;
	    height = containerNode.node().clientHeight;

	// MAGIC NUMBERS derived for the map of the continental US.
	// TODO: Generalize for other locations. Should be possible to make it in terms of the extent of the map data in topojson
	// https://github.com/topojson/topojson-specification
	var scale = (width + 1) / (Math.PI/12);
	var translate = [width*1.5, height*1];

	projection = d3.geo.mercator()
	    .scale(scale/(Math.PI*2))
	    .translate(translate)
	    .precision(.1);

	var map = containerNode.append("g").attr('id', mapId);

	var path = d3.geo.path().projection(projection);

	map.selectAll('path')
		.data(topojson.object(mapTopology, mapTopology.objects.states).geometries)
	    	.enter()
	      		.append('path')
	      		.attr('d', path);

	return map;
}
