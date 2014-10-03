package com.hourglassapps.util;

public class Log {
	public static enum Level {
		VERBOSE(2), INFO(1), ERROR(0);
		
		private final int mUnimportance;
		Level(int pUnimportance) {
			this.mUnimportance=pUnimportance;
		}
		
		public int unimportance() {
			return mUnimportance;
		}
	}
	
    public static Log.Level LEVEL = Log.Level.VERBOSE;

    static private void write(String pTag, String pMsg) {
    	System.err.println(pTag+": "+pMsg);
    }
    
    static private void printStack(String tag, Throwable e) {
    	String msg=e.getMessage();
    	if(msg!=null) {
    		write(tag, e.getMessage());
    	} else {
    		write(tag, "Exception: "+e);
    	}
		for(StackTraceElement frame: e.getStackTrace()) {
			write(tag, "\t"+frame.toString()+" ("+frame.getClassName()+":"+frame.getLineNumber()+")");
		}
		Throwable cause=e.getCause();
		if(cause!=null) {
			write(tag, "caused by...");
			printStack(tag, cause);
		}
    }
    
    static public void v(String tag, String msgFormat, Object...args)
    {
        if (LEVEL.unimportance()<=Log.Level.VERBOSE.unimportance())
        {
            write(tag, String.format(msgFormat, args));
        }
    }

    static public void v(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL.unimportance()<=Log.Level.VERBOSE.unimportance())
        {
            write(tag, String.format(msgFormat, args));
            printStack(tag, t); 
        }
    }

    static public void i(String tag, String msgFormat, Object...args)
    {
        if (LEVEL.unimportance()<=Log.Level.INFO.unimportance())
        {
        	write(tag, String.format(msgFormat, args));
        }
    }

    static public void i(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL.unimportance()<=Log.Level.INFO.unimportance())
        {
        	write(tag, String.format(msgFormat, args));
            printStack(tag, t); 
        }
    }

    static public void e(String tag, String msgFormat, Object...args)
    {
        if (LEVEL.unimportance()<=Log.Level.ERROR.unimportance())
        {
            write(tag, String.format(msgFormat, args));
        }
    }

    static public void e(Runnable pIf, Runnable pElse)
    {
        if (LEVEL.unimportance()<=Log.Level.ERROR.unimportance()) {
        	pIf.run();
        } else {
        	pElse.run();
        }
    }

    static public void i(Runnable pIf, Runnable pElse)
    {
        if (LEVEL.unimportance()<=Log.Level.INFO.unimportance()) {
        	pIf.run();
        } else {
        	pElse.run();
        }
    }

    static public void v(Runnable pIf, Runnable pElse)
    {
        if (LEVEL.unimportance()<=Log.Level.VERBOSE.unimportance()) {
        	pIf.run();
        } else {
        	pElse.run();
        }
    }

    static public void e(String tag, Throwable t)
    {
        if (LEVEL.unimportance()<=Log.Level.ERROR.unimportance())
        {
            printStack(tag, t); 
        }
    }

    static public void i(String tag, Throwable t)
    {
        if (LEVEL.unimportance()<=Log.Level.INFO.unimportance())
        {
            printStack(tag, t); 
        }
    }

    static public void eStack(String tag, String msgFormat, Object...args)
    {
        if (LEVEL.unimportance()<=Log.Level.ERROR.unimportance())
        {
            write(tag, String.format(msgFormat, args));
            try {
            	throw new Exception();
            } catch(Exception t) {
            	printStack(tag, t);
            }
        }
    }

    static public void e(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL.unimportance()<=Log.Level.ERROR.unimportance())
        {
            write(tag, String.format(msgFormat, args));
            printStack(tag, t); 
        }
    }

}
