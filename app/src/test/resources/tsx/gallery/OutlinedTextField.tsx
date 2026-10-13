"ui";

let email = compose.state('');

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.OutlinedTextField key='email' text={email.value} label='Email' keyboardType='email' isError={email.value.length > 0 && email.value.indexOf('@') < 0} supportingText='Enter a valid address' onValueChange={function (text) { email.value = text; }} />
        </compose.Column>
    );
});
