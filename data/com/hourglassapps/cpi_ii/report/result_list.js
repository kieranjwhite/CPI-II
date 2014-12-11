/*global $,glb
*/
(function() {
    var g=glb();
    g.results={
	root:"results/completed/",
	document_root:"",
	page:null,
	query:null,
	
	setPage:function(pageName) {
	    g.results.page=pageName;
	},

	setQuery:function(query) {
	    g.results.query=query;
	    $("div[data-role='header']>h3").html(query);
	}

    };

    var src=function(result_idx,path) {
	var source={
	    i:result_idx,
	    file:null,
	    line:null
	};

	var parent=g.rtu.dirname(path);
	var parentName=g.rtu.basename(parent);
	source.file=parent+'/'+'_'+parentName;
	var filename=g.rtu.basename(path);
	var extensionIdx=filename.lastIndexOf(".");
	if(extensionIdx===-1) {
	    source.line=filename>>0;
	} else {
	    source.line=filename.substring(0, extensionIdx)>>0;
	}
	return source;
    };
    
    var listItem=function(data,hidden) {
	var style;
	if(hidden) {
	    style="style=\"display:none;\"";
	} else {
	    style="";
	}
	
	if(data.t==="") {
	    return "<li "+style+"><a class=\"ui-btn ui-btn-icon-right ui-icon-carat-r\" href=\""+g.results.document_root+data.p+"\" rel=\"external\">"+data.p+"</a></li>\n";
	} else {
	    return "<li "+style+"><a class=\"ui-btn ui-btn-icon-right ui-icon-carat-r\" href=\""+g.results.document_root+data.p+"\" rel=\"external\">"+data.t+"</a></li>\n";
	}
    };

    g.list=function(results) {
	g.rtu.assert(glb().results.page!=='undefined');
	//var data=g.results.document_root+results[i];
	//var page=$('#'+glb().results.page);
	//page.append(list);
	//page.trigger("create");

	var todo=0;
	var idxToPendingURL={};
	var urlToIdx={};
	var list=$("#list").first();

	var lookupSrc=function(promise, i) {
	    return promise.then(function() {
		var source=src(i,results[i].p);
		var deferred=$.Deferred();
		$("#sources").load(source.file, function() {
		    var original_url=$(this).html().split('\n')[source.line-1];
		    if(urlToIdx.hasOwnProperty(original_url)) {
			if(source.i<urlToIdx[original_url]) {
			    urlToIdx[original_url]=source.i;
			}
		    } else {
			urlToIdx[original_url]=source.i;
		    }
		    var changes="";
		    idxToPendingURL[source.i]=original_url;
		    if(source.i===todo) {
			var any_visible=false;
			while(idxToPendingURL.hasOwnProperty(todo)) {
			    var visible=urlToIdx[idxToPendingURL[todo]]===todo;
			    any_visible=any_visible || visible;
			    changes+=listItem(results[todo], !visible);
			    delete(idxToPendingURL[todo]);
			    todo++;
			}
		    }
		    if(changes.length>0) {
			list.append(changes);
		    }
		    if(any_visible) {
			list.trigger("create");
		    }
		    deferred.resolve();
		});
		return deferred.promise();
	    });
	};
	
	var deferred=$.Deferred();
	deferred.resolve();
	var promise=deferred.promise();
	for(var i=0; i<results.length; i++) {
	    if(results[i]===null) {
		break;
	    }
	    promise=lookupSrc(promise, i);
	}
    };
}());
