"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.TextButton onClick={function () { toast('Text'); }}>Text button</compose.TextButton>
        </compose.Column>
    );
});
