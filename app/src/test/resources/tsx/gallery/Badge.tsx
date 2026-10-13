"ui";

compose.mount(function () {
    return (
        <compose.Row padding={16} spacing={24}>
            <compose.Badge text='3' visible={true}>
                <compose.Icon name='Filled.Email' />
            </compose.Badge>
            <compose.Badge visible={true}>
                <compose.Icon name='Filled.Notifications' />
            </compose.Badge>
        </compose.Row>
    );
});
