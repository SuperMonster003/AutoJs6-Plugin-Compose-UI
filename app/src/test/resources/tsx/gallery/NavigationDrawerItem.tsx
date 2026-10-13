"ui";

let item = compose.state('inbox');

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={8}>
            <compose.NavigationDrawerItem key='inbox' label='Inbox' icon={<compose.Icon name='Filled.Email' />} badge='12' selected={item.value === 'inbox'} onClick={function () { item.value = 'inbox'; }} />
            <compose.NavigationDrawerItem key='sent' label='Sent' icon={<compose.Icon name='Filled.Send' />} selected={item.value === 'sent'} onClick={function () { item.value = 'sent'; }} />
        </compose.Column>
    );
});
