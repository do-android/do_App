package doext.app;
import android.content.Context;
import core.interfaces.DoIAppDelegate;

/**
 * APP启动的时候会执行onCreate方法；
 *
 */
public class do_App_App implements DoIAppDelegate {

	private static do_App_App instance;
	
	private do_App_App(){
		
	}
	
	public static do_App_App getInstance() {
		if(instance == null){
			instance = new do_App_App();
		}
		return instance;
	}
	
	@Override
	public void onCreate(Context context) {
		// ...do something
	}
	
	@Override
	public String getTypeID() {
		return "do_App";
	}
}
