/***************************************************************************************************
global variables ARE THERE OTHERS? SHOULD THERE BE ANY?
***************************************************************************************************/

/***************************************************************************************************
the latest update to the model
***************************************************************************************************/
var model = {}

var currentMappings;
function clearMappings() {
	currentMappings = {switchMapping: {}, linkMapping: {}};
}
clearMappings();

/***************************************************************************************************
defined by (optional) data/layout.js

{
	"<dpid>": {
		"pos": [lng,lat],
		"name": "<label>"
	}
}

***************************************************************************************************/
var coreSwitchConfiguration = {};

var virtualNetworkConfiguration = {};

var lastVirtualNetworkModel;
var lastPhysicalNetworkModel;
var lastFlowtable;

var selectedFlowpath;
var selectedFlowtableSwitchId;

