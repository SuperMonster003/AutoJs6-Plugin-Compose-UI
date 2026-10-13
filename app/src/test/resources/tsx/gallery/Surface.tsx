"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.Surface color='#FFF8E1' shape='rounded' tonalElevation={2}>
                <compose.Text padding={16} text='A tinted surface' />
            </compose.Surface>
        </compose.Column>
    );
});
