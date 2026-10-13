"ui";

let open = compose.state(false);

compose.mount(function () {
    return (
        <compose.NavigationDrawer key='drawer' open={open.value} onOpenChange={function (value) { open.value = value; }} drawerContent={<compose.Column padding={16} spacing={8}>
            <compose.NavigationDrawerItem key='home' label='Home' icon={<compose.Icon name='Filled.Home' />} selected={true} onClick={function () { open.value = false; }} />
            <compose.NavigationDrawerItem key='settings' label='Settings' icon={<compose.Icon name='Filled.Settings' />} selected={false} onClick={function () { open.value = false; }} />
        </compose.Column>}>
            <compose.Column padding={16}>
                <compose.Button onClick={function () { open.value = true; }}>Open drawer</compose.Button>
            </compose.Column>
        </compose.NavigationDrawer>
    );
});
