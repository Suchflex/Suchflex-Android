(function() {
  'use strict';

  angular
    .module('core')
    .controller('MainController', MainController);
    
  MainController.$inject = ['$scope', '$timeout', '$location'];

  /* @ngInject */
  function MainController($scope, $timeout, $location) {
    // Current projects
    $scope.projects = [];
    // Init current view
    $scope.mainView = true;
    $scope.projectDetails = false;
    $scope.currentMode = "2";

    /**
     *
     * Use boincPlugin.boincMutexAcquired(s,e) before call another boinc method to check
     * if boinc client was correctly initialized
     *
     */

    // Auto update all data
    function autoUpdate(){
      // Read all data
      $scope.getProjects();
      $scope.readData();
      // program timeout new read
      if($location.path() == "/")
        $timeout(autoUpdate, 1000);
    }
    

    // Get Attached projects
    $scope.getProjects = function(){
      // request current attached projects
      boincPlugin.getProjects(function(projects){
        // save projects
        $scope.projects = JSON.parse(projects);
        // init projects op's and status
        angular.forEach($scope.projects, function(project) {
          $scope.getStatus(project);
          project.do_project_op = "1";
        });
      }, error);
    }

    // Get status for each project
    $scope.getStatus = function(project){
      boincPlugin.getProjectStatus(project.master_url, function(status){
        project.current_status = status;
      }, error);
    }

    // Read notices and status
    $scope.readData = function(){
      // Get notices and status description
      boincPlugin.getServerNotices(function(notice){
        $scope.notices = JSON.parse(notice);
      }, error);
      boincPlugin.getCurrentStatusDescription(function(status){
        $scope.status = status;
      }, error);
      // Get computing status
      boincPlugin.getComputingStatus(function(status){
        if(status == 0)
          $scope.currentMode = "3";
        if(status == 2)
          $scope.currentMode = "2";
      }, error);
      // Get attached account managers
      boincPlugin.getAcctMgrInfo(function(success){
        $scope.account_manager = JSON.parse(success);
      }, error);
    }

    $scope.accountManagerOp = function(op){
      if(op == 'sync'){
        boincPlugin.synchronizeAcctMgr($scope.account_manager.acct_mgr_url, function(success){
          alert("Sincronizando");
        }, error);
      } else { // 'remove'
        boincPlugin.attachAcctMgr("","","", function(success){
          alert("Removido " + success);
        }, error);
      }
    }

    // Do project operation
    $scope.operation = function(project){
      boincPlugin.projectOp(project.do_project_op, project.master_url, function(success){
        console.log("project action: " + project.master_url + " -> " + project.do_project_op);
        alert("Operation Ok!");
        // Refresh projects list
        $scope.getProjects();
      }, error);
    }

    // View project details and info
    $scope.details = function(project){
      boincPlugin.getProjectInfo(project.master_url, function(success){
        $scope.selected_project = JSON.parse(success);
        $scope.mainView = false;
        $scope.projectDetails = true;
      }, error);
    }

    // Set client run modes RUN_MODE_NEVER = 3 | BOINCDefs.RUN_MODE_AUTO = 2
    $scope.setRunMode = function(mode){
      boincPlugin.setRunMode($scope.currentMode, success, error);
      boincPlugin.setNetworkMode($scope.currentMode, success, error);
      //
      var succesfull = 0;
      function success(data){
        if(data == "true")
          succesfull++;
        if(succesfull == 2){
          alert("Run mode established");
          $scope.readData();
        }
        if(data == "false")
          alert("Cant set run mode");
      }
    }

    // Generic error function
    var error = function(message){
      console.log("Distribit Oopsie! " + message);
    };

    // Generic log message
    var log = function(message){
      console.log("Distribit log: " + message);
    }

    // Init autoupdate
    autoUpdate();
  }
})();