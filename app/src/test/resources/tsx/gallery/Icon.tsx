"ui";

compose.mount(function () {
    return (
        <compose.Row padding={16} spacing={16}>
            <compose.Icon name='Filled.Home' />
            <compose.Icon name='Filled.Favorite' tint='#E53935' />
            <compose.Icon name='Outlined.Settings' />
        </compose.Row>
    );
});
