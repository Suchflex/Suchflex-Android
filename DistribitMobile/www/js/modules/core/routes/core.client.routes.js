(function() {
  'use strict';

  // Setting up route
  angular
    .module('core')
    .config(Routes);

  Routes.$inject = ['$stateProvider', '$urlRouterProvider'];

  function Routes($stateProvider, $urlRouterProvider) {
    var prefix = 'js/modules/core/templates/';

    // Redirect to home view when route not found
    $urlRouterProvider.otherwise('/app/home');

    // Home state routing
    $stateProvider
    .state('app', {
      url: '/app',
      abstract: true,
      templateUrl: prefix + 'menu.client.template.html',
      controller: 'AppController'
    })

    .state('app.friends', {
        url: '/friends',
        views: {
          'menuContent': {
            templateUrl: prefix + 'friends.client.template.html'
          }
        }
      })
      .state('app.home', {
        url: '/home',
        views: {
          'menuContent': {
            templateUrl: prefix + 'home.client.template.html',
            controller: 'PlaylistsController'
          }
        }
      })
      .state('app.device', {
        url: '/device',
        views: {
          'menuContent': {
            templateUrl: prefix + 'device.client.template.html',
            controller: 'PlaylistsController'
          }
        }
      })

    .state('app.profile', {
      url: '/profile',
      views: {
        'menuContent': {
          templateUrl: prefix + 'profile.client.template.html',
          controller: 'PlaylistController'
        }
      }
    })
    // boinc views
    .state('app.main', {
      url: '/main',
      views: {
        'menuContent': {
          templateUrl: prefix + 'main.client.template.html',
          controller: 'MainController'
        }
      }
    })
    .state('app.settings', {
      url: '/settings',
      views: {
        'menuContent': {
          templateUrl: prefix + 'settings.client.template.html',
          controller: 'SettingsController'
        }
      }
    })
    .state('app.attach', {
      url: '/attach',
      views: {
        'menuContent': {
          templateUrl: prefix + 'attach.client.template.html',
          controller: 'AttachController'
        }
      }
    })
    .state('app.tasks', {
      url: '/tasks',
      views: {
        'menuContent': {
          templateUrl: prefix + 'tasks.client.template.html',
          controller: 'TasksController'
        }
      }
    });
  }
})();