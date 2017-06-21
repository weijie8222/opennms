(function () {

    'use strict';

    angular.module('onms.ui', [
        'onms.ui.admin',
        'ngRoute',
        'ui.bootstrap',
    ])
        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider
                .when('/', {
                    templateUrl: 'angular-app/components/core/empty.html'
                })
                .otherwise({
                    redirectTo: '/'
                });
        }])
        .run(['$rootScope', function($rootScope) {
            $rootScope.$on('$routeChangeSuccess', function (event, current, previous) {
                // Hack the title
                if (current && current.$$route && current.$$route.title) {
                    $rootScope.title = current.$$route.title + " | OpenNMS Web Console";
                } else {
                    $rootScope.title = "OpenNMS Web Console";
                }
                // Hack the base href
                
                angular.element(document).ready(function() {
                    document.title = $rootScope.title;
                    $rootScope.baseHref = document.baseURI;
                });
            })
        }]);

}());
