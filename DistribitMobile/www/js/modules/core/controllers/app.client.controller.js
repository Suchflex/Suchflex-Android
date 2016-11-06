(function() {
  'use strict';

  angular
    .module('core')
    .controller('AppController', AppController);
    
  AppController.$inject = ['$scope', '$ionicModal', '$timeout'];

  /* @ngInject */
  function AppController($scope, $ionicModal, $timeout) {
    var prefix = 'js/modules/core/templates/';

    // With the new view caching in Ionic, Controllers are only called
    // when they are recreated or on app start, instead of every page change.
    // To listen for when this page is active (for example, to refresh data),
    // listen for the $ionicView.enter event:
    //$scope.$on('$ionicView.enter', function(e) {
    //});
    $scope.autoplay=true;

    $scope.changeAutoplay = function(){
      $scope.autoplay = !$scope.autoplay;
    };

    // Form data for the login modal
    $scope.loginData = {};

    // Create the login modal that we will use later
    $ionicModal.fromTemplateUrl(prefix + 'login.client.template.html', {
      scope: $scope
    }).then(function(modal) {
      $scope.modal = modal;
    });

    // Triggered in the login modal to close it
    $scope.closeLogin = function() {
      $scope.modal.hide();
    };

    $scope.closeIntro = function() {
      $scope.modalIntro.hide();
    };

    $scope.swiper = {};
   
    $scope.onReadySwiper = function (swiper) {

      swiper.on('slideChangeStart', function () {
          console.log('slide start');
      });
       
      swiper.on('onSlideChangeEnd', function () {
          console.log('slide end');
      });     
    };

    // Open the login modal
    $scope.login = function() {
      $scope.modal.show();
    };

    $ionicModal.fromTemplateUrl(prefix + 'intro.client.template.html', {
      scope: $scope
    }).then(function(modal) {
      $scope.modalIntro = modal;
    });

    $scope.intro = function() {
      $scope.modalIntro.show();
    };
    
    // Perform the login action when the user submits the login form
    $scope.doLogin = function() {
      console.log('Doing login', $scope.loginData);

      // Simulate a login delay. Remove this and replace with your login
      // code if using a login system
      $timeout(function() {
        $scope.closeLogin();
      }, 1000);
    };
  }
})();