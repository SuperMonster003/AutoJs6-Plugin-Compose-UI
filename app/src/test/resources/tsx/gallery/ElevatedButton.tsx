"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.ElevatedButton onClick={function () { toast('Elevated'); }}>Elevated</compose.ElevatedButton>
        </compose.Column>
    );
});
