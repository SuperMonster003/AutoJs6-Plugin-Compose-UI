"ui";

let enabled = compose.state(true);

compose.mount(function () {
    return (
        <compose.Row padding={16} spacing={12} verticalAlignment='center'>
            <compose.Text key='label' text={enabled.value ? 'Enabled' : 'Disabled'} weight={1} />
            <compose.Switch key='toggle' checked={enabled.value} onCheckedChange={function (value) { enabled.value = value; }} />
        </compose.Row>
    );
});
