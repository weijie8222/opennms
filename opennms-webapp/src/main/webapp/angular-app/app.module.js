(function () {

    'use strict';

    angular.module('onms.ui', [
        'onms.ui.scanreport',
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
                var constantTitle = "OpenNMS Web Console";
                var titles = [];

                if (current && current.$$route && current.$$route.title) {
                    if (angular.isArray(current.$$route.title)) {
                        titles = titles.concat(current.$$route.title);
                    } else {
                        titles.push(current.$$route.title);
                    }
                }
                titles.push(constantTitle);

                $rootScope.title = titles.join(" | ");

                angular.element(document).ready(function() {
                    document.title = $rootScope.title;

                    // Hack the base href
                    $rootScope.baseHref = document.baseURI;
                });
            })
        }]);

}());
