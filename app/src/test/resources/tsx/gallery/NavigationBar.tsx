"ui";

let tab = compose.state('home');

compose.mount(function () {
    return (
        <compose.Column spacing={0}>
            <compose.Text key='page' padding={16} weight={1} text={'Selected: ' + tab.value} />
            <compose.NavigationBar key='bar'>
                <compose.NavigationBarItem key='home' icon={<compose.Icon name='Filled.Home' />} label='Home' selected={tab.value === 'home'} onClick={function () { tab.value = 'home'; }} />
                <compose.NavigationBarItem key='search' icon={<compose.Icon name='Filled.Search' />} label='Search' selected={tab.value === 'search'} onClick={function () { tab.value = 'search'; }} />
                <compose.NavigationBarItem key='profile' icon={<compose.Icon name='Filled.Person' />} label='Profile' selected={tab.value === 'profile'} onClick={function () { tab.value = 'profile'; }} />
            </compose.NavigationBar>
        </compose.Column>
    );
});
