/* lng, lat */
coreSwitchConfiguration = {
	"00:00:00:00:00:01:00:00": {
		"name": "SFO",
		"pos": [-122.386665,37.616611]
	},
	"00:00:00:00:00:02:00:00": {
		"name": "SEA",
		"pos": [-122.33242,47.611024]
	},
	"00:00:00:00:00:03:00:00": {
		"name": "LAX",
		"pos": [-118.238983,34.102708]
	},
	"00:00:00:00:00:04:00:00": {
		"name": "ATL",
		"pos": [-84.387360,33.758599]
	},
	"00:00:00:00:00:05:00:00": {
		"name": "IAD",
		"pos": [-77.447605,38.956205]
	},
	"00:00:00:00:00:06:00:00": {
		"name": "EWR",
		"pos": [-74.161921,40.714802]
	},
	"00:00:00:00:00:07:00:00": {
		"name": "SLC",
		"pos": [-111.891289,41.766242]
	},
	"00:00:00:00:00:08:00:00": {
		"name": "MCI",
		"pos": [-94.716311,39.299768]
	},
	"00:00:00:00:00:09:00:00": {
		"name": "ORD",
		"pos": [-87.902212,41.981634]
	},
	"00:00:00:00:00:0a:00:00": {
		"name": "CLE",
		"pos": [-81.836257,41.411256]
	},
	"00:00:00:00:00:0b:00:00": {
		"name": "IAH",
		"pos": [-97.338122,32.586944]
	}
}

/*START-Ami: Positioning HQ and RHQ text*/

virtualHostConfiguration = {
		
		"virtual_host00_00_00_09_00_01" : {
			"text": "HQ",
			"textPosX": 550,
			"textPosY": -648.82,
			"fontColor": "yellow"
		},
		
		"virtual_host00_00_00_07_00_02" : {
			"text": "B",
			"textPosX": 198,
			"textPosY": -637.66,
			"fontColor": "red"
		},
		
		"virtual_host00_00_00_02_00_04" : {
			"text": "B",
			"textPosX": 42,
			"textPosY": -621.32,
			"fontColor": "red"
		},
		
		"virtual_host00_00_00_03_00_05" : {
			"text": "B",
			"textPosX": 190,
			"textPosY": -13.61,
			"fontColor": "red"
		},
		
		"virtual_host00_00_00_0B_00_03" : {
			"text": "B",
			"textPosX": 335,
			"textPosY": -8,
			"fontColor": "red"
		},
		
		"virtual_host00_00_00_05_00_06" : {
			"text": "B",
			"textPosX": 800,
			"textPosY": -100.93,
			"fontColor": "red"
		},
		
		"virtual_host00_00_00_01_00_07" : {
			"text": "RHQ",
			"textPosX": -2,
			"textPosY": -115.93,
			"fontColor": "green"
		},
		
		"virtual_host00_00_00_06_00_08" : {
			"text": "RHQ",
			"textPosX": 835,
			"textPosY": -455,
			"fontColor": "green"
		}
}

/*END-Ami: Positioning HQ and RHQ text*/

virtualNetworkConfiguration = {
	1: "Giant Switch",
	2: "Physical Clone",
	3: "Ring",
	4: "Diamond",
	5: "Timezones"
}


