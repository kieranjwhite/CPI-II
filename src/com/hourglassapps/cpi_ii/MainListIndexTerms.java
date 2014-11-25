package com.hourglassapps.cpi_ii;

import java.io.IOException;
import java.util.List;

import com.hourglassapps.cpi_ii.lucene.Freq2TermMapper;
import com.hourglassapps.cpi_ii.lucene.IndexViewer;
import com.hourglassapps.util.ExpansionReceiver;
import com.hourglassapps.util.Log;

public class MainListIndexTerms {
	private final static String TAG=MainListIndexTerms.class.getName();
	public static void main(String[] args) {
		if(args.length>1) {
			Log.e(TAG, "Can accept at most one arg, the filename for serialised stem group data");
			System.exit(-1);
		}
		
		boolean freqs=false;
		String stemFile=null;
		for(int i=0; i<args.length; i++) {
			String arg=args[i];
			switch(arg) {
			case "--display-freqs":
				freqs=true;
				break;
			default:
				stemFile=args[i];
			}
		}

		
		try {
			final IndexViewer index=new IndexViewer(MainIndexConductus.UNSTEMMED_2_STEMMED_INDEX);
			if(freqs) {
				Freq2TermMapper mapper=new Freq2TermMapper();
				index.visit(CPIFields.KEY.fieldVal(), mapper);
				mapper.display();
			}
			
			if(stemFile!=null) {
				CPIUtils.listAllTokenExpansions(index, stemFile, new ExpansionReceiver<String>(){
					//final AbstractComboExpander<String, String> expander=
					//		new AbstractComboExpander<String, String>(stem2Variants, new IdentityConverter<String>()){

					@Override
					public void onExpansion(List<String> pExpansions) {
						if(pExpansions.size()>=1) {
							System.out.print(pExpansions.get(0));
						}
						for(String s: pExpansions.subList(1, pExpansions.size())) {
							System.out.print(' ');
							System.out.print(s);
						}
						System.out.println();
					}

					@Override
					public void onGroupDone(int pNumExpansions) {
						if(pNumExpansions>0) {
							System.out.println();
						}

					}
				});
			}
		} catch (NumberFormatException | IOException e) {
			Log.e(TAG, e);
		}
	}
}
