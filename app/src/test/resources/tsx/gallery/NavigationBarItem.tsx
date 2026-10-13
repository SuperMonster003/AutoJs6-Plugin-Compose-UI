"ui";

compose.mount(function () {
    return (
        <compose.NavigationBar key='bar'>
            <compose.NavigationBarItem key='inbox' icon={<compose.Icon name='Filled.Email' />} label='Inbox' selected={true} alwaysShowLabel={true} onClick={function () { toast('Inbox'); }} />
            <compose.NavigationBarItem key='starred' icon={<compose.Icon name='Filled.Star' />} label='Starred' selected={false} onClick={function () { toast('Starred'); }} />
        </compose.NavigationBar>
    );
});
