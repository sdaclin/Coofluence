const restEndpoint = '/rest';

var confluenceApp = angular.module('coofluenceApp', ['ngResource', 'ui.bootstrap'])
    .controller('AppCtrl', function ($scope) {
        $scope.searchQuery = {};
        $scope.results = [];
    })
    .controller('TypeaheadCtrl', function ($scope, $http, $location, $timeout) {
        if ($location.search()['q'] != undefined) {
            $scope.searchQuery.value = $location.search()['q'];
        }
        $scope.searchResults = function (search) {
            return $http.get(resolveUrl($location, restEndpoint + '/autoComplete'), {
                params: {
                    type: 'page',
                    q: search
                }
            }).then(function (response) {
                return response.data;
            });
        };

        // Little trick to start searching automatically after n ms of inactivity
        $scope.searchQuery.withDelay = '';
        var tempFilterText = '',
            filterTextTimeout;
        $scope.$watch('searchQuery.value', function (val) {
            if (filterTextTimeout) $timeout.cancel(filterTextTimeout);

            tempFilterText = val;
            filterTextTimeout = $timeout(function () {
                $scope.searchQuery.withDelay = tempFilterText;
                $location.search('q', tempFilterText);
            }, 250); // delay 250 ms
        });
    })
    .controller('ResultCtrl', function ($scope, SearchEndpoint) {


        $scope.$watch('searchQuery.withDelay', function (newVal) {
            if (newVal != null && newVal.length > 1) {
                $scope.results.content = SearchEndpoint.search({q: $scope.searchQuery.withDelay});
            }
        });
    });

confluenceApp
    .factory('SearchEndpoint', ['$resource', '$location', function ($resource, $location) {
        return $resource(resolveUrl($location, restEndpoint + '/search'), null, {search: {method: 'GET'}});
    }])
    .factory('SuggestEndpoint', ['$resource', '$location', function ($resource, $location) {
        return $resource(resolveUrl($location, restEndpoint + '/autoComplete'), null, {suggest: {method: 'GET', isArray: true}});
    }]);

confluenceApp.filter("sanitize", ['$sce', function ($sce) {
    return function (htmlCode) {
        return $sce.trustAsHtml(htmlCode);
    }
}]);

function resolveUrl($location, path) {
    return $location.protocol() + "://" + $location.host() + ":" + changePortIf8081($location.port()) + path;
}

function changePortIf8081(port) {
    return (port == "8081") ? "8080" : port;
}