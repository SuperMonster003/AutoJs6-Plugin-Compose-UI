"ui";

let agreed = compose.state(false);

compose.mount(function () {
    return (
        <compose.Row padding={16} spacing={12} verticalAlignment='center'>
            <compose.Checkbox key='agree' checked={agreed.value} onCheckedChange={function (value) { agreed.value = value; }} />
            <compose.Text key='label' text='I agree to the terms' />
        </compose.Row>
    );
});
