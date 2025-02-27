package cordova.plugin.codeplay.in.app.update;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.IntentSender;
import android.widget.Toast;
import android.support.design.widget.Snackbar;
import android.widget.FrameLayout;
import android.util.Log;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.tasks.Task;


public class inappupdate extends CordovaPlugin {

	public int REQUEST_CODE = 7;

    private static String IN_APP_UPDATE_TYPE = "FLEXIBLE";
	private static boolean isUpdateDownloaded = false;
	private static AppUpdateManager appUpdateManager;
    private static InstallStateUpdatedListener listener;
	private static Context testParameter;

	private CallbackContext _callbackContex;
	private FrameLayout layout;

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
            super.initialize(cordova, webView);
            layout = (FrameLayout) webView.getView().getParent();
    }

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		_callbackContex = callbackContext;
		testParameter = (cordova.getActivity()).getBaseContext();
		appUpdateManager = AppUpdateManagerFactory.create(testParameter);
		Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

		if (action.equals("isUpdateAvailable"))
		{
			if (isUpdateDownloaded){
				 popupSnackbarForCompleteUpdate();
			} else {
				appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
					if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
							// For a flexible update, use AppUpdateType.FLEXIBLE
							&& appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
					{
						//Toast.makeText(testParameter, "Flexible update ready", Toast.LENGTH_LONG).show();
						callbackContext.success("true");
					} else {
						callbackContext.success("false");
						//Toast.makeText(testParameter, "No update available", Toast.LENGTH_LONG).show();
					}
				});
			}

			return true;
		}

		if (action.equals("update"))
		{
			String updateType = args.getString(0);

			if(updateType.equals("IMMEDIATE"))
			{
				IN_APP_UPDATE_TYPE = "IMMEDIATE";
			}
			//Toast.makeText(testParameter, "Update application....", Toast.LENGTH_LONG).show();

			try {
				appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
				
				if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
				{
					 checkForUpdate(appUpdateInfo);
				}
			});
			}
			catch (final Exception e) {
				String str=e.getMessage();
				String stackTrace = Log.getStackTraceString(e);
				callbackContext.error(str+stackTrace);
				//Toast.makeText(testParameter, "Update error: "+str, Toast.LENGTH_LONG).show();
            }
		}

		return false;
	}

	public void checkForUpdate(final AppUpdateInfo appUpdateInfo) {
		int updateType = 0;
        if (IN_APP_UPDATE_TYPE.equals("FLEXIBLE")) {
                listener = state -> {
                        onStateUpdate(state);
                };
                appUpdateManager.registerListener(listener);
            } else {
				updateType = 1;
			}

            try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateType, cordova.getActivity(),
                                    REQUEST_CODE);
            } catch (final Exception e) {
				String str=e.getMessage();
				String stackTrace = Log.getStackTraceString(e);
				_callbackContex.error(str+stackTrace);
				//Toast.makeText(testParameter, "Update error: "+str, Toast.LENGTH_LONG).show();
        }
    }

    private void popupSnackbarForCompleteUpdate() {
        final Snackbar snackbar = Snackbar.make(layout, "Update has been downloaded.",
                            Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Update app", view -> appUpdateManager.completeUpdate());
        snackbar.show();
    }

	public void onStateUpdate(final InstallState state) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
			isUpdateDownloaded = true;
			//Toast.makeText(testParameter, "Update downloaded! ", Toast.LENGTH_LONG).show();
			popupSnackbarForCompleteUpdate();
        } else if (state.installStatus() == InstallStatus.CANCELED){
			Toast.makeText(testParameter, "Update app canceled ", Toast.LENGTH_LONG).show();
		} else if (state.installStatus() == InstallStatus.FAILED){
			Toast.makeText(testParameter, "Update app failed ", Toast.LENGTH_LONG).show();
		}
    };

 	@Override
	public void onResume(final boolean multitasking) {
        super.onResume(multitasking);
        appUpdateManager
            .getAppUpdateInfo()
            .addOnSuccessListener(
                appUpdateInfo -> {
                    if (IN_APP_UPDATE_TYPE.equals("FLEXIBLE") && appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate();
                    }

                    if (appUpdateInfo.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        // If an in-app update is already running, resume the update.
                        try {
                            checkForUpdate(appUpdateInfo);
                        } catch (Exception e) {
							String str=e.getMessage();
							String stackTrace = Log.getStackTraceString(e);
							_callbackContex.error(str+stackTrace);
							//Toast.makeText(testParameter, "Update error: "+str, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
