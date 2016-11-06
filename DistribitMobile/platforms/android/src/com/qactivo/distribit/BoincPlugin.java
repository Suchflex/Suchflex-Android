package com.qactivo.distribit;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.qactivo.distribit.boinc.attach.ProjectAttachService;
import com.qactivo.distribit.boinc.attach.ProjectAttachService.ProjectAttachWrapper;
import com.qactivo.distribit.boinc.client.IMonitor;
import com.qactivo.distribit.boinc.client.Monitor;
import com.qactivo.distribit.boinc.rpc.AccountIn;
import com.qactivo.distribit.boinc.rpc.AccountOut;
import com.qactivo.distribit.boinc.rpc.AcctMgrInfo;
import com.qactivo.distribit.boinc.rpc.GlobalPreferences;
import com.qactivo.distribit.boinc.rpc.ProjectConfig;
import com.qactivo.distribit.boinc.rpc.ProjectInfo;
import com.qactivo.distribit.boinc.rpc.Result;
import com.qactivo.distribit.boinc.utils.BOINCDefs;
import com.qactivo.distribit.boinc.utils.BOINCErrors;
import com.qactivo.distribit.boinc.utils.Logging;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import static com.qactivo.distribit.Action.List;

/**
 * BoincPlugin by Cristian Tardivo
 */
public class BoincPlugin extends CordovaPlugin {

    // Main App activity
    private Activity activity;

    // Plugin Methods mapped to action
    private HashMap<Action.List, Method> methodsMap;

    /**
     * Boinc Services
     */
    private ProjectAttachService attachService;
    private IMonitor monitor;
    private Boolean mIsBound = false;
    private boolean asIsBound = false;

    /**
     * Monitor service connection
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been established, getService returns
            // the Monitor object that is needed to call functions.
            monitor = IMonitor.Stub.asInterface(service);
            mIsBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This should not happen
            monitor = null;
            mIsBound = false;

            Log.e(Logging.TAG, "BOINCActivity onServiceDisconnected");
        }
    };

    /**
     * Project Attach service connection
     */
    private ServiceConnection mASConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been established, getService returns
            // the Monitor object that is needed to call functions.
            attachService = ((ProjectAttachService.LocalBinder) service).getService();
            asIsBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This should not happen
            attachService = null;
            asIsBound = false;
        }
    };

    /**
     * Bind Services
     */
    private void doBindServices() {
        // start service to allow setForeground later on...
        activity.startService(new Intent(activity, Monitor.class));
        //startService(new Intent(this, ProjectAttachService.class));
        // Establish a connection with the service, onServiceConnected gets called when
        activity.bindService(new Intent(activity, Monitor.class), mConnection, Service.BIND_AUTO_CREATE);
        activity.bindService(new Intent(activity, ProjectAttachService.class), mASConnection, Service.BIND_AUTO_CREATE);
    }

    /**
     * Unbind Services
     */
    private void doUnbindServices() {
        if (mIsBound) {
            // Detach existing connection.
            activity.unbindService(mConnection);
            mIsBound = false;
        }
        if (asIsBound) {
            // Detach existing connection.
            activity.unbindService(mASConnection);
            asIsBound = false;
        }
    }

    @Override
    public void pluginInitialize() {
        activity = this.cordova.getActivity();
        // bind boinc services
        doBindServices();
        // init methods map
        initMethodsMap();
    }

    /**
     * Init Methods map to action
     */
    private void initMethodsMap() {
        methodsMap = new HashMap<>();
        final Class controller = this.getClass();
        final Method[] methods = controller.getMethods();
        for (Method method : methods) {
            final Action annotation = method.getAnnotation(Action.class);
            if (annotation != null) {
                methodsMap.put(annotation.value(), method);
            }
        }
    }

    /**
     * Execute action
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments. like an array with a object [{object}]
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return
     * @throws JSONException
     */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final BoincPlugin plugin = this;
        final JSONObject parameters = args.getJSONObject(0);
        // Get and invoke method action
        final Method method = methodsMap.get(List.valueOf(action));
        if (method != null) {
            // Invoke method in a thread pool to avoid cordova main thread blocked
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        method.invoke(plugin, parameters, callbackContext);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            });
            // Return valid action
            return true;
        }
        // Invalid action
        return false;
    }

    /**
     * Check whether device is online before starting connection attempt
     * as needed for AttachProjectLoginActivity (retrieval of ProjectConfig)
     * note: available internet does not guarantee connection to project server is possible!
     *
     * @return callback success: true/false
     */
    @Action(List.CHECK_DEVICE_ONLINE)
    public void checkDeviceOnline(final JSONObject args, final CallbackContext callbackContext) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        final Boolean online = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        if (!online) {
            if (Logging.DEBUG) Log.d(Logging.TAG, "not online, stop!");
        }
        callbackContext.success(online.toString());
    }

    /**
     * Attempts attach of account manager with credentials provided as parameter.
     * Does not require to select project or set credentials beforehand.
     *
     * @param args json array  url name pwd
     *             url: acct mgr url
     *             name: user name
     *             pwd: password
     * @return callback success if ok, error if an error occurred (string error description)
     *
     * Dettach: monitor.addAcctMgrErrorNum("", "", "") == BOINCErrors.ERR_OK;
     */
    @Action(List.ATTACH_ACC_MGR)
    public void attachAcctMgr(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        final int res = attachService.attachAcctMgr(args.getString("url"), args.getString("name"), args.getString("pwd"));
        if (res == BOINCErrors.ERR_OK)
            callbackContext.success();
        else
            callbackContext.error(attachService.mapErrorNumToString(res));
    }

    /**
     * Creates account for given user information and returns account credentials if successful.
     *
     * @param args json array  url email userName pwd teamName
     *             url: master URL of project
     *             email: email address of user
     *             userName: user name of user
     *             pwd: password
     *             teamName: name of team, account shall get associated to
     * @return callback success if ok (with auth string), error if and error occurred (string error description)
     */
    @Action(List.CREATE_ACCOUNT_POLLING)
    public void createAccountPolling(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final AccountIn information = new AccountIn(args.getString("url"), args.getString("email"), args.getString("userName"), false, args.getString("pwd"), args.getString("teamName"));
            final AccountOut accountPolling = monitor.createAccountPolling(information);
            if (accountPolling.error_num != BOINCErrors.ERR_OK)
                callbackContext.error(accountPolling.error_msg);
            else
                callbackContext.success(accountPolling.authenticator);
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Attaches project, requires authenticator
     *
     * @param args json array  url projectName authenticator
     *             url: URL of project to be attached, either masterUrl(HTTP) or webRpcUrlBase(HTTPS)
     *             projectName: name of project as shown in the manager
     *             authenticator: user authentication key, has to be obtained first
     * @return callback success if project attached, error if an error occurred (string with las error message)
     */
    @Action(List.ATTACH_PROJECT)
    public void attachProject(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            // Get data
            final boolean success = monitor.attachProject(args.getString("url"), args.getString("projectName"), args.getString("authenticator"));
            if (success)
                callbackContext.success("Project attached");
            else
                callbackContext.error(monitor.getLastErrorMessage());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Checks whether project of given master URL is currently attached to BOINC client
     *
     * @param args json array url
     *             url: master URL of the project
     * @return callback success with true/false if project attached or not, error if an error occurred
     */
    @Action(List.CHECK_PROJECT_ATTACHED)
    public void checkProjectAttached(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Boolean attached = monitor.checkProjectAttached(args.getString("url"));
            callbackContext.success(attached.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * sets single selected project with URL inserted manually, not chosen from list.
     * Starts configuration download in new thread and returns immediately.
     * Check projectConfigRetrievalFinished to see whether job finished.
     *
     * @param args json array url
     *             url: master URL of the project
     * @return callback success with true/false if project was set selected or not
     */
    @Action(List.SET_MANUALLY_SELECTED_PROJECT)
    public void setManuallySelectedProject(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        final Boolean success = attachService.setManuallySelectedProject(args.getString("url"));
        callbackContext.success(success.toString());
    }

    /**
     * Returns selected but untried project to be attached
     *
     * @return callback success with project attachwrapper json, error if an error ocurred or null project
     */
    @Action(List.GET_NEXT_SELECTED_PROJECT)
    public void getNextSelectedProject(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        final ProjectAttachWrapper project = attachService.getNextSelectedProject();
        final Gson gson = new Gson();
        if (project != null) {
            callbackContext.success(gson.toJson(project));
        } else {
            callbackContext.error("Cant get next selected project");
        }
    }

    /**
     * Returns true as long as there have been unresolved conflicts.
     * <p>
     * To resolve try:
     * attachService.setCredentials(email, name, pwd);
     * project.lookupAndAttach(login);
     *
     * @return callback success true/false indicator whether conflicts exist
     */
    @Action(List.HAS_UNRESOLVED_CONFLICTS)
    public void hasUnresolvedConflicts(final JSONObject args, final CallbackContext callbackContext) {
        final Boolean success = attachService.unresolvedConflicts();
        callbackContext.success(success.toString());
    }

    /**
     * Get a list of manual selected projects
     *
     * @return callback success with a list of manual selected projects (project attach wrapper json)
     */
    @Action(List.GET_SELECTED_PROJECTS)
    public void getSelectedProjects(final JSONObject args, final CallbackContext callbackContext) {
        final ArrayList<ProjectAttachWrapper> selectedProjects = attachService.getSelectedProjects();
        final Gson gson = new Gson();
        callbackContext.success(gson.toJson(selectedProjects));
    }

    /**
     * Get the number of manual selected projects
     *
     * @return callback success number of manual selected projects
     */
    @Action(List.GET_NUMBER_SELECTED_PROJECTS)
    public void getNumberSelectedProjects(final JSONObject args, final CallbackContext callbackContext) {
        callbackContext.success(attachService.getNumberSelectedProjects());
    }

    /**
     * Returns last user input to be able to pre-populate fields.
     *
     * @return callback success a json object with email and name
     */
    @Action(List.GET_USER_DEFAULT_VALUES)
    public void getUserDefaultValues(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        final ArrayList<String> values = attachService.getUserDefaultValues();
        callbackContext.success(new JSONObject().put("email", values.get(0)).put("name", values.get(1)));
    }

    /**
     * Set credentials to be used in account RPCs.
     * Set / update prior to calling attach or register
     * Saves email and user persistently to pre-populate fields
     *
     * @param args json array email user pwd
     *             email: user email
     *             user: username
     *             pwd: password
     * @return callback success if credentials was set
     */
    @Action(List.SET_CREDENTIALS)
    public void setCredentials(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        attachService.setCredentials(args.getString("email"), args.getString("user"), args.getString("pwd"));
        callbackContext.success();
    }

    /**
     * Attempts account lookup with the credentials previously set in service. (Previous selected project)
     * <p>
     * Retries in case of non-deterministic errors
     * Long-running and network communication, do not execute in UI thread.
     *
     * @return callback success auth string key, error if and error occurred (with error string info)
     */
    @Action(List.LOGIN)
    public void login(final JSONObject args, final CallbackContext callbackContext) {
        final ProjectAttachWrapper project = attachService.getNextSelectedProject();
        if (project == null) {
            callbackContext.error("No selected project");
            return;
        }
        final AccountOut login = project.login();
        //
        if (login.error_num != BOINCErrors.ERR_OK) {
            String result;
            switch (login.error_num) {
                case BOINCErrors.ERR_DB_NOT_UNIQUE:
                    result = activity.getString(R.string.attachproject_conflict_not_unique);
                    break;
                case BOINCErrors.ERR_BAD_PASSWD:
                    result = activity.getString(R.string.attachproject_conflict_bad_password);
                    break;
                case BOINCErrors.ERR_DB_NOT_FOUND:
                    if (project.config.clientAccountCreationDisabled) {
                        result = activity.getString(R.string.attachproject_conflict_unknown_user_creation_disabled);
                    } else {
                        result = activity.getString(R.string.attachproject_conflict_unknown_user);
                    }
                    break;
                default:
                    result = activity.getString(R.string.attachproject_conflict_undefined);
            }
            callbackContext.error(result);
        } else {
            callbackContext.success(login.authenticator);
        }
    }

    /**
     * Attempts account registration with the credentials previously set in service.
     * Registration also succeeds if account exists and password is correct.
     * <p>
     * Retries in case of non-deterministic errors
     * Long-running and network communication, do not execute in UI thread.
     *
     * @return callback success auth string key, error if and error occurred (with error string info)
     */
    @Action(List.REGISTER)
    public void register(final JSONObject args, final CallbackContext callbackContext) {
        final ProjectAttachWrapper project = attachService.getNextSelectedProject();
        if (project == null) {
            callbackContext.error("No selected project");
            return;
        }
        final AccountOut register = project.register();
        //
        if (register.error_num != BOINCErrors.ERR_OK) {
            String result;
            switch (register.error_num) {
                case BOINCErrors.ERR_DB_NOT_UNIQUE:
                    result = activity.getString(R.string.attachproject_conflict_not_unique);
                    break;
                case BOINCErrors.ERR_BAD_PASSWD:
                    result = activity.getString(R.string.attachproject_conflict_bad_password);
                    break;
                case BOINCErrors.ERR_DB_NOT_FOUND:
                    if (project.config.clientAccountCreationDisabled) {
                        result = activity.getString(R.string.attachproject_conflict_unknown_user_creation_disabled);
                    } else {
                        result = activity.getString(R.string.attachproject_conflict_unknown_user);
                    }
                    break;
                default:
                    result = activity.getString(R.string.attachproject_conflict_undefined);
            }
            callbackContext.error(result);
        } else {
            callbackContext.success(register.authenticator);
        }
    }

    /**
     * Attaches the previous manually selected project to BOINC client.
     * Account lookup/registration using credentials set at service.
     * <p>
     * Using registration RPC if client side registration is enabled,
     * succeeds also if account exists and password is correct.
     * <p>
     * Using login RPC if client side registration is disabled.
     * <p>
     * Attaches project if account lookup succeeded.
     * <p>
     * Retries in case of non-deterministic errors
     * Long-running and network communication, do not execute in UI thread.
     *
     * @return callback success returns status conflict
     */
    @Action(List.LOOKUP_AND_ATTACH)
    public void lookupAndAttach(final JSONObject args, final CallbackContext callbackContext) {
        final ProjectAttachWrapper project = attachService.getNextSelectedProject();
        if (project == null) {
            callbackContext.error("No selected project");
            return;
        }
        callbackContext.success(project.lookupAndAttach(true)); // return 3 if success
    }

    /**
     * sets selected projects and downloads their configuration files
     * configuration download in new thread, returns immediately.
     * Check projectConfigRetrievalFinished to see whether job finished.
     *
     * @param args json array projects  {projects: [{name, url, generalArea, specificArea, description, home, {platform, platform}, imageUr, summary},..]}
     * @return callback success true/false if projects selected
     */
    @Action(List.SET_SELECTED_PROJECTS)
    public void setSelectedProjects(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        final Gson gson = new Gson();
        final ArrayList<ProjectInfo> projects = new ArrayList<>();
        final JSONArray data = args.getJSONArray("projects");
        for (int i = 0; i < data.length(); i++) {
            gson.fromJson(data.getJSONObject(i).toString(), ProjectInfo.class);
        }
        Boolean success = attachService.setSelectedProjects(projects);
        callbackContext.success(success.toString());
    }


    /**
     * Check if project config retrieval has finished before execute other action
     *
     * @return callback success true/false if project config retrieval has finished or not
     */
    @Action(List.PROJECT_CONFIG_RETRIEVAL_FINISHED)
    public void hasProjectConfigRetrievalFinished(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        final Boolean result = attachService.projectConfigRetrievalFinished;
        callbackContext.success(result.toString());
    }

    /**
     * Get all current projects
     *
     * @return callback success json string with current projects info, error if an error occurred
     */
    @Action(List.GET_PROJECTS)
    public void getProjects(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getProjects()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Set client run mode
     *
     * @param args json array mode
     *             mode BOINCDefs.RUN_MODE_NEVER = 3 | BOINCDefs.RUN_MODE_AUTO = 2
     * @return callback success true/false if mode was set, error if an error occurred
     */
    @Action(List.SET_RUN_MODE)
    public void setRunMode(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Boolean success = monitor.setRunMode(args.getInt("mode"));
            callbackContext.success(success.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Set client network mode
     *
     * @param args json array mode
     *             mode BOINCDefs.RUN_MODE_NEVER = 3 | BOINCDefs.RUN_MODE_AUTO = 2
     * @return callback success true/false if mode was set, error if an error occurred
     */
    @Action(List.SET_NETWORK_MODE)
    public void setNetworkMode(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Boolean success = monitor.setNetworkMode(args.getInt("mode"));
            callbackContext.success(success.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get client computing status
     *
     * @return callback success current computing Numeric status, error if an error occurred
     */
    @Action(List.GET_COMPUTING_STATUS)
    public void getComputingStatus(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(monitor.getComputingStatus());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get client preferences
     *
     * @return callback success current client preferences json String
     */
    @Action(List.GET_CLIENT_PREFS)
    public void getClientPrefs(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getPrefs()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Set Client preferences
     *
     * @param args json array GlobalPreferences json object
     * @return callback success true/false if preferences was set, error if an error occurred
     */
    @Action(List.SET_CLIENT_PREFS)
    public void setClientPrefs(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            final Boolean success = monitor.setGlobalPreferences(gson.fromJson(args.toString(), GlobalPreferences.class));
            callbackContext.success(success.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get Host info
     *
     * @return callback success HostInfo json String, error if an error occurred
     */
    @Action(List.GET_HOST_INFO)
    public void getHostInfo(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getHostInfo()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get project info from all_projects_list.xml
     *
     * @param args json object with url data
     * @return callback success ProjectInfo json String, error if an error occurred
     */
    @Action(List.GET_PROJECT_INFO)
    public void getProjectInfo(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getProjectInfo(args.getString("url"))));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Triggers change of state of project in BOINC core client
     *
     * @param args json object operation projectUrl
     *             operation operation to be triggered RpcClient.PROJECT_UPDATE | PROJECT_SUSPEND | PROJECT_RESUME | PROJECT_NNW | PROJECT_ANW | PROJECT_DETACH | PROJECT_RESET
     *             projectUrl master URL of project
     * @return callback success true/false if operation was done, error if an error occurred
     */
    @Action(List.PROJECT_OP)
    public void projectOp(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Boolean success = monitor.projectOp(args.getInt("operation"), args.getString("projectUrl"));
            callbackContext.success(success.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Reads project configuration for specified master URL.
     *
     * @param args json object url
     *             url master URL of the project
     * @return callback success with ProjectConfig json String, error if an error occurred
     */
    @Action(List.GET_PROJECT_CONFIG_POLLING)
    public void getProjectConfigPolling(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            final ProjectConfig config = monitor.getProjectConfigPolling(args.getString("url"));
            callbackContext.success(gson.toJson(config));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get all transfers
     *
     * @return callback success with a list of Transfer json String, error if an error occurred
     */
    @Action(List.GET_TRANSFERS)
    public void getTransfers(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getTransfers()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get Server notices
     *
     * @return callback success with a list of server Notice json String, error if an error occurred
     */
    @Action(List.GET_SERVER_NOTICES)
    public void getServerNotices(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getServerNotices()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get projects tasks
     *
     * @return callback success with a list of Result json String, error if an error occurred (Result is a Task)
     */
    @Action(List.GET_TASKS)
    public void getTasks(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getTasks()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Triggers operation on task in BOINC core client
     *
     * @param args json object operation projectUrl resultName
     *             operation operation to be triggered RpcClient.RESULT_SUSPEND | RESULT_RESUME | RESULT_ABORT
     *             projectUrl master URL of project
     *             resultName name of the result (or task)
     * @return callback success true/false if result operation was done, error if an error occurred
     */
    @Action(List.RESULT_OP)
    public void resultOp(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Boolean success = monitor.resultOp(args.getInt("operation"), args.getString("projectUrl"), args.getString("resultName"));
            callbackContext.success(success.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Looks up account credentials for given user data.
     * Contains authentication key for project attachment.
     *
     * @param args json object url id
     *             url URL of project, either masterUrl(HTTP) or webRpcUrlBase(HTTPS)
     *             id user ID, can be either name or eMail, see usesName
     *             pwd password
     *             usesName if true, id represents a user name, if not, the user's email address
     *             teamName team name
     * @return callback success with account Authenticator string, error if an error occurred
     */
    @Action(List.LOOKUP_CREDENTIALS)
    public void lookupCredentials(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            final AccountOut accountOut = monitor.lookupCredentials(new AccountIn(args.getString("url"), args.getString("id"), args.getString("id"), args.getBoolean("usesName"), args.getString("pwd"), args.getString("teamName")));
            if (accountOut.error_num != BOINCErrors.ERR_OK)
                callbackContext.error(accountOut.error_msg);
            else
                callbackContext.success(accountOut.authenticator);
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get if app AutoStart if enabled or not
     *
     * @return callback success with true/false if autostart is enabled or not, error if an error occurred
     */
    @Action(List.GET_AUTOSTART)
    public void getAutostart(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Boolean success = monitor.getAutostart();
            callbackContext.success(success.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get client current status text description
     *
     * @return callback success with status description String, error if an error occurred
     */
    @Action(List.GET_CURRENT_STATUS_DESCRIPTION)
    public void getCurrentStatusDescription(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(monitor.getCurrentStatusDescription());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get client current status title description
     *
     * @return callback success with status title description String, error if an error occurred
     */
    @Action(List.GET_CURRENT_STATUS_TITLE)
    public void getCurrentStatusTitle(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(monitor.getCurrentStatusTitle());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get device battery charge level
     *
     * @return callback success with battery status, error if an error occurred
     */
    @Action(List.GET_BATTERY_CHARGE_STATUS)
    public void getBatteryChargeStatus(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(monitor.getBatteryChargeStatus());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get client computing suspend numeric reason
     *
     * @return callback success computing suspend reason number, error if an error occurred
     */
    @Action(List.GET_COMPUTING_SUSPEND_REASON)
    public void getComputingSuspendReason(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(monitor.getComputingSuspendReason());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get client network suspend numeric reason
     *
     * @return callback success network suspend reason number, error if an error occurred
     */
    @Action(List.GET_NETWORK_SUSPEND_REASON)
    public void getNetworkdSuspendReason(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(monitor.getNetworkSuspendReason());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get config, suspend computing when device screen is on
     *
     * @return callback success suspend status true/false, error if an error occurred
     */
    @Action(List.GET_SUSPEND_WHEN_SCREEN_ON)
    public void getSuspendWhenScreenOn(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(String.valueOf(monitor.getSuspendWhenScreenOn()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get config, computing power source AC
     * @return callback success computing power source AC true/false, error if an error occurred
     */
    @Action(List.GET_POWER_SOURCE_AC)
    public void getPowerSourceAc(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(String.valueOf(monitor.getPowerSourceAc()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get config, computing power source USB
     * @return callback success computing power source USB true/false, error if an error occurred
     */
    @Action(List.GET_POWER_SOURCE_USB)
    public void getPowerSourceUsb(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(String.valueOf(monitor.getPowerSourceUsb()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get config, computing power source Wireless
     * @return callback success computing power source Wireless true/false, error if an error occurred
     */
    @Action(List.GET_POWER_SOURCE_WIRELESS)
    public void getPowerSourceWireless(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(String.valueOf(monitor.getPowerSourceWireless()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Set app autostrat on/off
     * @param args status
     *          status true/false for autostart
     * @return callback success when autostart set, error if an error occurred
     */
    @Action(List.SET_AUTOSTART)
    public void setAutostart(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            monitor.setAutostart(args.getBoolean("status"));
            callbackContext.success();
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Set suspend when screen is on enabled/disabled
     * @param args status
     *          status true/false
     * @return callback success when suspend when screen is on set, error if an error occurred
     */
    @Action(List.SET_SUSPEND_WHEN_SCREEN_ON)
    public void setSuspendWhenScreenOn(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            monitor.setSuspendWhenScreenOn(args.getBoolean("status"));
            callbackContext.success();
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Set power source ac enabled/disabled
     * @param args status
     *          status true/false
     * @return callback success when computing power source AC set, error if an error occurred
     */
    @Action(List.SET_POWER_SOURCE_AC)
    public void setPowerSourceAc(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            monitor.setPowerSourceAc(args.getBoolean("status"));
            callbackContext.success();
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Set power source usb enabled/disabled
     * @param args status
     *          status true/false
     * @return callback success when computing power source USB set, error if an error occurred
     */
    @Action(List.SET_POWER_SOURCE_USB)
    public void setPowerSourceUsb(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            monitor.setPowerSourceUsb(args.getBoolean("status"));
            callbackContext.success();
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Set power source wireless enabled/disabled
     * @param args status
     *          status true/false
     * @return callback success when computing power source WIRELESS set, error if an error occurred
     */
    @Action(List.SET_POWER_SOURCE_WIRELESS)
    public void setPowerSourceWireless(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            monitor.setPowerSourceWireless(args.getBoolean("status"));
            callbackContext.success();
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Run client benchmarks
     * @return callback success true/false if benchmarks runs, error if an error occurred
     */
    @Action(List.RUN_BENCHMARKS)
    public void runBenchmarks(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Boolean success = monitor.runBenchmarks();
            callbackContext.success(success.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Force client refresh
     * @return callback success when force refresh
     */
    @Action(List.FORCE_REFRESH)
    public void forceRefresh(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            monitor.forceRefresh();
            callbackContext.success();
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Indicates whether service was able to obtain BOINC mutex.
     * If not, BOINC has not started and all other calls will fail.
     * @return callback success BOINC mutex acquisition successful (true/false), error if an error occurred
     */
    @Action(List.BOINC_MUTEX_ACQUIRED)
    public void boincMutexAcquired(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Boolean success = monitor.boincMutexAcquired();
            callbackContext.success(success.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get project status
     * @param args json url
     *          url master project url
     * @return callback success project status string, error if an error occurred
     */
    @Action(List.GET_PROJECT_STATUS)
    public void getProjectStatus(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            callbackContext.success(monitor.getProjectStatus(args.getString("url")));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get attachable projects for current device and status
     * @return callback success with a list of ProjectInfo json String (attachable projects), error if an error occurred
     */
    @Action(List.GET_ATTACHABLE_PROJECTS)
    public void getAttachableProjects(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getAttachableProjects()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get Result/Task current status text
     * @param args result
     *       result: result/task json object
     * @return callback success with task current status text
     */
    @Action(List.DETERMINE_RESULT_STATUS_TEXT)
    public void determineResultStatusText(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        final Gson gson = new Gson();
        final Result result = gson.fromJson(args.toString(), Result.class);
        String text = null;

        Integer status;
        if(result.suspended_via_gui) status = BOINCDefs.RESULT_SUSPENDED_VIA_GUI;
        if(result.project_suspended_via_gui) status = BOINCDefs.RESULT_PROJECT_SUSPENDED;
        if(result.ready_to_report && result.state != BOINCDefs.RESULT_ABORTED && result.state != BOINCDefs.RESULT_COMPUTE_ERROR) status = BOINCDefs.RESULT_READY_TO_REPORT;
        if(result.active_task){
            status = result.active_task_state;
        } else {
            status = result.state;
        }

        // custom state
        if(status == BOINCDefs.RESULT_SUSPENDED_VIA_GUI)
            text = activity.getString(R.string.tasks_custom_suspended_via_gui);
        else if(status == BOINCDefs.RESULT_PROJECT_SUSPENDED)
            text = activity.getString(R.string.tasks_custom_project_suspended_via_gui);
        else if(status == BOINCDefs.RESULT_READY_TO_REPORT)
            text = activity.getString(R.string.tasks_custom_ready_to_report);
        // Return result text status if necessary
        if(text != null)
            callbackContext.success(text);

        //active state
        if(result.active_task) {
            switch(status) {
                case BOINCDefs.PROCESS_UNINITIALIZED:
                    text = activity.getString(R.string.tasks_active_uninitialized);
                    break;
                case BOINCDefs.PROCESS_EXECUTING:
                    text = activity.getString(R.string.tasks_active_executing);
                    break;
                case BOINCDefs.PROCESS_ABORT_PENDING:
                    text = activity.getString(R.string.tasks_active_abort_pending);
                    break;
                case BOINCDefs.PROCESS_QUIT_PENDING:
                    text = activity.getString(R.string.tasks_active_quit_pending);
                    break;
                case BOINCDefs.PROCESS_SUSPENDED:
                    text = activity.getString(R.string.tasks_active_suspended);
                    break;
                default:
                    text = "";
            }
        } else {
            // passive state
            switch(status) {
                case BOINCDefs.RESULT_NEW:
                    text = activity.getString(R.string.tasks_result_new);
                    break;
                case BOINCDefs.RESULT_FILES_DOWNLOADING:
                    text = activity.getString(R.string.tasks_result_files_downloading);
                    break;
                case BOINCDefs.RESULT_FILES_DOWNLOADED:
                    text = activity.getString(R.string.tasks_result_files_downloaded);
                    break;
                case BOINCDefs.RESULT_COMPUTE_ERROR:
                    text = activity.getString(R.string.tasks_result_compute_error);
                    break;
                case BOINCDefs.RESULT_FILES_UPLOADING:
                    text = activity.getString(R.string.tasks_result_files_uploading);
                    break;
                case BOINCDefs.RESULT_FILES_UPLOADED:
                    text = activity.getString(R.string.tasks_result_files_uploaded);
                    break;
                case BOINCDefs.RESULT_ABORTED:
                    text = activity.getString(R.string.tasks_result_aborted);
                    break;
                case BOINCDefs.RESULT_UPLOAD_FAILED:
                    text = activity.getString(R.string.tasks_result_upload_failed);
                    break;
                default:
                    text = "";
            }
        }
        // Return result text status
        callbackContext.success(text);
    }

    /**
     * Synchronize an Account Manager
     * @param args json url
     *          url master account manager url
     * @return callback success true/false if synchronized, error if an error occurred
     */
    @Action(List.SYNCHRONIZE_ACCT_MGR)
    public void synchronizeAcctMgr(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Boolean success = monitor.synchronizeAcctMgr(args.getString("url"));
            callbackContext.success(success.toString());
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get account manager info
     * @return callback success account manager info, error if an error occurred
     */
    @Action(List.GET_ACCT_MGR_INFO)
    public void getAcctMgrInfo(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getAcctMgrInfo()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }

    /**
     * Get client account manager info
     * @return callback success client account manager info, error if an error occurred
     */
    @Action(List.GET_CLIENT_ACCT_MGR_INFO)
    public void getClientAcctMgrInfo(final JSONObject args, final CallbackContext callbackContext) throws JSONException {
        try {
            final Gson gson = new Gson();
            callbackContext.success(gson.toJson(monitor.getClientAcctMgrInfo()));
        } catch (RemoteException e) {
            callbackContext.error("Remote Exception");
        }
    }
}
