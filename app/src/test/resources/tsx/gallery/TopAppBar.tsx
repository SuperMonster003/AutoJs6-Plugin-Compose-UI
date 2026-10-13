"ui";

compose.mount(function () {
    return (
        <compose.Scaffold topBar={<compose.TopAppBar title='Title' navigationIcon={<compose.IconButton onClick={function () { toast('Navigation'); }}>
            <compose.Icon name='Filled.Menu' />
        </compose.IconButton>} actions={<compose.IconButton onClick={function () { toast('Action'); }}>
            <compose.Icon name='Filled.MoreVert' />
        </compose.IconButton>} />}>
            <compose.Column padding={16}>
                <compose.Text key='body' text='Page content' />
            </compose.Column>
        </compose.Scaffold>
    );
});
