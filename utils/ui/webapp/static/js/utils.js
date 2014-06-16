/***************************************************************************************************
return the center of a bounding box
***************************************************************************************************/
function bbCenter(bb) {
	return {x: bb.x + bb.width/2, y: bb.y + bb.height/2};
}

/***************************************************************************************************
extract url parameters into a map
***************************************************************************************************/
function parseURLParameters() {
	var parameters = {};

	var search = location.href.split('?')[1];
	if (search) {
		search.split('&').forEach(function (param) {
			var key = param.split('=')[0];
			var value = param.split('=')[1];
			parameters[key] = decodeURIComponent(value);
		});
	}

	return parameters;
}

/***************************************************************************************************
convenience function for moving an SVG element to the front so that it draws on top
***************************************************************************************************/
d3.selection.prototype.moveToFront = function() {
  return this.each(function(){
    this.parentNode.appendChild(this);
  });
};

d3.selection.prototype.moveToBack = function() {
  return this.each(function(){
    this.parentNode.insertBefore(this, this.parentNode.firstChild);
  });
};

/***************************************************************************************************
standard function for generating the 'd' attribute for a path from an array of points
***************************************************************************************************/
var line = d3.svg.line()
    .x(function(d) {
    	return d.x;
    })
    .y(function(d) {
    	return d.y;
    });

/***************************************************************************************************

***************************************************************************************************/
function doConfirm(prompt, cb, options) {
	var confirm = d3.select('#confirm');
	confirm.select('#confirm-prompt').text(prompt);

	var select = d3.select(document.getElementById('confirm-select'));
	if (options) {
		select.style('display', 'block');
		select.text('');
		select.selectAll('option').
			data(options)
			.enter()
				.append('option')
					.attr('value', function (d) {return d})
					.text(function (d) {return d});
	} else {
		select.style('display', 'none');
	}

	function show() {
		confirm.style('display', '-webkit-box');
		confirm.style('opacity', 0);
		setTimeout(function () {
			confirm.style('opacity', 1);
		}, 0);
	}

	function dismiss() {
		confirm.style('opacity', 0);
		confirm.on('webkitTransitionEnd', function () {
			confirm.style('display', 'none');
			confirm.on('webkitTransitionEnd', null);
		});
	}

	confirm.select('#confirm-ok').on('click', function () {
		d3.select(this).on('click', null);
		dismiss();
		if (options) {
			cb(select[0][0].value);
		} else {
			cb(true);
		}
	});

	confirm.select('#confirm-cancel').on('click', function () {
		d3.select(this).on('click', null);
		dismiss();
		cb(false);
	});

	show();
}


/***************************************************************************************************
jsonDiff

	Copyright 2013 Paul Greyson
	MIT License

***************************************************************************************************/
function jsonDiff(val1, val2, results, keyPath) {

	keyPath = keyPath || '';

	if (val1 && !val2) {
		results[keyPath] =  'non-null => null-or-undefined';
	} else if (!val1 && val2) {
		results[keyPath] =  'null-or-undefined => non-null';
	} else if (val1.constructor !== val2.constructor) {
    	results[keyPath] = val1.constructor + ' => ' + val2.constructor;
    } else if (typeof val1 !== 'undefined' && typeof val2 !== 'undefined') {
        switch (val1.constructor) {
            case Array:
            	keyPath += '/';
                diffArrays(val1, val2);
                break;
            case Object:
            	keyPath += '/';
                diffObjects(val1, val2);
                break;
            case Number:
            case String:
            case Boolean:
                diffLiterals(val1, val2);
                break;
            default:
                throw new Error('Unsupported object type in jsonDiff');
        }
    }

    function diffObjects(obj1, obj2) {
        var key;
        for (key in obj1) {
        	if (obj1.hasOwnProperty(key)) {
	            var newKeyPath = keyPath + key;
	            if (typeof obj2[key] !== 'undefined') {
	                // recurse
	                jsonDiff(obj1[key], obj2[key], results, newKeyPath);
	            } else {
	                results[newKeyPath] = '-key';
	            }
        	}
        }
        for (key in obj2) {
        	if (obj2.hasOwnProperty(key)) {
	            var newKeyPath = keyPath + key;
	            if (typeof obj1[key] === 'undefined') {
	                results[newKeyPath] = '+key';
	            }
	        }
        }
    }

    function diffArrays(arr1, arr2) {
        if (arr1.length !== arr2.length) {
        	var delta = arr2.length - arr1.length;
        	if (delta > 0) {
        		delta = '+' + delta;
        	}
            results[keyPath] = '[' + delta + ']'
        } else {
            var i;
            for (i=0; i<arr1.length; i+=1) {
                var newKeyPath = keyPath + '[' + i + ']';
                jsonDiff(arr1[i], arr2[i], results, newKeyPath);
            }
        }
    }

    function diffLiterals(lit1, lit2) {
        if (lit1 !== lit2) {
            results[keyPath] = lit1 + ' => ' + lit2;
        }

    }
}

function logDiff(val1, val2) {
	var results = {};
	jsonDiff(val1, val2, results);
	var different = Object.keys(results).length;
	if (different) {
		console.log(results);
	}
	return different;
}







