"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.FilledTonalButton onClick={function () { toast('Tonal'); }}>Filled tonal</compose.FilledTonalButton>
        </compose.Column>
    );
});
