package com.hourglassapps.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExpansionDistributor<T> implements ExpansionReceiver<T> {
	private final static String TAG=ExpansionReceiver.class.getName();
	private final List<ExpansionReceiver<T>> mReceivers;
	private final List<Filter<List<List<T>>>> mFilters;
	private List<List<T>> mAllExpansions=new ArrayList<>();
	
	public ExpansionDistributor(
			List<Ii<ExpansionReceiver<T>, Filter<List<List<T>>>>> pReceiverFilters) {
		Ii<List<ExpansionReceiver<T>>,List<Filter<List<List<T>>>>> receiversFilters=
				Ii.unzip(pReceiverFilters);
		mReceivers=receiversFilters.fst();
		mFilters=receiversFilters.snd();
	}
	
	public static <T> ExpansionDistributor<T> relay(ExpansionReceiver<T> pReceiver, Filter<List<List<T>>> pFilter) {
		return new ExpansionDistributor<T>(
				Collections.singletonList(new Ii<ExpansionReceiver<T>, Filter<List<List<T>>>>(pReceiver, pFilter)));
	}
	
	@Override
	public void onExpansion(List<T> pExpansions) {
		mAllExpansions.add(new ArrayList<T>(pExpansions));
	}

	@Override
	public void onGroupDone(int pNumExpansions) {
		int i=0;
		List<List<T>> unmodifiableExpansions=Collections.unmodifiableList(mAllExpansions);
		if(mFilters==null) {
			for(List<T> expansions: unmodifiableExpansions) {
				mReceivers.get(i).onExpansion(expansions);
				i++;
			}
			mReceivers.get(i).onGroupDone(mAllExpansions.size());				
		} else {
			for(ExpansionReceiver<T> r: mReceivers) {
				if(mFilters.get(i).accept(unmodifiableExpansions)) {
					for(List<T> expansions: unmodifiableExpansions) {
						mReceivers.get(i).onExpansion(expansions);
					}
					mReceivers.get(i).onGroupDone(mAllExpansions.size());
				}
				i++;
			}
		}
		mAllExpansions=new ArrayList<>();
	}	
}