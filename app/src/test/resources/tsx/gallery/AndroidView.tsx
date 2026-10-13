"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.Text key='label' text='A classic View inside Compose' />
            <compose.AndroidView key='native' view={ui.inflate('<text text="Hello from XML" textSize="18sp" />')} />
        </compose.Column>
    );
});
