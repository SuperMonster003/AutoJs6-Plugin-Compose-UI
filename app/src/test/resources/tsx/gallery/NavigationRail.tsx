"ui";

let page = compose.state('home');

compose.mount(function () {
    return (
        <compose.Row>
            <compose.NavigationRail key='rail' header={<compose.Icon name='Filled.Menu' />}>
                <compose.NavigationRailItem key='home' icon={<compose.Icon name='Filled.Home' />} label='Home' selected={page.value === 'home'} onClick={function () { page.value = 'home'; }} />
                <compose.NavigationRailItem key='settings' icon={<compose.Icon name='Filled.Settings' />} label='Settings' selected={page.value === 'settings'} onClick={function () { page.value = 'settings'; }} />
            </compose.NavigationRail>
            <compose.Text key='page' padding={16} text={'Page: ' + page.value} />
        </compose.Row>
    );
});
