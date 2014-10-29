package com.hourglassapps.util;

public class ConcreteThrower<E extends Exception> implements Thrower {
	private E mThrowable=null;
	
	public void ctch(E pThrowable) {
		if(mThrowable==null) {
			mThrowable=pThrowable;
		}
	}

	public boolean fallThrough() {
		return mThrowable!=null;
	}

	@Override
	public <E2 extends Exception> void throwCaught(Class<E2> pCatchable)
			throws E {
		if(mThrowable!=null && (pCatchable==null || pCatchable.isAssignableFrom(mThrowable.getClass()))) {
			try {
				throw mThrowable;
			} finally  {
				mThrowable=null;
			}
		}
	}

	@Override
	public void close() throws E {
		throwCaught(null);
	}
}
