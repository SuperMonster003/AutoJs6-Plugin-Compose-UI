"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.IconButton onClick={function () { toast('Icon'); }}>
                <compose.Icon name='Filled.Favorite' />
            </compose.IconButton>
        </compose.Column>
    );
});
