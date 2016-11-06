var boincPlugin = {

     /**
     * Check whether device is online before starting connection attempt
     * as needed for AttachProjectLoginActivity (retrieval of ProjectConfig)
     * note: available internet does not guarantee connection to project server is possible!
     * @return success with true or false (online / no-online)
     */
    checkDeviceOnline: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback, // success callback function
            errorCallback, // error callback function
            'BoincPlugin', // mapped to our native Java class called "BoincPlugin"
            'CHECK_DEVICE_ONLINE', // with this action name
            [{ }] // and this array of custom arguments to create our entry, always declare call parameters (inclusive if are empty)
        );
    },

    /**
     * Attempts attach of account manager with credentials provided as parameter.
     * Does not require to select project or set credentials beforehand.
     * @param url: acct mgr url
     * @param name: user name
     * @param pwd: password
     * @return succes: if all ok
     * @return error: with error string if an error ocurred
     */
    attachAcctMgr: function(url, name, pwd, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'ATTACH_ACC_MGR',
            [{'url': url, 'name': name, 'pwd': pwd}]
        );
    },

    /**
     * Creates account for given user information and returns account credentials if successful.
     * @param url: master URL of project
     * @param email: email address of user
     * @param userName: user name of user
     * @param pwd: password
     * @param teamName: name of team, account shall get associated to
     * @return succes: account credentials (auth code)
     * @return error: error string if an error ocurred
     */
    createAccountPolling: function(url, email, userName, pwd, teamName, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'CREATE_ACCOUNT_POLLING',
            [{'url': url, 'email': email, 'userName': userName, 'pwd': pwd, 'teamName': teamName}]
        );
    },

    /**
     * Attaches project, requires authenticator
     * @param url: URL of project to be attached, either masterUrl(HTTP) or webRpcUrlBase(HTTPS)
     * @param projectName: name of project as shown in the manager
     * @param authenticator: user authentication key, has to be obtained first
     * @return success: if all ok
     * @return error: last error message if an error ocurred
     */
    attachProject: function(url, projectName, authenticator, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'ATTACH_PROJECT',
            [{'url': url, 'projectName': projectName, 'authenticator': authenticator}]
        );
    },
    
    /**
     * Checks whether project of given master URL is currently attached to BOINC client
     * @param url: master URL of the project
     * @return success: true if attached, false if no attached
     * @return error: if and error/exception ocurred
     */
    checkProjectAttached: function(url, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'CHECK_PROJECT_ATTACHED',
            [{'url': url}]
        );
    },

    /**
     * sets single selected project with URL inserted manually, not chosen from list.
     * Starts configuration download in new thread and returns immediately.
     * Check projectConfigRetrievalFinished to see whether job finished.
     * @param url: master URL of the project 
     * @return success: true or false if project has selected correctly
     */
    setManuallySelectedProject: function(url, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_MANUALLY_SELECTED_PROJECT',
            [{'url': url}]
        );
    },

    /**
     * Returns selected but untried project to be attached
     * @return success: project  (ProjectAttachWrapper json)
     * @return error: if no more untried projects (can't get next selected project)
     */
    getNextSelectedProject: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_NEXT_SELECTED_PROJECT',
            [{ }]
        );
    },

    /**
     * Returns true as long as there have been unresolved conflicts.
     *
     *  To resolve try:
     *      attachService.setCredentials(email, name, pwd);
     *      project.lookupAndAttach(login);
     *
     * @return succes: true or false, indicator whether conflicts exist
     */
    hasUnresolvedConflicts: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'HAS_UNRESOLVED_CONFLICTS',
            [{ }]
        );
    },

    /**
     * Get a list of manual selected projects
     * @return success: a list of ProjectAttachWrapper {url, ProjectInfo, name, ProjectConfig, result}
     */
    getSelectedProjects: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_SELECTED_PROJECTS',
            [{ }]
        );
    },

    /**
     * Get the number of manual selected projects
     * @return success: number of manual selected projects
     */
    getNumberSelectedProjects: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_NUMBER_SELECTED_PROJECTS',
            [{ }]
        );
    },

    /**
     * Returns last user input to be able to pre-populate fields.
     * @return success: array of values, index 0: email address, index 1: user name
     */
    getUserDefaultValues: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_USER_DEFAULT_VALUES',
            [{ }]
        );
    },
    
    /**
     * Set credentials to be used in account RPCs.
     * Set / update prior to calling attach or register
     * Saves email and user persistently to pre-populate fields
     * @param email: user email
     * @param user: username
     * @param pwd: password
     * @return success: nothing
     */
    setCredentials: function(email, user, pwd, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_CREDENTIALS',
            [{'email': email, 'user': user, 'pwd': pwd}]
        );
    },

    /**
     * Attempts account lookup with the credentials previously set in service. 
     * (Previous selected project)
     *
     * Retries in case of non-deterministic errors
     * Long-running and network communication, do not execute in UI thread.
     *
     * @return success: authenticator credentials
     * @return error: error message if an error ocurred
     */
    login: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'LOGIN',
            [{ }]
        );
    },

    /**
     * Attempts account registration with the credentials previously set in service.
     * Registration also succeeds if account exists and password is correct.
     * (Previous selected project)
     *
     * Retries in case of non-deterministic errors
     * Long-running and network communication, do not execute in UI thread.
     * @return success: authenticator credentials
     * @return error: error message if an error ocurred
     */
    register: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'REGISTER',
            [{ }]
        );
    },

    /**
     * Previous Select Project
     * Attaches this project to BOINC client.
     * Account lookup/registration using credentials set at service.
     *
     * Using registration RPC if client side registration is enabled,
     * succeeds also if account exists and password is correct.
     *
     * Using login RPC if client side registration is disabled.
     *
     * Attaches project if account lookup succeeded.
     *
     * Retries in case of non-deterministic errors
     * Long-running and network communication, do not execute in UI thread.
     * @return success: status conflict
     * @return error: if no selected project (use setManuallySelectedProject)
     */
    lookupAndAttach: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'LOOKUP_AND_ATTACH',
            [{ }]
        );
    },

    /**
     * sets selected projects and downloads their configuration files
     * configuration download in new thread, returns immediately.
     * Check projectConfigRetrievalFinished to see whether job finished.
     *
     * @param projects:  (list of ProjectInfo) ie. {projects: [{name, url, generalArea, specificArea, description, home, {platform, platform}, imageUr, summary},..]}
     * @return success: true/false if succes or not
     */
    setSelectedProjects: function(projects, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_SELECTED_PROJECTS',
            [{'projects': projects}]
        );
    },

    /**
     * Check if project config retrieval has finished before execute other action
     * @return success: true or false
     */
    hasProjectConfigRetrievalFinished: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'PROJECT_CONFIG_RETRIEVAL_FINISHED',
            [{ }]
        );
    },

    /**
     * Get all current projects
     * @return success: list of Project
     */
    getProjects: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_PROJECTS',
            [{ }]
        );
    },

    /**
     * Set client run mode
     * @param mode: BOINCDefs.RUN_MODE_NEVER = 3 | BOINCDefs.RUN_MODE_AUTO = 2
     * @return success: true/false if mode was setted
     * @return error: if an error ocurred
     */
    setRunMode: function(mode, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_RUN_MODE',
            [{'mode': mode}]
        );
    },

    /**
     * Set client network mode
     * @param mode: BOINCDefs.RUN_MODE_NEVER = 3 | BOINCDefs.RUN_MODE_AUTO = 2
     * @return success: true/false if mode was setted
     * @return error: if an error ocurred
     */
    setNetworkMode: function(mode, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_NETWORK_MODE',
            [{'mode': mode}]
        );
    },

    /**
     * Get client computing status
     * @return success: current comupting status
     * @return error: if an error ocurred
     */
    getComputingStatus: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_COMPUTING_STATUS',
            [{ }]
        );
    },

    /**
     * Get client preferences
     * @return success: current GlobalPreferences
     * @return error: if an error ocurred
     */
    getClientPrefs: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_CLIENT_PREFS',
            [{ }]
        );
    },

    /**
     * Set Client preferences
     * @param preferences GlobalPreferences json object
     * @return success: true/false if preferences was setted or not
     * @return errror: if an error ocurred
     */
    setClientPrefs: function(preferences, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_CLIENT_PREFS',
            [preferences]
        );
    },

    /**
     * Get Host info
     * @return succes: HostInfo json
     * @return errror: if an error ocurred
     */
    getHostInfo: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_HOST_INFO',
            [{ }]
        );
    },

    /**
     * Get project info from all_projects_list.xml
     * @param args json object with url data
     * @return succes: ProjectInfo json
     * @return errror: if an error ocurred
     */
    getProjectInfo: function(url, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_PROJECT_INFO',
            [{'url': url}]
        );
    },

    /**
     * Triggers change of state of project in BOINC core client
     * @param operation: operation to be triggered RpcClient.PROJECT_UPDATE = 1 | PROJECT_SUSPEND = 2 | PROJECT_RESUME = 3 | PROJECT_NNW = 4 | PROJECT_ANW = 5 | PROJECT_DETACH = 6 | PROJECT_RESET = 7
     * @param projectUrl: master URL of project
     * @return success: true for success, false for failure
     * @return error: if an errror ocurred
     */
    projectOp: function(operation, projectUrl, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'PROJECT_OP',
            [{'operation': operation, 'projectUrl': projectUrl}]
        );
    },

    /**
     * Reads project configuration for specified master URL.
     * @param url: master URL of the project
     * @return success: project configuration information (ProjectConfig json)
     * @return error: if an error ocurred
     */
    getProjectConfigPolling: function(url, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_PROJECT_CONFIG_POLLING',
            [{'url': url}]
        );
    },

    /**
     * Get all transfers
     * @return success: list of Transfer json
     * @return error: if an error ocurred
     */
    getTransfers: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_TRANSFERS',
            [{ }]
        );
    },

    /**
     * Get Server notices
     * @return success: string with server notices
     * @return error: if an error ocurred
     */
    getServerNotices: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_SERVER_NOTICES',
            [{ }]
        );
    },

    /**
     * Get projects tasks
     * @return success: list of Result json for al projects
     * @return error: if an error ocurred
     */
    getTasks: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_TASKS',
            [{ }]
        );
    },

    /**
     * Triggers operation on task in BOINC core client
     * @param  operation operation to be triggered RpcClient.RESULT_SUSPEND = 10 | RESULT_RESUME = 11 | RESULT_ABORT = 12
     * @param  projectUrl master URL of project
     * @param  resultName name of the result (or task)
     * @return success: true for success, false for failure
     * @return error: if an error ocurred
     */
    resultOp: function(operation, projectUrl, resultName, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'RESULT_OP',
            [{'operation': operation, 'projectUrl': projectUrl, 'resultName': resultName}]
        );
    },

    /**
     * Looks up account credentials for given user data.
     * Contains authentication key for project attachment.
     * @param url URL of project, either masterUrl(HTTP) or webRpcUrlBase(HTTPS)
     * @param id user ID, can be either name or eMail, see usesName
     * @param pwd password
     * @param usesName if true, id represents a user name, if not, the user's email address
     * @param teamName team name
     * @return success: account credentials (authenticator)
     * @return error: if an error ocurred (error message if some field are incorrect)
     */
    lookupCredentials: function(url, id, usesName, pwd, teamName, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'LOOKUP_CREDENTIALS',
            [{'url': url, 'id': id, 'usesName': usesName, 'pwd': pwd, 'teamName': teamName}]
        );
    },

    /**
     * Get client Auto-Start configuration
     * @return success: true/false if autostart is enabled
     * @return error: if an error ocurred
     */
    getAutostart: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_AUTOSTART',
            [{ }]
        );
    },

    /**
     * Get client current status description string
     * @return success: string with status description
     * @return error: if an error ocurred
     */
    getCurrentStatusDescription: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_CURRENT_STATUS_DESCRIPTION',
            [{ }]
        );
    },

    /**
     * Get client current status title string
     * @return success: string with status title
     * @return error: if an error ocurred
     */
    getCurrentStatusTitle: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_CURRENT_STATUS_TITLE',
            [{ }]
        );
    },

    /**
     * Get bettery charge status
     * @return success: charge level
     * @return error: if an error ocurred
     */
    getBatteryChargeStatus: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_BATTERY_CHARGE_STATUS',
            [{ }]
        );
    },

    /**
     * Get computing suspend reason (only a int that identify a reason, use statusDescription for a string)
     * @return success: computing suspend reason (int)
     * @return error: if an error ocurred
     */
    getComputingSuspendReason: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_COMPUTING_SUSPEND_REASON',
            [{ }]
        );
    },

    /**
     * Get network suspend reason (only a int that identify a reason, use statusDescription for a string)
     * @return success: network suspend reason (int)
     * @return error: if an error ocurred
     */
    getNetworkdSuspendReason: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_NETWORK_SUSPEND_REASON',
            [{ }]
        );
    },

    /**
     * Get if suspend computing when screen is on
     * @return success: true/false
     * @return error: if an error ocurred
     */
    getSuspendWhenScreenOn: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_SUSPEND_WHEN_SCREEN_ON',
            [{ }]
        );
    },

    /**
     * Get if computing on AC power source is enabled
     * @return success: true/false
     * @return error: if an error ocurred
     */
    getPowerSourceAc: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_POWER_SOURCE_AC',
            [{ }]
        );
    },

    /**
     * Get if computing on USB power source is enabled
     * @return success: true/false
     * @return error: if an error ocurred
     */
    getPowerSourceUsb: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_POWER_SOURCE_USB',
            [{ }]
        );
    },

    /**
     * Get if computing on WIRELESS power source is enabled
     * @return success: true/false
     * @return error: if an error ocurred
     */
    getPowerSourceWireless: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_POWER_SOURCE_WIRELESS',
            [{ }]
        );
    },

    /**
     * Set autostrat on/off
     * @param status true/false for autostart
     * @return success: if option setted
     * @return error: if an error ocurred       
     */
    setAutostart: function(status, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_AUTOSTART',
            [{'status': status}]
        );
    },

    /**
     * Set suspend when screen on on/off
     * @param status true/false for autostart
     * @return success: if option setted
     * @return error: if an error ocurred       
     */
    setSuspendWhenScreenOn: function(status, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_SUSPEND_WHEN_SCREEN_ON',
            [{'status': status}]
        );
    },

    /**
     * Set computing power surce AC on/off
     * @param status true/false for autostart
     * @return success: if option setted
     * @return error: if an error ocurred       
     */
    setPowerSourceAc: function(status, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_POWER_SOURCE_AC',
            [{'status': status}]
        );
    },

    /**
     * Set computing power surce USB on/off
     * @param status true/false for autostart
     * @return success: if option setted
     * @return error: if an error ocurred       
     */
    setPowerSourceUsb: function(status, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_POWER_SOURCE_USB',
            [{'status': status}]
        );
    },

    /**
     * Set computing power surce USB on/off
     * @param status true/false for autostart
     * @return success: if option setted
     * @return error: if an error ocurred       
     */
    setPowerSourceWireless: function(status, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SET_POWER_SOURCE_WIRELESS',
            [{'status': status}]
        );
    },

    /**
     * Run system Benchmarks
     * @return success: true/false if benchmark run
     * @return error: if an error ocurred  
     */
    runBenchmarks: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'RUN_BENCHMARKS',
            [{ }]
        );
    },

    /**
     * Force client data refresh
     * @return success: 
     * @return error: if an error ocurred  
     */
    forceRefresh: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'FORCE_REFRESH',
            [{ }]
        );
    },

     /**
     * Indicates whether service was able to obtain BOINC mutex.
     * If not, BOINC has not started and all other calls will fail.
     * @return success: true/false BOINC mutex acquisition successful
     * @return error: if an error ocurred
     */
    boincMutexAcquired: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'BOINC_MUTEX_ACQUIRED',
            [{ }]
        );
    },

    /**
     * Get project status
     * @param url master project url
     * @return success: String with project status
     * @return error: if an error ocurred        
     */
    getProjectStatus: function(url, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_PROJECT_STATUS',
            [{'url': url}]
        );
    },

    /**
     * Get attachale projects for current device and status (from all_projects.xml)
     * @return success: list of attachable projects (json of ProjectInfo)
     * @return error: if an error ocurred 
     */
    getAttachableProjects: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_ATTACHABLE_PROJECTS',
            [{ }]
        ); 
    },

    /**
     * Get Result/Task current status text
     * @param result result json object
     * @param success: string with current status
     */
    determineResultStatusText: function(result, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'DETERMINE_RESULT_STATUS_TEXT',
            [result]
        ); 
    },

    /**
     * Synchronize an Account Manager
     * @param url account manager url
     * @param success: success or error (true/false)
     */
    synchronizeAcctMgr: function(url, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'SYNCHRONIZE_ACCT_MGR',
            [{'url': url}]
        ); 
    },

    /**
     * Get account manager info
     * @param success: account manager info json string
     */
    getAcctMgrInfo: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_ACCT_MGR_INFO',
            [{ }]
        ); 
    },

    /**
     * Get client account manager info
     * @param success: client account manager info json string
     */
    getClientAcctMgrInfo: function(url, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'BoincPlugin',
            'GET_CLIENT_ACCT_MGR_INFO',
            [{ }]
        ); 
    }
}
