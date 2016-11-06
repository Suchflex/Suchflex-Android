(function() {
  'use strict';

  //
  angular
    .module('core')
    .controller('HomeController', HomeController);

  HomeController.$inject = ['$scope', '$reactive'];

  function HomeController($scope, $reactive) {
    $reactive(this).attach($scope);

    this.helpers({
      variable: function () {
        return 'here ohh';
      }
    });

  }

})();