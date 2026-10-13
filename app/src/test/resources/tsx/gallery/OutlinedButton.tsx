"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.OutlinedButton onClick={function () { toast('Outlined'); }}>Outlined</compose.OutlinedButton>
        </compose.Column>
    );
});
