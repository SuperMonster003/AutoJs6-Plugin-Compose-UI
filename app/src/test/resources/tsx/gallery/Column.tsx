"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={8} horizontalAlignment='center'>
            <compose.Text key='first' text='First' />
            <compose.Text key='second' text='Second' />
            <compose.Text key='third' text='Third' />
        </compose.Column>
    );
});
