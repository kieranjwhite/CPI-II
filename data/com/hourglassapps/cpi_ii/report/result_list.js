/*global $,glb
*/
(function() {
    glb().results={};

    var g=glb();
    var res=g.results;

    res.root="results/completed/";
    res.document_root="";
    
    g.setPage=function(pageName) {
	res.page=pageName;
    };

    g.page=function() {
	g.rtu.assert(g.results.page!=='undefined');
	return g.results.page;
    };

    g.refreshList=function(results) {
	g.rtu.assert(glb().results.page!=='undefined');

	var list="<ul data-role=\"listview\">\n";
	for(var i=0; i<results.length; i++) {
	    if(results[i]===null) {
		break;
	    }
	    var link=g.results.document_root+results[i];
	    list+="<li><a href=\""+link+"\" rel=\"external\">"+link+"</a><li>";
	}
	list+="</ul>\n";
	var page=$('#'+glb().results.page);
	page.html(list);
	page.trigger("create");
    };
}());
