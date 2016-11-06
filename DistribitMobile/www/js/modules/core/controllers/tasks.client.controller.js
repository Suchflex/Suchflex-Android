(function() {
  'use strict';

  angular
    .module('core')
    .controller('TasksController', TasksController);
    
  TasksController.$inject = ['$scope', '$timeout', '$location'];

  /* @ngInject */
  function TasksController($scope, $timeout, $location) {
    // Current tasks (as object Result)
    $scope.tasks = [];

    // Auto update all data
    function autoUpdate(){
      // Read tasks
      $scope.getTasks();
      // program timeout new read
      if($location.path() == "/tasks")
        $timeout(autoUpdate, 4000);
    }

    // Get Tasks list
    $scope.getTasks = function(){
      boincPlugin.getTasks(function(tasks){
        // save tasks
        $scope.tasks = JSON.parse(tasks);
        // init data
        angular.forEach($scope.tasks, function(task) {
          task.do_result_op = "10";
          $scope.determineStatus(task);
        });
      }, error);
    }

    // Do task operation
    $scope.operation = function(task){
      boincPlugin.resultOp(task.do_result_op, task.project_url, task.name, function(success){
        console.log("task action: " + task.name + " at " + task.project_url + " -> " + task.do_result_op);
        alert("Operation Ok!");
        // Refresh tasks list
        $scope.getTasks();
      }, error);
    }

    // Determine task status
    $scope.determineStatus = function(task){
      boincPlugin.determineResultStatusText(task, function(status){
        task.current_status = status;
      }, error);
    }

    // Go to main view
    $scope.goBack = function(){
      $location.path("/");
    }

    // Generic error function
    var error = function(message){
      console.log("Distribit Oopsie! " + message);
    }

    // Generic log message
    var log = function(message){
      console.log("Distribit log: " + message);
    }

    // Init autoupdate
    autoUpdate();
  }
})();