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
      var title = ["Manage Provisioning Requisitions", "Admin"];
      $routeProvider
          .when('/admin/requisitions', {
              templateUrl: 'angular-app/components/admin/requisition/views/requisitions.html',
              controller: 'RequisitionsController',
              title: title
          })
          .when('/admin/requisitions/:foreignSource', {
              templateUrl: 'angular-app/components/admin/requisition/views/requisition.html',
              controller: 'RequisitionController',
              title: title
          })
          .when('/admin/requisitions/:foreignSource/foreignSource', {
              templateUrl: 'angular-app/components/admin/requisition/views/foreignsource.html',
              controller: 'ForeignSourceController',
              title: title
          })
          .when('/admin/requisitions/:foreignSource/nodes/:foreignId', {
              templateUrl: 'angular-app/components/admin/requisition/views/node.html',
              controller: 'NodeController',
              title: title
          })
          .when('/admin/requisitions/:foreignSource/nodes/:foreignId/vertical', {
              templateUrl: 'angular-app/components/admin/requisition/views/node-panels.html',
              controller: 'NodeController',
              title: title
          })
          .when('/admin/quickaddnode', {
              templateUrl: 'angular-app/components/admin/requisition/views/quick-add-node-standalone.html',
              controller: 'QuickAddNodeController',
              title: ["Quick-Add Node", "Admin"],
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
