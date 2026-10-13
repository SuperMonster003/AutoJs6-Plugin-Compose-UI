"ui";

compose.mount(function () {
    return (
        <compose.TabRow key='tabs' selectedIndex={0} scrollable={true}>
            <compose.Tab key='text' text='Text only' onClick={function () { toast('Text'); }} />
            <compose.Tab key='icon' text='With icon' icon={<compose.Icon name='Filled.Star' />} onClick={function () { toast('Icon'); }} />
            <compose.Tab key='disabled' text='Disabled' enabled={false} />
        </compose.TabRow>
    );
});
