"ui";

let query = compose.state('');
let expanded = compose.state(false);

compose.mount(function () {
    return (
        <compose.Column padding={16}>
            <compose.SearchBar key='search' query={query.value} expanded={expanded.value} placeholder='Search' onQueryChange={function (text) { query.value = text; }} onSearch={function (text) { toast('Searching ' + text); expanded.value = false; }} onExpandedChange={function (value) { expanded.value = value; }}>
                <compose.Text key='hint' text='Suggestions appear here' />
            </compose.SearchBar>
        </compose.Column>
    );
});
