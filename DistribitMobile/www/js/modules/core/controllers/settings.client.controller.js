(function() {
  'use strict';

  angular
    .module('core')
    .controller('SettingsController', SettingsController);
    
  SettingsController.$inject = ['$scope', '$timeout', '$location'];

  /* @ngInject */
  function SettingsController($scope, $timeout, $location) {
    // Current Settings
    $scope.settings = {};
    $scope.misc = {};
    
    // Get client preferences
    $scope.getSettings = function(){
      boincPlugin.getClientPrefs(function(success){
        // Parse and save settings
        $scope.settings = JSON.parse(success);
        // Read Other settings
        boincPlugin.getAutostart(function(success){
          $scope.misc.auto_start = success == "true";
        }, error);
        boincPlugin.getSuspendWhenScreenOn(function(success){
          $scope.misc.suspend_when_screen_on = success == "true";
        }, error);
        boincPlugin.getPowerSourceAc(function(success){
          $scope.misc.power_source_ac = success == "true";
        }, error);
        boincPlugin.getPowerSourceUsb(function(success){
          $scope.misc.power_source_usb = success == "true";
        }, error);
        boincPlugin.getPowerSourceWireless(function(success){
          $scope.misc.power_source_wireless = success == "true";
        }, error);
      }, error);
    };

    // Save client preferences
    $scope.save = function(){
      // prefs saved
      var count = 0;
      // saved success
      var success = function(data){
        console.log(data);
        if(data == "true" || data == "OK"){
          count++;
        } else {
          alert("error saving settings");
        }
        if(count == 6){
          log("settings saved");
          alert("Settings saved");
          $timeout(function(){$location.path("/");}, 2000);
        }
      }
      // Set client preferences
      boincPlugin.setClientPrefs($scope.settings, success, error);
      boincPlugin.setAutostart($scope.misc.auto_start, success, error);
      boincPlugin.setSuspendWhenScreenOn($scope.misc.suspend_when_screen_on, success, error);
      boincPlugin.setPowerSourceAc($scope.misc.power_source_ac, success, error);
      boincPlugin.setPowerSourceUsb($scope.misc.power_source_usb, success, error);
      boincPlugin.setPowerSourceWireless($scope.misc.power_source_wireless, success, error);
    }

    // Discard changes and go home
    $scope.cancel = function(){
      $location.path("/");
    }

      // Profile 1
    $scope.profile1 = function () {
        $scope.settings.run_on_batteries = false;
        $scope.settings.network_wifi_only = true;
        $scope.misc.suspend_when_screen_on
        $scope.misc.power_source_ac
        $scope.misc.power_source_usb
        $scope.save();
        
    }
    $scope.profile2 = function () {
        $scope.settings.run_on_batteries = false;
        $scope.settings.network_wifi_only = true;
        $scope.save();

    }
    $scope.profile3 = function () {
        $scope.settings.run_on_batteries = true;
        $scope.settings.network_wifi_only = false;
        $scope.save();

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