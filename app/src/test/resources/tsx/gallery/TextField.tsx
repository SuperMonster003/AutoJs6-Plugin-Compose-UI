"ui";

let name = compose.state('');

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={8}>
            <compose.TextField key='name' text={name.value} label='Name' placeholder='Your name' singleLine={true} onValueChange={function (text) { name.value = text; }} />
            <compose.Text key='echo' text={'Hello, ' + (name.value || 'stranger')} />
        </compose.Column>
    );
});
