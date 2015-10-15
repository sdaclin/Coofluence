var confluenceApp = angular.module('coofluenceApp', ['ngResource', 'ui.bootstrap'])
    .controller('AppCtrl', function ($scope, $rootScope) {
        $scope.searchQuery = {};
        $scope.results = [];
    })
    .controller('TypeaheadCtrl', function ($scope, $http) {
        $scope.getLocation = function (search) {
            return $http.get('http://localhost:8080/rest/autoComplete', {
                params: {
                    type: 'page',
                    q: search
                }
            }).then(function (response) {
                return response.data;
            });
        };
    })
    .controller('ResultCtrl', function ($scope, $timeout, SearchEndpoint) {
        // Little trick to start searching automatically after n ms of inactivity
        $scope.searchQuery.withDelay = '';
        var tempFilterText = '',
            filterTextTimeout;
        $scope.$watch('searchQuery.value', function (val) {
            if (filterTextTimeout) $timeout.cancel(filterTextTimeout);

            tempFilterText = val;
            filterTextTimeout = $timeout(function () {
                $scope.searchQuery.withDelay = tempFilterText;
            }, 250); // delay 250 ms
        });

        $scope.$watch('searchQuery.withDelay', function (newVal) {
            if (newVal != null && newVal.length > 1) {
                $scope.results.content = SearchEndpoint.search({q: $scope.searchQuery.withDelay});
            }
        });
    });

confluenceApp
    .factory('SearchEndpoint', ['$resource', function ($resource) {
        return $resource('http://localhost:8080/rest/search', null, {search: {method: 'GET'}});
    }])
    .factory('SuggestEndpoint', ['$resource', function ($resource) {
        return $resource('http://localhost:8080/rest/autoComplete', null, {suggest: {method: 'GET', isArray: true}});
    }]);

confluenceApp.filter("sanitize", ['$sce', function ($sce) {
    return function (htmlCode) {
        return $sce.trustAsHtml(htmlCode);
    }
}]);