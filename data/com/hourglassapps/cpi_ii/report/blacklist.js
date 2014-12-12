/*global glb
*/
(function() {
    glb().results.setBlacklist([
	    /^(?:.*[\/.])?catalogue.conductus.ac.uk(?:[\/:?].*)?$/,
	    /^(?:.*[\/.])?diamm.ac.uk(?:[\/:?].*)?$/,
	    /^(?:.*[\/.])?chmtl.indiana.edu\/tml(?:[\/?].*)?$/,
	    /^(?:.*[\/.])?archive.org\/(?:stream|details)\/analectahymnicam20drev(?:[\/?].*)?$/,
	    /^(?:.*[\/.])?archive.org\/(?:stream|details)\/analectahymnica21drevuoft(?:[\/?].*)?$/,
	    /^(?:.*[\/.])?archive.org\/(?:stream|details)\/analectahymnicam21drev(?:[\/?].*)?$/
    ]);
}());
