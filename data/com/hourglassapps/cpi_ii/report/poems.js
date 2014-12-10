/*global $,glb
 */

(function() { 
    $(document).on( "mobileinit", function() {
	glb().ui_management={
	    panel_open: false,
	    iframe: null
	};

	//$('body').on("pageshow", "div:jqmData(data-role='page')",
	/*
	$('body').on("pageshow",
		     function(event, ui) {
			 glb().ui_management.iframe=$(ui.nextPage).first("iframe");
			 glb().ui.resizeEl(glb().ui_management.iframe);
		     });
*/
    });

}());
