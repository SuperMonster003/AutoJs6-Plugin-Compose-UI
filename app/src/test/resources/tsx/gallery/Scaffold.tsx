"ui";

compose.mount(function () {
    return (
        <compose.Scaffold topBar={<compose.TopAppBar title='Scaffold' />}>
            <compose.Column padding={16}>
                <compose.Text key='body' text='Content below the app bar' />
            </compose.Column>
        </compose.Scaffold>
    );
});
