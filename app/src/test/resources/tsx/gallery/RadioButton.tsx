"ui";

let size = compose.state('small');

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={8}>
            <compose.Row key='small' spacing={12} verticalAlignment='center'>
                <compose.RadioButton selected={size.value === 'small'} onClick={function () { size.value = 'small'; }} />
                <compose.Text key='small-label' text='Small' />
            </compose.Row>
            <compose.Row key='large' spacing={12} verticalAlignment='center'>
                <compose.RadioButton selected={size.value === 'large'} onClick={function () { size.value = 'large'; }} />
                <compose.Text key='large-label' text='Large' />
            </compose.Row>
        </compose.Column>
    );
});
