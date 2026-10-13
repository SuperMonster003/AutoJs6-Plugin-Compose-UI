"ui";

compose.mount(function () {
    return (
        <compose.Row padding={16} spacing={8} verticalAlignment='center'>
            <compose.Icon name='Filled.Star' />
            <compose.Text key='label' text='Starred' />
            <compose.Spacer weight={1} />
            <compose.Text key='trailing' text='End' />
        </compose.Row>
    );
});
