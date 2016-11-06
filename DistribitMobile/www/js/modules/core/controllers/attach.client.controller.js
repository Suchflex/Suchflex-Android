(function() {
  'use strict';

  angular
    .module('core')
    .controller('AttachController', AttachController);
    
  AttachController.$inject = ['$scope', '$timeout', '$location'];

  /* @ngInject */
  function AttachController($scope, $timeout, $location) {
    // Attachable projects
    $scope.attachableProjects = [];
    
    // Current credentials input
    $scope.credentials = {};

    // Status
    $scope.attach_project = true;
    $scope.attachable_projects = false;
    $scope.input_credentials = false;
    $scope.input_url = false;
    $scope.input_account_manager = false;
    $scope.current_status = "No attachable projects";

    // Current selected project to attach
    $scope.selected_project = {};

    // Account Manager data
    $scope.manager = {};

    // Get attachable projects
    $scope.getAttachableProjects = function(){
        // save attachable projects
        var success = function(data){
          $scope.attachableProjects = JSON.parse(data);
          if($scope.attachableProjects.length != 0){
            $scope.current_status = "Waiting action";
            $scope.attachable_projects = true;
          } else {
            $scope.current_status = "No attachable projects";
            $scope.attachable_projects = false;
          }
          //
          console.log(data);
        };
        // request attachable projects
        boincPlugin.getAttachableProjects(success, error);
    };

    // Show attach account manager
    $scope.attachAccountManager = function(){
      $scope.attach_project = false;
      $scope.input_credentials = false;
      $scope.input_url = false;
      $scope.input_account_manager = true;
      $scope.current_status = "Enter Account Manager credentials"
    }

    // Do account manager attach
    $scope.attachMgr = function(){
      boincPlugin.attachAcctMgr($scope.manager.url, $scope.manager.user, $scope.manager.pwd, function(success){
        alert("Account Manager Attached");
        $scope.attach_project = true;
        $scope.input_account_manager = false;
      }, function(error){
        alert(error);
      });
    }

    // Attach project by master url
    $scope.attachByUrl = function(){
      $scope.attach_project = false;
      $scope.input_credentials = false;
      $scope.input_account_manager = false;
      $scope.input_url = true;
      $scope.current_status = "Enter project master url";
    }

    $scope.acceptUrl = function(){
      // Validate url
      if($scope.selected_project.url.length == 0){
        alert("Empty url");
        return;
      }
      $scope.current_status = "Retrieving project config...";
      $scope.input_url = false;
      $scope.attach($scope.selected_project);
    }

    // Attach a project (by attachable projet list)
    $scope.attach = function(project){
      // Save reference to selected project
      $scope.selected_project = project;

      $scope.current_status = "Retrieving project config...";
      $scope.attach_project = false;

      // Set selected project
      boincPlugin.setManuallySelectedProject($scope.selected_project.url, checkConfigRetrieval, function(error){
        alert(error);
        $location.path("/attach");
      });
    };

    // Wait for project config retrieval finished
    function checkConfigRetrieval(){
      boincPlugin.hasProjectConfigRetrievalFinished(configRetrieval, error);
    };

    // wait for finished set credentials for singin/singup
    function configRetrieval(finished){
      if(finished == "true") {
        boincPlugin.getNextSelectedProject(function(projectWrapper){
          console.log(projectWrapper);
          $scope.selected_project = JSON.parse(projectWrapper);
          $scope.input_credentials = true;
          $scope.current_status = "Input user credentials";  
        }, function(error){
          alert("Can't attach project");
          $location.path("/");
        });
      } else {
        $timeout(checkConfigRetrieval, 500);
      }
    };

    // Login to project (attach)
    $scope.login = function(){
      $scope.current_status = "Try to login";
      // check input
      if(!$scope.credentials.email || $scope.credentials.email.length == 0) {
        alert("Enter a valid email");
        return;
      } 
      else if(!$scope.credentials.user || $scope.credentials.user.length == 0) {
        alert("Enter a valid username");
        return;
      } 
      else if(!$scope.credentials.pwd || $scope.credentials.pwd.length == 0) {
        alert("Enter a valid password");
        return;
      }
      else if(!$scope.credentials.pwd || $scope.credentials.pwd.length < $scope.selected_project.config.minPwdLength) {
        alert("Password to short");
        return;
      }
      // Try to login
      boincPlugin.setCredentials($scope.credentials.email, $scope.credentials.user, $scope.credentials.pwd, doLogin, error);
      function doLogin(data){
        boincPlugin.login(attach, accountError);
      }
    }

    // Register to project (attach)
    $scope.register = function(){
      $scope.current_status = "Try to register";
      // check input
      if(!$scope.credentials.email || $scope.credentials.email.length == 0) {
        alert("Enter a valid email");
        return;
      } 
      else if(!$scope.credentials.user || $scope.credentials.user.length == 0) {
        alert("Enter a valid username");
        return;
      } 
      else if(!$scope.credentials.pwd || $scope.credentials.pwd.length == 0) {
        alert("Enter a valid password");
        return;
      }
      else if($scope.credentials.pwd.length < $scope.selected_project.config.minPwdLength) {
        alert("Password to short");
        return;
      }
      // Check account creation enabled
      if($scope.selected_project.config.accountCreationDisabled || $scope.selected_project.config.clientAccountCreationDisabled){
        alert("Account Creation / Client account Creation: Disabled");
        return;
      }

      // Try to register
      boincPlugin.setCredentials($scope.credentials.email, $scope.credentials.user, $scope.credentials.pwd, doRegister, error);
      function doRegister(data){
        boincPlugin.register(attach, accountError);
      }
    }

    // Used to show register/login error alert
    function accountError(data){
      alert(data);
    }

    // Do project attach using auth code from register/login
    function attach(auth){
      $scope.current_status = "Authentication Ok, attaching project...";
      $scope.input_credentials = false;

      // Try to attach the project
      boincPlugin.attachProject($scope.selected_project.url, $scope.selected_project.name, auth, attachOk, error);

      // If attach succesfull
      function attachOk(data){
        alert("Project attached");
        // Update list of attachable projects
        $scope.getAttachableProjects();
        $scope.attach_project = true;
      }
    }

    // Go to main view
    $scope.goBack = function(){
      $location.path("/");
    }

    // Generic error function
    var error = function(message){
      console.log("Distribit Oopsie! " + message);
    };

    // Generic log message
    var log = function(message){
      console.log("Distribit log: " + message);
    }
  }
})();