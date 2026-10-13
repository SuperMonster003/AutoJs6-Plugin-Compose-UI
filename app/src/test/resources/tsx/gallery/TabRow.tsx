"ui";

let index = compose.state(0);

compose.mount(function () {
    return (
        <compose.Column>
            <compose.TabRow key='tabs' selectedIndex={index.value}>
                <compose.Tab key='overview' text='Overview' onClick={function () { index.value = 0; }} />
                <compose.Tab key='details' text='Details' onClick={function () { index.value = 1; }} />
                <compose.Tab key='history' text='History' onClick={function () { index.value = 2; }} />
            </compose.TabRow>
            <compose.Text key='page' padding={16} text={'Tab ' + (index.value + 1)} />
        </compose.Column>
    );
});
