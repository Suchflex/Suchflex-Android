(function() {
  'use strict';

  angular
    .module('core')
    .controller('PlaylistController', PlaylistController);
    
  PlaylistController.$inject = ['$scope', '$stateParams'];

  /* @ngInject */
  function PlaylistController($scope, $stateParams) {
    var vm = this;
    vm.title = 'PlaylistController';

    vm.init = init;
    
    // declarations
    //
    function init() {
    }
  }
})();