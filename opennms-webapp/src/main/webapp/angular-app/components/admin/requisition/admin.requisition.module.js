/**
* @author Alejandro Galue <agalue@opennms.org>
* @copyright 2014 The OpenNMS Group, Inc.
*/

(function() {

  'use strict';

  angular.module('onms.ui.admin.requisition', [
    'ngRoute',
    'ngCookies',
    'ngAnimate',
    'ui.bootstrap',
    'angular-growl',
    'angular-loading-bar'
  ])

  .config(['$routeProvider', function ($routeProvider) {
      $routeProvider
          .when('/admin/requisitions', {
              templateUrl: 'angular-app/components/admin/requisition/views/requisitions.html',
              controller: 'RequisitionsController',
          })
          .when('/admin/requisitions/:foreignSource', {
              templateUrl: 'angular-app/components/admin/requisition/views/requisition.html',
              controller: 'RequisitionController',
          })
          .when('/admin/requisitions/:foreignSource/foreignSource', {
              templateUrl: 'angular-app/components/admin/requisition/views/foreignsource.html',
              controller: 'ForeignSourceController'
          })
          .when('/admin/requisitions/:foreignSource/nodes/:foreignId', {
              templateUrl: 'angular-app/components/admin/requisition/views/node.html',
              controller: 'NodeController'
          })
          .when('/admin/requisitions/:foreignSource/nodes/:foreignId/vertical', {
              templateUrl: 'angular-app/components/admin/requisition/views/node-panels.html',
              controller: 'NodeController'
          })
          .when('/admin/requisitions/quickaddnode', {
              templateUrl: 'angular-app/components/admin/requisition/views/quick-add-node-standalone.html',
              controller: 'QuickAddNodeController',
              resolve: {
                  foreignSources: function () {
                      return null;
                  }
              }
          });
  }])

  .config(['growlProvider', function(growlProvider) {
    growlProvider.globalTimeToLive(3000);
    growlProvider.globalPosition('bottom-center');
  }])

  .config(['$uibTooltipProvider', function($uibTooltipProvider) {
    $uibTooltipProvider.setTriggers({
      'mouseenter': 'mouseleave'
    });
    $uibTooltipProvider.options({
      'placement': 'left',
      'trigger': 'mouseenter'
    });
  }]);

}());
