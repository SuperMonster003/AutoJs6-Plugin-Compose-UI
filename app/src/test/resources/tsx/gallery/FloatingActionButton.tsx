"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.FloatingActionButton onClick={function () { toast('Add'); }}>
                <compose.Icon name='Filled.Add' />
            </compose.FloatingActionButton>
        </compose.Column>
    );
});
