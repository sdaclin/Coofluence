var confluenceApp = angular.module('coofluenceApp', ['siyfion.sfTypeahead', 'ngResource', 'ngAnimate'])
    .controller('AppCtrl', function ($scope, $rootScope) {
        $scope.searchQuery = {};
        $scope.results = [];
    })
    .controller('TypeaheadCtrl', function ($scope) {
        var pageSuggestions = new Bloodhound({
            datumTokenizer: Bloodhound.tokenizers.whitespace,
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            prefetch: 'http://localhost:8080/rest/autoComplete?type=page&action=prefetch',
            remote: {
                url: 'http://localhost:8080/rest/autoComplete?type=page&q=WILDCARD',
                wildcard: 'WILDCARD'
            },
            limit: 10,
            sufficient: 10
        });

        var blogPostSuggestion = new Bloodhound({
            datumTokenizer: Bloodhound.tokenizers.whitespace,
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            prefetch: 'http://localhost:8080/rest/autoComplete?type=blogPost&action=prefetch',
            remote: {
                url: 'http://localhost:8080/rest/autoComplete?type=blogPost&q=WILDCARD',
                wildcard: 'WILDCARD'
            },
            limit: 10,
            sufficient: 10
        });

        // Typeahead options object
        $scope.typeaheadOptions = {
            //highlight: true
        };

        // Multiple dataset example
        $scope.pageAndBlogPostDataset = [
            {
                name: 'pageSuggestions',
                //displayKey: 'label',
                source: pageSuggestions.ttAdapter(),   // Note the pageSuggestions Bloodhound engine isn't really defined here.
                templates: {
                    header: '<h3>Pages</h3>',
                    suggestion: Handlebars.compile('<div><a href="{{payload}}">{{label}}</a></div>')
                }
            },
            {
                name: 'blogPostSuggestion',
                //displayKey: 'label',
                source: blogPostSuggestion.ttAdapter(),   // Note the blogPostSuggestion Bloodhound engine isn't really defined here.
                templates: {
                    header: '<h3>Blog posts</h3>',
                    suggestion: Handlebars.compile('<div><a href="{{payload}}">{{label}}</a></div>')
                }
            }
        ];


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
            console.log(newVal);
            if (newVal != null && newVal.length > 1) {
                $scope.results.content = SearchEndpoint.search({q: $scope.searchQuery.withDelay});
            }
        });
    });

confluenceApp.factory('SearchEndpoint', ['$resource', function ($resource) {
    return $resource('http://localhost:8080/rest/search', null, {search: {method: 'GET', isArray: true}});
}]);

confluenceApp.filter("sanitize", ['$sce', function ($sce) {
    return function (htmlCode) {
        return $sce.trustAsHtml(htmlCode);
    }
}]);