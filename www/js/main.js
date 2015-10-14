var coofluence = {
    main: function () {
        var nbaTeams = new Bloodhound({
            datumTokenizer: Bloodhound.tokenizers.obj.whitespace('team'),
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            prefetch: 'http://localhost:8080/rest/autoComplete?type=page&action=prefetch',
            remote: 'http://localhost:8080/rest/autoComplete?type=page'
        });

        var nhlTeams = new Bloodhound({
            datumTokenizer: Bloodhound.tokenizers.obj.whitespace('team'),
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            prefetch: 'http://localhost:8080/rest/autoComplete?type=blogPost&action=prefetch',
            remote: 'http://localhost:8080/rest/autoComplete?type=blogPost'
        });

        $('#multiple-datasets .typeahead').typeahead({
                classNames: {
                    input: 'form-control input-lg',
                    hint: 'form-control input-lg'
                },
                highlight: true
            },
            //{
            //    name: 'coofluence',
            //    display:"title",
            //    source: function(query,syncResults,asyncResults){
            //        syncResults([{query:query}]);
            //        console.log(query);
            //    },
            //    templates: {
            //        suggestion:Handlebars.compile('<div><a href="#query={{query}}">Search {{query}}</a></div>')
            //    }
            //},
            {
                name: 'pages',
                display: "title",
                source: nbaTeams,
                templates: {
                    header: '<h3 class="league-name">Pages</h3>',
                    suggestion: Handlebars.compile('<div>' +
                        '<div><strong>{{title}}</strong><span class="pull-right">{{author}}</span></div>' +
                        '<div>{{author}} [{{tags}}]</div>' +
                        '</div>')
                }
            },
            {
                name: 'blogPosts',
                display: "title",
                source: nhlTeams,
                templates: {
                    header: '<h3 class="league-name">Blog posts</h3>',
                    suggestion: Handlebars.compile('<div>' +
                        '<div><strong>{{title}}</strong><span class="pull-right">{{author}}</span></div>' +
                        '<div>{{author}} [{{tags}}]</div>' +
                        '</div>')
                }
            });
    }
};
coofluence.main();

