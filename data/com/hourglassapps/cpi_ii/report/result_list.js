/*global $,glb,jx
*/
(function() {
    var g=glb();
    var document_root="../";
    var query=null;
    var blacklist=[];
    var num_blacklisted=0;

    g.results={
	page:null,
	root:"results/completed/",
	
	setPage:function(pageName) {
	    g.results.page=pageName;
	},

	setQuery:function(pQuery) {
	    query=pQuery;

	    var title;
	    var lastUnderscorePos=pQuery.lastIndexOf('_');
	    if(lastUnderscorePos>-1) {
		var firstPlusPos=pQuery.indexOf('+'); //word after first space is a label
		var secondPlusPos=pQuery.substring(firstPlusPos+1).indexOf('+')+firstPlusPos+1;
		title=pQuery.substring(firstPlusPos+1, secondPlusPos)+": "+pQuery.substring(secondPlusPos+1);
	    } else {
		title=query;
	    }
	    $("div[data-role='header']>h2").html(title);
	},

	setBlacklist:function(pBlacklist) {
	    blacklist=pBlacklist;
	    num_blacklisted=blacklist.length;
	}
    };

    var blacklisted=function(url) {
	for(var i=0; i<num_blacklisted; i++) {
	    if(blacklist[i].test(url)) {
		return true;
	    }
	}
	return false;
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

    var pathToTextCopy=function(path) {
	var middleArr=path.split('/');
	var journalName=middleArr[1];
	var journalNum=journalName.split('_')[0];
	var ngram=middleArr[3];
	var numExtn=middleArr[4];
	var num=numExtn.split('.')[0];
	return "results/completed/"+journalNum+'+'+g.rtu.escapeHTML(ngram)+'+'+num+"/links.js";
    };
    
    var listItem=function(data, original_url, hidden) {
	var style;
	if(hidden) {
	    style="style=\"display:none;\"";
	} else {
	    style="";
	}
	var title;
	if(data.t==="") {
	    title=data.p;
	} else {
	    title=data.t;
	}
	/*
	return "<li "+style+">"+
	    "<div data-role=\"controlgroup\" data-type=\"horizontal\" data-mini=\"true\" style>"+
	    "<a class=\"ui-btn ui-btn-icon-right ui-icon-action\" data-role=\"button\" href=\""+original_url+"\" rel=\"external\">"+title+"</a>"+
	    "<a class=\"ui-btn ui-btn-icon-right ui-icon-arrow-d\" data-role=\"button\" href=\""+document_root+data.p+"\" rel=\"external\">Local copy</a>"+
	    "</div>"+
	    "</li>\n";
	 */
	return "<li "+style+">"+
	    "<a href=\""+original_url+"\" data-rel=\"external\" target=\"_blank\"><h3>"+g.rtu.escapeHtml(title)+"</h3><p>"+g.rtu.escapeHtml(original_url)+"</p></a>"+
	    "<a href=\""+document_root+data.p+"\" data-rel=\"external\" target=\"_blank\">Local copy</a>"+
	    "</li>\n";
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
		//$("#sources").load(document_root+source.file, function() {
		jx.load(document_root+source.file, 'text/plain') .then(function(response) {
		    //var sources=$("#sources").text();
		    var sources=response.result;
		    if(sources==="") {
			g.rtu.report("Unable to load original source URLs. Your browser may be imposing cross-domain restrictions on iframe loading. Not all results will be listed");
		    }
		    var original_url=sources.split('\n')[source.line-1];
		    if(urlToIdx.hasOwnProperty(original_url)) {
			if(source.i<urlToIdx[original_url]) {
			    urlToIdx[original_url]=source.i;
			}
		    } else if(blacklisted(original_url)) {
			urlToIdx[original_url]=-1;
		    } else {
			urlToIdx[original_url]=source.i;
		    }
		    var changes="";
		    idxToPendingURL[source.i]=original_url;
		    if(source.i===todo) {
			var any_visible=false;
			while(idxToPendingURL.hasOwnProperty(todo)) {
			    var visible=(urlToIdx[idxToPendingURL[todo]]===todo);
			    any_visible=any_visible || visible;
			    changes+=listItem(results[todo], original_url, !visible);
			    delete(idxToPendingURL[todo]);
			    todo++;
			}
		    }
		    if(changes.length>0) {
			list.append(changes);
		    }
		    if(any_visible) {
			list.listview("refresh");
		    }
		    deferred.resolve();
		}), function(pErr) {
		    g.rtu.report("Failed to load source: "+pErr.msg+" ("+pErr.status+")");
		};
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
