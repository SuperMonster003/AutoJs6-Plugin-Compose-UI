"ui";

compose.mount(function () {
    return (
        <compose.NavigationRail key='rail'>
            <compose.NavigationRailItem key='one' icon={<compose.Icon name='Filled.Favorite' />} label='Favorites' selected={true} onClick={function () { toast('Favorites'); }} />
            <compose.NavigationRailItem key='two' icon={<compose.Icon name='Filled.Info' />} label='Info' selected={false} onClick={function () { toast('Info'); }} />
        </compose.NavigationRail>
    );
});
